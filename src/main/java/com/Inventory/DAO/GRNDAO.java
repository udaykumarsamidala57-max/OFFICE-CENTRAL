package com.Inventory.DAO;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.Inventory.Bean.GRNLine;
import com.Inventory.Bean.GoodsReceivedNote;

@Repository
public class GRNDAO {
    private final JdbcTemplate jdbc;

    public GRNDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public GoodsReceivedNote findPurchaseOrder(String poNumber) {
        List<GoodsReceivedNote> results = jdbc.query(
                "SELECT PO_id, po_number, po_date, vendor_name, vendor_gstin, vendor_address, Approval, po_status "
                        + "FROM po_master WHERE po_number=?",
                (rs, row) -> {
                    GoodsReceivedNote note = new GoodsReceivedNote();
                    note.setPoId(rs.getInt("PO_id"));
                    note.setPoNumber(rs.getString("po_number"));
                    note.setVendorName(rs.getString("vendor_name"));
                    note.setVendorGstin(rs.getString("vendor_gstin"));
                    note.setVendorAddress(rs.getString("vendor_address"));
                    note.setPoApproval(rs.getString("Approval"));
                    note.setPoStatus(rs.getString("po_status"));
                    return note;
                }, poNumber);
        if (results.isEmpty()) return null;
        GoodsReceivedNote note = results.get(0);
        note.setItems(findOutstandingItems(note.getPoId()));
        return note;
    }

    /** Serializes GRN submissions for this PO and returns its latest approval/status snapshot. */
    public GoodsReceivedNote lockPurchaseOrder(String poNumber) {
        List<GoodsReceivedNote> results = jdbc.query(
                "SELECT PO_id, po_number, vendor_name, vendor_gstin, vendor_address, Approval, po_status "
                        + "FROM po_master WHERE po_number=? FOR UPDATE",
                (rs, row) -> {
                    GoodsReceivedNote note = new GoodsReceivedNote();
                    note.setPoId(rs.getInt("PO_id"));
                    note.setPoNumber(rs.getString("po_number"));
                    note.setVendorName(rs.getString("vendor_name"));
                    note.setVendorGstin(rs.getString("vendor_gstin"));
                    note.setVendorAddress(rs.getString("vendor_address"));
                    note.setPoStatus(rs.getString("po_status"));
                    note.setPoApproval(rs.getString("Approval"));
                    return note;
                }, poNumber);
        return results.isEmpty() ? null : results.get(0);
    }

    public void lockPurchaseOrderItems(int poId) {
        jdbc.query("SELECT po_item_id FROM po_items WHERE PO_id=? ORDER BY sl_no FOR UPDATE",
                (rs, row) -> rs.getInt(1), poId);
    }

    public List<GRNLine> findOutstandingItems(int poId) {
        return jdbc.query("SELECT pi.po_item_id, pi.item_id, pi.description, im.UOM, pi.qty, "
                        + "COALESCE(SUM(gi.qty_accepted),0) AS accepted_qty "
                        + "FROM po_items pi LEFT JOIN item_master im ON im.Item_id=pi.item_id "
                        + "LEFT JOIN grn_items gi ON gi.po_item_id=pi.po_item_id "
                        + "WHERE pi.PO_id=? GROUP BY pi.po_item_id, pi.item_id, pi.description, im.UOM, pi.qty, pi.sl_no "
                        + "ORDER BY pi.sl_no",
                (rs, row) -> {
                    GRNLine item = new GRNLine();
                    item.setPoItemId(rs.getInt("po_item_id"));
                    item.setItemId(rs.getInt("item_id"));
                    item.setDescription(rs.getString("description"));
                    item.setUom(rs.getString("UOM"));
                    item.setOrderedQty(nonNull(rs.getBigDecimal("qty")));
                    item.setAlreadyAccepted(nonNull(rs.getBigDecimal("accepted_qty")));
                    return item;
                }, poId);
    }

    public int insertMaster(GoodsReceivedNote note) {
        String sql = "INSERT INTO grn_master(grn_no,grn_date,po_id,po_number,vendor_name,vendor_gstin,vendor_address,"
                + "invoice_no,invoice_date,received_by,remarks,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,NOW())";
        KeyHolder keys = new GeneratedKeyHolder();
        PreparedStatementCreator creator = connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, note.getGrnNo());
            ps.setObject(2, note.getGrnDate());
            ps.setInt(3, note.getPoId());
            ps.setString(4, note.getPoNumber());
            ps.setString(5, note.getVendorName());
            ps.setString(6, note.getVendorGstin());
            ps.setString(7, note.getVendorAddress());
            ps.setString(8, note.getInvoiceNo());
            ps.setObject(9, note.getInvoiceDate());
            ps.setString(10, note.getReceivedBy());
            ps.setString(11, note.getRemarks());
            return ps;
        };
        jdbc.update(creator, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("The GRN was saved without a generated ID.");
        return key.intValue();
    }

    public void insertItems(int grnId, List<GRNLine> items) {
        jdbc.batchUpdate("INSERT INTO grn_items(grn_id,po_item_id,item_id,item_description,qty_received,qty_accepted,"
                        + "qty_rejected,remarks) VALUES(?,?,?,?,?,?,?,?)", items, 50, (ps, item) -> {
                    ps.setInt(1, grnId);
                    ps.setInt(2, item.getPoItemId());
                    ps.setInt(3, item.getItemId());
                    ps.setString(4, item.getDescription());
                    ps.setBigDecimal(5, item.getQtyReceived());
                    ps.setBigDecimal(6, item.getQtyAccepted());
                    ps.setBigDecimal(7, item.getQtyRejected());
                    ps.setString(8, item.getRemarks());
                });
    }

    /** Updates inventory only for accepted stock and writes a matching receipt ledger row. */
    public void receiveAcceptedStock(int grnId, String grnNo, LocalDate grnDate, GRNLine item) {
        BigDecimal accepted = item.getQtyAccepted();
        if (accepted.signum() <= 0) return;

        List<Map<String, Object>> stockRows = jdbc.query(
                "SELECT stock_id,total_received,balance_qty FROM stock WHERE item_id=? ORDER BY stock_id LIMIT 1 FOR UPDATE",
                (rs, row) -> {
                    Map<String, Object> stock = new HashMap<>();
                    stock.put("stock_id", rs.getInt("stock_id"));
                    stock.put("total_received", nonNull(rs.getBigDecimal("total_received")));
                    stock.put("balance_qty", nonNull(rs.getBigDecimal("balance_qty")));
                    return stock;
                }, item.getItemId());

        BigDecimal totalReceived;
        BigDecimal balance;
        if (stockRows.isEmpty()) {
            totalReceived = accepted;
            balance = accepted;
            jdbc.update("INSERT INTO stock(item_id,po_item_id,total_received,total_issued,balance_qty,last_updated) "
                            + "VALUES(?,?,?,0,?,NOW())",
                    item.getItemId(), item.getPoItemId(), totalReceived, balance);
        } else {
            Map<String, Object> stock = stockRows.get(0);
            totalReceived = ((BigDecimal) stock.get("total_received")).add(accepted);
            balance = ((BigDecimal) stock.get("balance_qty")).add(accepted);
            jdbc.update("UPDATE stock SET total_received=?,balance_qty=?,last_updated=NOW() WHERE stock_id=?",
                    totalReceived, balance, stock.get("stock_id"));
        }

        jdbc.update("INSERT INTO stock_ledger(item_id,po_item_id,trans_type,trans_id,trans_date,qty,running_balance,remarks) "
                        + "VALUES(?,?,'RECEIPT',?,?,?, ?,?)",
                item.getItemId(), item.getPoItemId(), grnId, grnDate, accepted, balance, "GRN " + grnNo);
    }

    public void updatePurchaseOrderReceiptStatus(int poId) {
        List<Map<String, Object>> balances = jdbc.query(
                "SELECT pi.qty AS ordered_qty,COALESCE(SUM(gi.qty_accepted),0) AS accepted_qty "
                        + "FROM po_items pi LEFT JOIN grn_items gi ON gi.po_item_id=pi.po_item_id "
                        + "WHERE pi.PO_id=? GROUP BY pi.po_item_id,pi.qty",
                (rs, row) -> {
                    Map<String, Object> balance = new HashMap<>();
                    balance.put("ordered", nonNull(rs.getBigDecimal("ordered_qty")));
                    balance.put("accepted", nonNull(rs.getBigDecimal("accepted_qty")));
                    return balance;
                }, poId);
        if (balances.isEmpty()) return;

        boolean anyAccepted = false;
        boolean allAccepted = true;
        for (Map<String, Object> row : balances) {
            BigDecimal ordered = (BigDecimal) row.get("ordered");
            BigDecimal accepted = (BigDecimal) row.get("accepted");
            anyAccepted |= accepted.signum() > 0;
            allAccepted &= accepted.compareTo(ordered) >= 0;
        }
        if (allAccepted) {
            jdbc.update("UPDATE po_master SET po_status='Closed' WHERE PO_id=?", poId);
        } else if (anyAccepted) {
            jdbc.update("UPDATE po_master SET po_status='Partially Received' WHERE PO_id=?", poId);
        }
    }

    public List<GoodsReceivedNote> findReport(String search, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sql = new StringBuilder("SELECT grn_id,grn_no,grn_date,po_id,po_number,vendor_name,vendor_gstin,"
                + "vendor_address,invoice_no,invoice_date,received_by,remarks FROM grn_master WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append("AND (grn_no LIKE ? OR po_number LIKE ? OR vendor_name LIKE ? OR invoice_no LIKE ?) ");
            String term = "%" + search.trim() + "%";
            args.add(term); args.add(term); args.add(term); args.add(term);
        }
        if (fromDate != null) { sql.append("AND grn_date>=? "); args.add(fromDate); }
        if (toDate != null) { sql.append("AND grn_date<=? "); args.add(toDate); }
        sql.append("ORDER BY grn_id DESC");
        List<GoodsReceivedNote> notes = jdbc.query(sql.toString(), (rs, row) -> mapNote(rs), args.toArray());
        attachItems(notes);
        return notes;
    }

    public GoodsReceivedNote findByNumber(String number) {
        List<GoodsReceivedNote> notes = jdbc.query("SELECT grn_id,grn_no,grn_date,po_id,po_number,vendor_name,vendor_gstin,"
                        + "vendor_address,invoice_no,invoice_date,received_by,remarks FROM grn_master WHERE grn_no=?",
                (rs, row) -> mapNote(rs), number);
        if (notes.isEmpty()) return null;
        attachItems(notes);
        return notes.get(0);
    }

    private void attachItems(List<GoodsReceivedNote> notes) {
        if (notes.isEmpty()) return;
        List<Integer> ids = notes.stream().map(GoodsReceivedNote::getId).toList();
        Map<Integer, List<GRNLine>> byGrn = new HashMap<>();
        for (int start = 0; start < ids.size(); start += 500) {
            List<Integer> batch = ids.subList(start, Math.min(start + 500, ids.size()));
            String marks = String.join(",", java.util.Collections.nCopies(batch.size(), "?"));
            jdbc.query("SELECT gi.grn_id,gi.po_item_id,gi.item_id,gi.item_description,gi.qty_received,"
                            + "gi.qty_accepted,gi.qty_rejected,gi.remarks,pi.qty AS ordered_qty "
                            + "FROM grn_items gi LEFT JOIN po_items pi ON pi.po_item_id=gi.po_item_id "
                            + "WHERE gi.grn_id IN (" + marks + ") ORDER BY gi.grn_id,gi.po_item_id",
                    rs -> {
                        GRNLine item = new GRNLine();
                        item.setPoItemId(rs.getInt("po_item_id"));
                        item.setItemId(rs.getInt("item_id"));
                        item.setDescription(rs.getString("item_description"));
                        item.setOrderedQty(nonNull(rs.getBigDecimal("ordered_qty")));
                        item.setQtyReceived(nonNull(rs.getBigDecimal("qty_received")));
                        item.setQtyAccepted(nonNull(rs.getBigDecimal("qty_accepted")));
                        item.setQtyRejected(nonNull(rs.getBigDecimal("qty_rejected")));
                        item.setRemarks(rs.getString("remarks"));
                        byGrn.computeIfAbsent(rs.getInt("grn_id"), ignored -> new ArrayList<>()).add(item);
                    }, batch.toArray());
        }
        notes.forEach(note -> note.setItems(byGrn.getOrDefault(note.getId(), List.of())));
    }

    private GoodsReceivedNote mapNote(java.sql.ResultSet rs) throws java.sql.SQLException {
        GoodsReceivedNote note = new GoodsReceivedNote();
        note.setId(rs.getInt("grn_id"));
        note.setGrnNo(rs.getString("grn_no"));
        java.sql.Date grnDate = rs.getDate("grn_date");
        note.setGrnDate(grnDate == null ? null : grnDate.toLocalDate());
        note.setPoId(rs.getInt("po_id"));
        note.setPoNumber(rs.getString("po_number"));
        note.setVendorName(rs.getString("vendor_name"));
        note.setVendorGstin(rs.getString("vendor_gstin"));
        note.setVendorAddress(rs.getString("vendor_address"));
        note.setInvoiceNo(rs.getString("invoice_no"));
        java.sql.Date invoiceDate = rs.getDate("invoice_date");
        note.setInvoiceDate(invoiceDate == null ? null : invoiceDate.toLocalDate());
        note.setReceivedBy(rs.getString("received_by"));
        note.setRemarks(rs.getString("remarks"));
        return note;
    }

    private BigDecimal nonNull(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}

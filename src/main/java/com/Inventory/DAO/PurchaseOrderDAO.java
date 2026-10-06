package com.Inventory.DAO;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.Inventory.Bean.PurchaseOrder;
import com.Inventory.Bean.PurchaseOrderLine;
import com.Inventory.Bean.VendorOption;

@Repository
public class PurchaseOrderDAO {
    private final JdbcTemplate jdbc;

    public PurchaseOrderDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<PurchaseOrderLine> findEligibleIndents() {
        return jdbc.query("SELECT indent_id, indent_no, item_id, item_name, UOM, qty, indent_date, department, requested_by "
                        + "FROM indent WHERE POStatus IS NULL AND Indentnext='PO' ORDER BY indent_id DESC",
                (rs, row) -> {
                    PurchaseOrderLine line = new PurchaseOrderLine();
                    line.setIndentId(rs.getInt("indent_id"));
                    line.setIndentNo(rs.getString("indent_no"));
                    line.setItemId(rs.getInt("item_id"));
                    line.setDescription(rs.getString("item_name"));
                    line.setUom(rs.getString("UOM"));
                    line.setQuantity(rs.getBigDecimal("qty"));
                    return line;
                });
    }

    public List<PurchaseOrderLine> findEligibleIndents(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        return jdbc.query("SELECT indent_id, indent_no, item_id, item_name, UOM, qty FROM indent "
                        + "WHERE POStatus IS NULL AND Indentnext='PO' AND indent_id IN (" + placeholders + ") "
                        + "ORDER BY indent_id", (rs, row) -> {
                    PurchaseOrderLine line = new PurchaseOrderLine();
                    line.setIndentId(rs.getInt("indent_id"));
                    line.setIndentNo(rs.getString("indent_no"));
                    line.setItemId(rs.getInt("item_id"));
                    line.setDescription(rs.getString("item_name"));
                    line.setUom(rs.getString("UOM"));
                    line.setQuantity(rs.getBigDecimal("qty"));
                    return line;
                }, ids.toArray());
    }

    public List<VendorOption> findVendors() {
        return jdbc.query("SELECT name, GSTIN, address FROM vendors ORDER BY name", (rs, row) -> {
            VendorOption vendor = new VendorOption();
            vendor.setName(rs.getString("name"));
            vendor.setGstin(rs.getString("GSTIN"));
            vendor.setAddress(rs.getString("address"));
            return vendor;
        });
    }

    public String nextNumber() {
        List<String> latest = jdbc.query("SELECT po_number FROM po_master ORDER BY po_id DESC LIMIT 1",
                (rs, row) -> rs.getString(1));
        if (latest.isEmpty() || latest.get(0) == null) return "PO0001";
        String digits = latest.get(0).replaceAll("\\D+", "");
        long next = digits.isEmpty() ? 1 : Long.parseLong(digits) + 1;
        return String.format("PO%04d", next);
    }

    public int insertMaster(PurchaseOrder po) {
        String sql = "INSERT INTO po_master(vendor_name,vendor_gstin,vendor_address,po_number,quotation_number,po_date,"
                + "billing_address,total_gst,total_dis,total_amount,amount_in_words,terms_conditions,general_conditions,"
                + "po_status,Approval,Servicecharge) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,'Open','Pending',?)";
        KeyHolder keys = new GeneratedKeyHolder();
        PreparedStatementCreator creator = connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, po.getVendorName());
            ps.setString(2, po.getVendorGstin());
            ps.setString(3, po.getVendorAddress());
            ps.setString(4, po.getNumber());
            ps.setString(5, po.getQuotationNumber());
            ps.setString(6, po.getDate());
            ps.setString(7, po.getBillingAddress());
            ps.setBigDecimal(8, po.getTotalGst());
            ps.setBigDecimal(9, po.getTotalDiscount());
            ps.setBigDecimal(10, po.getTotalAmount());
            ps.setString(11, "");
            ps.setString(12, po.getTermsConditions());
            ps.setString(13, po.getGeneralConditions());
            ps.setBigDecimal(14, po.getServiceCharge());
            return ps;
        };
        jdbc.update(creator, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("The purchase order was saved without a generated ID.");
        return key.intValue();
    }

    public void insertItems(int poId, String poNumber, List<PurchaseOrderLine> items) {
        java.util.concurrent.atomic.AtomicInteger serial = new java.util.concurrent.atomic.AtomicInteger();
        jdbc.batchUpdate("INSERT INTO po_items(po_id,po_no,sl_no,item_id,description,qty,rate,amount,discount_percent,"
                        + "discount_value,gst_percent,gst_value,net_amount) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                items, 50, (ps, line) -> {
                    ps.setInt(1, poId);
                    ps.setString(2, poNumber);
                    ps.setInt(3, serial.incrementAndGet());
                    ps.setInt(4, line.getItemId());
                    ps.setString(5, line.getDescription());
                    ps.setBigDecimal(6, line.getQuantity());
                    ps.setBigDecimal(7, line.getRate());
                    ps.setBigDecimal(8, line.getAmount());
                    ps.setBigDecimal(9, line.getDiscountPercent());
                    ps.setBigDecimal(10, line.getDiscountValue());
                    ps.setBigDecimal(11, line.getGstPercent());
                    ps.setBigDecimal(12, line.getGstValue());
                    ps.setBigDecimal(13, line.getNetAmount());
                });
    }

    public int markIndentsRaised(List<Integer> ids) {
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        List<Object> args = new ArrayList<>(ids);
        return jdbc.update("UPDATE indent SET POStatus='Raised' WHERE POStatus IS NULL AND Indentnext='PO' "
                + "AND indent_id IN (" + placeholders + ")", args.toArray());
    }

    public List<PurchaseOrder> findOrders(boolean approvalsOnly, String search, String fromDate, String toDate,
                                          String approval) {
        StringBuilder sql = new StringBuilder("SELECT * FROM po_master WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (approvalsOnly) sql.append("AND po_status IN ('Open','Partially Received') ");
        if (search != null && !search.isBlank()) {
            sql.append("AND (po_number LIKE ? OR vendor_name LIKE ?) ");
            String like = "%" + search.trim() + "%";
            args.add(like); args.add(like);
        }
        if (fromDate != null && !fromDate.isBlank()) { sql.append("AND po_date >= ? "); args.add(fromDate); }
        if (toDate != null && !toDate.isBlank()) { sql.append("AND po_date <= ? "); args.add(toDate); }
        if (approval != null && !approval.isBlank()) { sql.append("AND Approval = ? "); args.add(approval); }
        // Do not cap the report at an arbitrary number of orders. Older POs must
        // remain searchable/visible as the database grows.
        sql.append("ORDER BY PO_id DESC");
        List<PurchaseOrder> orders = jdbc.query(sql.toString(), (rs, row) -> mapOrder(rs), args.toArray());
        if (!orders.isEmpty()) {
            List<Integer> ids = orders.stream().map(PurchaseOrder::getId).toList();
            java.util.Map<Integer, List<PurchaseOrderLine>> itemsByOrder = new java.util.HashMap<>();
            // Keep the IN clause small so loading a large history does not exceed
            // database/driver parameter limits.
            final int batchSize = 500;
            for (int start = 0; start < ids.size(); start += batchSize) {
                List<Integer> batch = ids.subList(start, Math.min(start + batchSize, ids.size()));
                String itemPlaceholders = String.join(",", java.util.Collections.nCopies(batch.size(), "?"));
                jdbc.query("SELECT i.PO_id, i.po_no, i.item_id, i.description, i.qty, i.rate, i.amount, i.discount_percent, "
                                + "i.discount_value, i.gst_percent, i.gst_value, i.net_amount, m.UOM, "
                                + "COALESCE(s.balance_qty,0) AS balance_qty, COALESCE((SELECT SUM(g.qty_accepted) FROM grn_items g "
                                + "WHERE g.po_item_id=i.po_item_id),0) AS received_qty FROM po_items i "
                                + "LEFT JOIN item_master m ON i.item_id=m.Item_id LEFT JOIN stock s ON i.item_id=s.item_id "
                                + "WHERE i.PO_id IN (" + itemPlaceholders + ") ORDER BY i.PO_id, i.sl_no", rs -> {
                            PurchaseOrderLine line = mapItem(rs);
                            itemsByOrder.computeIfAbsent(rs.getInt("PO_id"), key -> new ArrayList<>()).add(line);
                        }, batch.toArray());
            }
            orders.forEach(order -> order.setItems(itemsByOrder.getOrDefault(order.getId(), List.of())));
        }
        if (!approvalsOnly) java.util.Collections.reverse(orders);
        return orders;
    }

    public PurchaseOrder findByNumber(String number) {
        List<PurchaseOrder> orders = jdbc.query("SELECT * FROM po_master WHERE po_number=?", (rs, row) -> mapOrder(rs), number);
        if (orders.isEmpty()) return null;
        PurchaseOrder order = orders.get(0);
        order.setItems(findItems(order.getId()));
        return order;
    }

    public int approve(String number) {
        return jdbc.update("UPDATE po_master SET Approval='Approved' WHERE po_number=? AND COALESCE(Approval,'Pending') <> 'Approved'", number);
    }

    public int deletePending(String number) {
        PurchaseOrder order = findByNumber(number);
        if (order == null) return 0;
        if ("Approved".equalsIgnoreCase(order.getApproval())) throw new IllegalArgumentException("Approved purchase orders cannot be deleted.");
        jdbc.update("DELETE FROM po_items WHERE po_no=?", number);
        return jdbc.update("DELETE FROM po_master WHERE po_number=? AND COALESCE(Approval,'Pending') <> 'Approved'", number);
    }

    private List<PurchaseOrderLine> findItems(int poId) {
        return jdbc.query("SELECT i.po_item_id, i.po_no, i.item_id, i.description, i.qty, i.rate, i.amount, i.discount_percent, "
                        + "i.discount_value, i.gst_percent, i.gst_value, i.net_amount, m.UOM, COALESCE(s.balance_qty,0) AS balance_qty, "
                        + "COALESCE((SELECT SUM(g.qty_accepted) FROM grn_items g WHERE g.po_item_id=i.po_item_id),0) AS received_qty "
                        + "FROM po_items i LEFT JOIN item_master m ON i.item_id=m.Item_id LEFT JOIN stock s ON i.item_id=s.item_id "
                        + "WHERE i.PO_id=? ORDER BY i.sl_no",
                (rs, row) -> mapItem(rs), poId);
    }

    private PurchaseOrderLine mapItem(java.sql.ResultSet rs) throws java.sql.SQLException {
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setIndentNo(rs.getString("po_no"));
        line.setItemId(rs.getInt("item_id"));
        line.setDescription(rs.getString("description"));
        line.setQuantity(nonNull(rs.getBigDecimal("qty")));
        line.setRate(nonNull(rs.getBigDecimal("rate")));
        line.setAmount(nonNull(rs.getBigDecimal("amount")));
        line.setDiscountPercent(nonNull(rs.getBigDecimal("discount_percent")));
        line.setDiscountValue(nonNull(rs.getBigDecimal("discount_value")));
        line.setGstPercent(nonNull(rs.getBigDecimal("gst_percent")));
        line.setGstValue(nonNull(rs.getBigDecimal("gst_value")));
        line.setNetAmount(nonNull(rs.getBigDecimal("net_amount")));
        line.setUom(rs.getString("UOM"));
        line.setReceivedQty(nonNull(rs.getBigDecimal("received_qty")));
        line.setBalanceQty(nonNull(rs.getBigDecimal("balance_qty")));
        return line;
    }

    private PurchaseOrder mapOrder(java.sql.ResultSet rs) throws java.sql.SQLException {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(rs.getInt("PO_id"));
        po.setNumber(rs.getString("po_number"));
        po.setDate(rs.getString("po_date"));
        po.setVendorName(rs.getString("vendor_name"));
        po.setVendorGstin(rs.getString("vendor_gstin"));
        po.setVendorAddress(rs.getString("vendor_address"));
        po.setQuotationNumber(rs.getString("quotation_number"));
        po.setBillingAddress(rs.getString("billing_address"));
        po.setTotalGst(nonNull(rs.getBigDecimal("total_gst")));
        po.setTotalDiscount(nonNull(rs.getBigDecimal("total_dis")));
        po.setTotalAmount(nonNull(rs.getBigDecimal("total_amount")));
        po.setTermsConditions(rs.getString("terms_conditions"));
        po.setGeneralConditions(rs.getString("general_conditions"));
        po.setStatus(rs.getString("po_status"));
        po.setApproval(rs.getString("Approval"));
        po.setServiceCharge(nonNull(rs.getBigDecimal("Servicecharge")));
        return po;
    }

    private java.math.BigDecimal nonNull(java.math.BigDecimal value) {
        return value == null ? java.math.BigDecimal.ZERO : value;
    }
}

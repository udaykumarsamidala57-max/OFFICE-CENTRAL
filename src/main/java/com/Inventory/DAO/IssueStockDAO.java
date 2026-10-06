package com.Inventory.DAO;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IssueStockDAO {
    private final JdbcTemplate jdbc;

    public IssueStockDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> findPendingIssues() {
        String sql = "SELECT i.indent_id,i.indent_no,i.requested_by,i.department,i.item_id,i.item_name,i.qty,i.UOM,"
                + "i.purpose,i.remarks,COALESCE(s.balance_qty,0) AS available_stock,"
                + "COALESCE((SELECT pi.net_amount/NULLIF(pi.qty,0) FROM po_items pi WHERE pi.item_id=i.item_id "
                + "AND pi.qty>0 ORDER BY pi.PO_id DESC,pi.sl_no DESC LIMIT 1),s.last_price,0) AS unit_price "
                + "FROM indent i LEFT JOIN stock s ON s.item_id=i.item_id "
                + "WHERE i.status='Approved' AND i.Indentnext='Issue' "
                + "AND (i.Issued_status IS NULL OR i.Issued_status='Pending') ORDER BY i.indent_id DESC";
        return jdbc.query(sql, (rs, row) -> mapPendingIssue(rs));
    }

    public List<Map<String, Object>> findIssueReport(LocalDate from, LocalDate to, String department, String search) {
        StringBuilder sql = new StringBuilder("SELECT si.issueno,si.indent_no,si.item_id,im.Item_name,si.issued_to,"
                + "si.department,si.qty_issued,si.unit_price,si.total_value,si.issue_date,si.remarks "
                + "FROM stock_issues si JOIN item_master im ON im.Item_id=si.item_id WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (from != null && to != null) { sql.append("AND si.issue_date>=? AND si.issue_date<? "); args.add(from); args.add(to.plusDays(1)); }
        else if (from != null) { sql.append("AND si.issue_date>=? "); args.add(from); }
        else if (to != null) { sql.append("AND si.issue_date<? "); args.add(to.plusDays(1)); }
        if (department != null && !department.isBlank()) { sql.append("AND si.department=? "); args.add(department.trim()); }
        if (search != null && !search.isBlank()) {
            sql.append("AND (si.indent_no LIKE ? OR si.issueno LIKE ? OR im.Item_name LIKE ? OR si.issued_to LIKE ?) ");
            String pattern = "%" + search.trim() + "%";
            args.add(pattern); args.add(pattern); args.add(pattern); args.add(pattern);
        }
        sql.append("ORDER BY si.issue_date DESC,si.issueno DESC");
        return jdbc.query(sql.toString(), (rs, row) -> mapIssue(rs), args.toArray());
    }

    public List<String> findDepartments() {
        return jdbc.query("SELECT DISTINCT department FROM stock_issues WHERE department IS NOT NULL AND department<>'' ORDER BY department",
                (rs, row) -> rs.getString(1));
    }

    public List<Map<String, Object>> findIssueVoucher(String indentNo) {
        return jdbc.query("SELECT si.issueno,si.indent_no,si.item_id,im.Item_name,si.issued_to,si.department,"
                        + "si.qty_issued,si.unit_price,si.total_value,si.issue_date,si.remarks FROM stock_issues si "
                        + "JOIN item_master im ON im.Item_id=si.item_id WHERE si.indent_no=? ORDER BY si.issue_date,si.issueno,si.item_id",
                (rs, row) -> mapIssue(rs), indentNo);
    }

    public List<String> findCategories() {
        return jdbc.query("SELECT DISTINCT Category FROM item_master WHERE Category IS NOT NULL AND Category<>'' ORDER BY Category",
                (rs, row) -> rs.getString(1));
    }

    public List<Map<String, Object>> findCurrentStock(String category, String search) {
        StringBuilder sql = new StringBuilder("SELECT s.item_id,im.Item_name,im.Category,im.Sub_Category,im.UOM,"
                + "COALESCE(s.total_received,0) AS total_received,COALESCE(s.total_issued,0) AS total_issued,"
                + "COALESCE(s.balance_qty,0) AS balance_qty,COALESCE(s.last_price,0) AS last_price,s.last_updated "
                + "FROM stock s JOIN item_master im ON im.Item_id=s.item_id WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (category != null && !category.isBlank()) { sql.append("AND im.Category=? "); args.add(category.trim()); }
        if (search != null && !search.isBlank()) {
            sql.append("AND (im.Item_name LIKE ? OR CAST(s.item_id AS CHAR) LIKE ? OR im.Sub_Category LIKE ?) ");
            String pattern = "%" + search.trim() + "%";
            args.add(pattern); args.add(pattern); args.add(pattern);
        }
        sql.append("ORDER BY im.Item_name");
        return jdbc.query(sql.toString(), (rs, row) -> mapStock(rs), args.toArray());
    }

    public List<Map<String, Object>> findStockSummary(LocalDate from, LocalDate to, String category, String subCategory) {
        StringBuilder sql = new StringBuilder("SELECT im.Item_id,im.Item_name,im.Category,im.Sub_Category,im.UOM,"
                + "COALESCE(s.last_price,0) AS last_price,"
                + "COALESCE((SELECT SUM(CASE WHEN sl.trans_type='RECEIPT' THEN sl.qty WHEN sl.trans_type='ISSUE' THEN -sl.qty ELSE 0 END) "
                + "FROM stock_ledger sl WHERE sl.item_id=im.Item_id AND sl.trans_date<?),0) AS opening_balance,"
                + "COALESCE((SELECT SUM(sl.qty) FROM stock_ledger sl WHERE sl.item_id=im.Item_id AND sl.trans_type='RECEIPT' "
                + "AND sl.trans_date>=? AND sl.trans_date<?),0) AS receipts,"
                + "COALESCE((SELECT SUM(sl.qty) FROM stock_ledger sl WHERE sl.item_id=im.Item_id AND sl.trans_type='ISSUE' "
                + "AND sl.trans_date>=? AND sl.trans_date<?),0) AS issues "
                + "FROM item_master im LEFT JOIN stock s ON s.item_id=im.Item_id WHERE 1=1 ");
        List<Object> args = new ArrayList<>(List.of(from, from, to.plusDays(1), from, to.plusDays(1)));
        if (category != null && !category.isBlank()) { sql.append("AND im.Category=? "); args.add(category.trim()); }
        if (subCategory != null && !subCategory.isBlank()) { sql.append("AND im.Sub_Category=? "); args.add(subCategory.trim()); }
        sql.append("ORDER BY im.Category,im.Sub_Category,im.Item_name");
        return jdbc.query(sql.toString(), (rs, row) -> mapSummary(rs), args.toArray());
    }

    public Map<String, Object> lockIssueIndent(int indentId) {
        List<Map<String, Object>> rows = jdbc.query("SELECT indent_id,indent_no,requested_by,department,item_id,item_name,qty,status,Indentnext,Issued_status "
                        + "FROM indent WHERE indent_id=? FOR UPDATE", (rs, row) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("indent_id")); m.put("indentNo", rs.getString("indent_no"));
                    m.put("requestedBy", rs.getString("requested_by")); m.put("department", rs.getString("department"));
                    m.put("itemId", rs.getInt("item_id")); m.put("itemName", rs.getString("item_name"));
                    m.put("qty", nz(rs.getBigDecimal("qty"))); m.put("status", rs.getString("status"));
                    m.put("indentNext", rs.getString("Indentnext")); m.put("issuedStatus", rs.getString("Issued_status"));
                    return m;
                }, indentId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public BigDecimal lockStockBalance(int itemId) {
        List<BigDecimal> balances = jdbc.query("SELECT COALESCE(balance_qty,0) FROM stock WHERE item_id=? ORDER BY stock_id LIMIT 1 FOR UPDATE",
                (rs, row) -> nz(rs.getBigDecimal(1)), itemId);
        return balances.isEmpty() ? BigDecimal.ZERO : balances.get(0);
    }

    public String nextIssueNumber() {
        jdbc.update("INSERT IGNORE INTO inventory_sequences(sequence_name,last_value) "
                + "SELECT 'stock_issue_no',COALESCE(MAX(CAST(issueno AS UNSIGNED)),0) FROM stock_issues");
        Integer current = jdbc.queryForObject("SELECT last_value FROM inventory_sequences WHERE sequence_name='stock_issue_no' FOR UPDATE", Integer.class);
        int next = (current == null ? 0 : current) + 1;
        jdbc.update("UPDATE inventory_sequences SET last_value=? WHERE sequence_name='stock_issue_no'", next);
        return String.valueOf(next);
    }

    public void insertIssue(String issueNo, Map<String, Object> indent, BigDecimal qty, BigDecimal price, LocalDate date) {
        BigDecimal total = qty.multiply(price);
        jdbc.update("INSERT INTO stock_issues(issueno,item_id,issued_to,department,qty_issued,remarks,indent_no,unit_price,total_value,issue_date) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?)", issueNo, indent.get("itemId"), indent.get("requestedBy"), indent.get("department"),
                qty, "Issued against indent " + indent.get("indentNo"), indent.get("indentNo"), price, total, date);
        int updated = jdbc.update("UPDATE stock SET total_issued=total_issued+?,balance_qty=balance_qty-?,last_price=?,last_updated=NOW() "
                        + "WHERE item_id=? AND balance_qty>=?", qty, qty, price, indent.get("itemId"), qty);
        if (updated != 1) throw new IllegalArgumentException("Stock changed while the issue was being saved. Reload and try again.");
        jdbc.update("UPDATE indent SET Issued_status='Issued',Issued_qty=?,POStatus='Completed',Indentnext='Issued' WHERE indent_id=?",
                qty, indent.get("id"));
        BigDecimal balance = jdbc.queryForObject("SELECT balance_qty FROM stock WHERE item_id=? ORDER BY stock_id LIMIT 1",
                BigDecimal.class, indent.get("itemId"));
        jdbc.update("INSERT INTO stock_ledger(item_id,trans_type,trans_id,qty,running_balance,remarks,trans_date) VALUES(?,'ISSUE',?,?,?,?,?)",
                indent.get("itemId"), issueNo, qty, nz(balance), "Issue for indent " + indent.get("id"), date);
    }

    private Map<String, Object> mapPendingIssue(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("indentId", rs.getInt("indent_id")); m.put("indentNo", rs.getString("indent_no"));
        m.put("requestedBy", rs.getString("requested_by")); m.put("department", rs.getString("department"));
        m.put("itemId", rs.getInt("item_id")); m.put("itemName", rs.getString("item_name"));
        m.put("qty", nz(rs.getBigDecimal("qty"))); m.put("uom", rs.getString("UOM"));
        m.put("purpose", rs.getString("purpose")); m.put("remarks", rs.getString("remarks"));
        m.put("availableStock", nz(rs.getBigDecimal("available_stock"))); m.put("unitPrice", nz(rs.getBigDecimal("unit_price")));
        return m;
    }

    private Map<String, Object> mapIssue(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issueNo", rs.getString("issueno")); m.put("indentNo", rs.getString("indent_no"));
        m.put("itemId", rs.getInt("item_id")); m.put("itemName", rs.getString("Item_name"));
        m.put("issuedTo", rs.getString("issued_to")); m.put("department", rs.getString("department"));
        m.put("qty", nz(rs.getBigDecimal("qty_issued"))); m.put("unitPrice", nz(rs.getBigDecimal("unit_price")));
        m.put("totalValue", nz(rs.getBigDecimal("total_value"))); m.put("issueDate", rs.getTimestamp("issue_date"));
        m.put("remarks", rs.getString("remarks")); return m;
    }

    private Map<String, Object> mapStock(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("itemId", rs.getInt("item_id")); m.put("itemName", rs.getString("Item_name"));
        m.put("category", rs.getString("Category")); m.put("subCategory", rs.getString("Sub_Category"));
        m.put("uom", rs.getString("UOM")); m.put("received", nz(rs.getBigDecimal("total_received")));
        m.put("issued", nz(rs.getBigDecimal("total_issued"))); m.put("balance", nz(rs.getBigDecimal("balance_qty")));
        m.put("unitPrice", nz(rs.getBigDecimal("last_price")));
        m.put("totalValue", nz(rs.getBigDecimal("balance_qty")).multiply(nz(rs.getBigDecimal("last_price"))));
        m.put("lastUpdated", rs.getTimestamp("last_updated")); return m;
    }

    private Map<String, Object> mapSummary(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        BigDecimal opening = nz(rs.getBigDecimal("opening_balance"));
        BigDecimal receipts = nz(rs.getBigDecimal("receipts"));
        BigDecimal issues = nz(rs.getBigDecimal("issues"));
        BigDecimal closing = opening.add(receipts).subtract(issues);
        BigDecimal price = nz(rs.getBigDecimal("last_price"));
        m.put("itemId", rs.getInt("Item_id")); m.put("itemName", rs.getString("Item_name"));
        m.put("category", rs.getString("Category")); m.put("subCategory", rs.getString("Sub_Category"));
        m.put("uom", rs.getString("UOM")); m.put("opening", opening); m.put("receipts", receipts);
        m.put("issues", issues); m.put("closing", closing); m.put("unitPrice", price); m.put("closingValue", closing.multiply(price));
        return m;
    }

    private static BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}

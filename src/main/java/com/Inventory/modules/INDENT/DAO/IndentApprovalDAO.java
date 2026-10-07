package com.Inventory.modules.INDENT.DAO;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import com.Inventory.Bean.IndentListItem;
import com.Inventory.SESSION.RoleAccess;

@Repository
public class IndentApprovalDAO {
    private static final String ROW_SELECT = "SELECT i.indent_id, i.indent_no, i.indent_date, "
            + "i.PurchaseorIssue, i.item_id, i.item_name, i.qty, i.UOM, i.department, "
            + "i.requested_by, i.purpose, i.Istatus, i.IstausApprove, i.Iapprovedate, "
            + "i.status, i.Fapprovedate, i.Fapprovedby, i.Indentnext, COALESCE(s.balance_qty,0) AS balance_qty "
            + "FROM indent i LEFT JOIN stock s ON i.item_id=s.item_id ";

    private final JdbcTemplate jdbc;

    public IndentApprovalDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<IndentListItem> findActive(String role, String department) {
        StringBuilder sql = new StringBuilder(ROW_SELECT)
                .append("WHERE (TRIM(i.Indentnext) NOT IN ('Issue','PO','Issued','Cancelled','PO Raised') OR i.Indentnext IS NULL) ")
                .append("AND (TRIM(i.status) NOT IN ('Cancelled') OR i.status IS NULL) ");
        List<Object> args = new ArrayList<>();
        if (RoleAccess.isSuperAdmin(role)) {
            // Super Admin can approve across every department.
        } else if ("Global".equalsIgnoreCase(role)) {
            sql.append("AND (i.department <> 'Dining Hall' OR i.department IS NULL) ");
        } else if ("Admin".equalsIgnoreCase(role)) {
            sql.append("AND i.department IN ('Electrical','Housekeeping','Plumbing','Dining Hall','RO Plant','Store','Dhobi') ");
        } else if (department != null && !department.isBlank()) {
            sql.append("AND i.department = ? ");
            args.add(department.trim());
        } else {
            return List.of();
        }
        sql.append("ORDER BY i.indent_id ASC");
        return jdbc.query(sql.toString(), IndentListDAO::mapRow, args.toArray());
    }

    public Map<Integer, Double> pendingByItem() {
        String sql = "SELECT item_id, COALESCE(SUM(qty),0) AS pending_sum FROM indent "
                + "WHERE Indentnext='Issue' AND status='Approved' GROUP BY item_id";
        Map<Integer, Double> pending = new HashMap<>();
        jdbc.query(sql, (RowCallbackHandler) rs -> pending.put(rs.getInt("item_id"), rs.getDouble("pending_sum")));
        return pending;
    }

    public IndentListItem findById(int id) {
        List<IndentListItem> rows = jdbc.query(ROW_SELECT + "WHERE i.indent_id=? LIMIT 1", IndentListDAO::mapRow, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public double sumApprovedIssueQty(int itemId, int excludeIndentId) {
        Double sum = jdbc.queryForObject("SELECT COALESCE(SUM(qty),0) FROM indent "
                + "WHERE item_id=? AND Indentnext='Issue' AND status='Approved' AND indent_id<>?",
                Double.class, itemId, excludeIndentId);
        return sum == null ? 0 : sum;
    }

    public int approveLevelOne(int id, String username, Date date) {
        return jdbc.update("UPDATE indent SET Istatus='Approved', IstausApprove=?, Iapprovedate=? "
                + "WHERE indent_id=? AND (Istatus IS NULL OR Istatus<>'Approved')", username, date, id);
    }

    public int setFinalDecision(int id, String nextStep, Date date, String username) {
        if ("Issue".equalsIgnoreCase(nextStep) || "PO".equalsIgnoreCase(nextStep)) {
            return jdbc.update("UPDATE indent SET status='Approved', Fapprovedate=?, Fapprovedby=?, Indentnext=? "
                    + "WHERE indent_id=?", date, username, nextStep, id);
        }
        return jdbc.update("UPDATE indent SET Indentnext=?, Fapprovedate=?, Fapprovedby=? WHERE indent_id=?",
                nextStep, date, username, id);
    }
}

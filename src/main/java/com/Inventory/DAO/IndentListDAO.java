package com.Inventory.DAO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.Inventory.Bean.IndentListItem;
import com.Inventory.SESSION.RoleAccess;

@Repository
public class IndentListDAO {
    private final JdbcTemplate jdbc;

    public IndentListDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<IndentListItem> findRecent(String role, String department) {
        StringBuilder sql = new StringBuilder(
                "SELECT i.indent_id, i.indent_no, i.indent_date, i.PurchaseorIssue, i.item_id, "
              + "i.item_name, i.qty, i.UOM, i.department, i.requested_by, i.purpose, i.Istatus, "
              + "i.IstausApprove, i.Iapprovedate, i.status, i.Fapprovedate, i.Fapprovedby, i.Indentnext, "
              + "i.stock AS balance_qty FROM indent i "
              + "WHERE (LOWER(i.purpose) <> 'adjustment' OR i.purpose IS NULL) ");
        List<Object> args = new ArrayList<>();

        boolean global = RoleAccess.isGlobal(role);
        boolean admin = "Admin".equalsIgnoreCase(role);
        boolean unrestrictedDepartment = "Finance".equalsIgnoreCase(department)
                || "Store".equalsIgnoreCase(department);

        if (!global && !unrestrictedDepartment) {
            if (admin) {
                sql.append("AND i.department IN ('Electrical','Housekeeping','Plumbing','Dininghall') ");
            } else if (department != null && !department.isBlank()) {
                sql.append("AND i.department = ? ");
                args.add(department.trim());
            } else {
                // No department means the user has no report scope.
                return List.of();
            }
        }

        sql.append("ORDER BY i.indent_id DESC LIMIT 1000");
        return jdbc.query(sql.toString(), IndentListDAO::mapRow, args.toArray());
    }

    public List<IndentListItem> findByIndentNo(String indentNo, String role, String department) {
        StringBuilder sql = new StringBuilder(
                "SELECT i.indent_id, i.indent_no, i.indent_date, i.PurchaseorIssue, i.item_id, "
              + "i.item_name, i.qty, i.UOM, i.department, i.requested_by, i.purpose, i.Istatus, "
              + "i.IstausApprove, i.Iapprovedate, i.status, i.Fapprovedate, i.Fapprovedby, i.Indentnext, "
              + "i.stock AS balance_qty FROM indent i WHERE i.indent_no = ? ");
        List<Object> args = new ArrayList<>();
        args.add(indentNo);

        boolean global = RoleAccess.isGlobal(role);
        boolean admin = "Admin".equalsIgnoreCase(role);
        boolean unrestrictedDepartment = "Finance".equalsIgnoreCase(department)
                || "Store".equalsIgnoreCase(department);
        if (!global && !unrestrictedDepartment) {
            if (admin) {
                sql.append("AND i.department IN ('Electrical','Housekeeping','Plumbing','Dininghall') ");
            } else if (department != null && !department.isBlank()) {
                sql.append("AND i.department = ? ");
                args.add(department.trim());
            } else {
                return List.of();
            }
        }
        sql.append("ORDER BY i.indent_id ASC");
        return jdbc.query(sql.toString(), IndentListDAO::mapRow, args.toArray());
    }

    static IndentListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        IndentListItem item = new IndentListItem();
        item.setId(rs.getInt("indent_id"));
        item.setIndentNo(rs.getString("indent_no"));
        item.setDate(rs.getDate("indent_date"));
        item.setType(rs.getString("PurchaseorIssue"));
        item.setItemId(rs.getInt("item_id"));
        item.setItemName(rs.getString("item_name"));
        item.setQty(rs.getDouble("qty"));
        item.setUom(rs.getString("UOM"));
        item.setDepartment(rs.getString("department"));
        item.setRequestedBy(rs.getString("requested_by"));
        item.setPurpose(rs.getString("purpose"));
        item.setLevelOneStatus(rs.getString("Istatus"));
        item.setLevelOneApprovedBy(rs.getString("IstausApprove"));
        item.setLevelOneApprovedDate(rs.getDate("Iapprovedate"));
        item.setLevelTwoStatus(rs.getString("status"));
        item.setLevelTwoApprovedDate(rs.getDate("Fapprovedate"));
        item.setLevelTwoApprovedBy(rs.getString("Fapprovedby"));
        item.setIndentStatus(rs.getString("Indentnext"));
        item.setBalanceQty(rs.getDouble("balance_qty"));
        return item;
    }
}

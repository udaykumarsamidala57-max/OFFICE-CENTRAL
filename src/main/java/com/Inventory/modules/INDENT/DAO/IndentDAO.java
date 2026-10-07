package com.Inventory.modules.INDENT.DAO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.Inventory.Bean.IndentBean;
import com.Inventory.SESSION.RoleAccess;

@Repository
public class IndentDAO {

    private final JdbcTemplate jdbc;

    public IndentDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int getNextIndentNumber() {
        try {
            Integer no = jdbc.queryForObject("SELECT next_val + 1 FROM id_sequences WHERE seq_name='indent_no'", Integer.class);
            if (no != null) return no;
        } catch (Exception ignored) {
            // Fall back to MAX(indent_no)
        }
        return jdbc.queryForObject("SELECT COALESCE(MAX(CAST(indent_no AS UNSIGNED)), 0) + 1 FROM indent", Integer.class);
    }

    public List<IndentBean> getDepartments(String role, String sessionDepartment) {
        if ("Admin".equalsIgnoreCase(role)) {
            return Arrays.stream(new String[]{"Housekeeping", "Plumbing", "Electrical"})
                    .map(dept -> { IndentBean b = new IndentBean(); b.setDepartmentName(dept); return b; })
                    .toList();
        }

        if (!RoleAccess.isGlobal(role) && sessionDepartment != null && !sessionDepartment.trim().isEmpty()) {
            IndentBean bean = new IndentBean();
            bean.setDepartmentName(sessionDepartment.trim());
            return List.of(bean);
        }

        String sql = "SELECT DISTINCT Department FROM dept_cate WHERE Department IS NOT NULL AND Department <> '' ORDER BY Department ASC";
        return jdbc.query(sql, (rs, rowNum) -> {
            IndentBean bean = new IndentBean();
            bean.setDepartmentName(rs.getString("Department"));
            return bean;
        });
    }

    public List<IndentBean> getCategoriesByDepartment(String department) {
        String sql = "SELECT DISTINCT Category FROM dept_cate WHERE Department = ? AND Category IS NOT NULL AND Category <> '' ORDER BY Category ASC";
        return jdbc.query(sql, (rs, rowNum) -> {
            IndentBean bean = new IndentBean();
            bean.setCategory(rs.getString("Category"));
            return bean;
        }, department);
    }

    public List<IndentBean> getSubCategoriesByDepartmentAndCategory(String department, String category) {
        String sql = "SELECT DISTINCT c.Sub_Category, c.Category FROM category c " +
                     "INNER JOIN dept_cate dc ON dc.Category = c.Category " +
                     "WHERE dc.Department = ? AND c.Category = ? AND c.Status = 'Active' " +
                     "AND c.Sub_Category IS NOT NULL AND c.Sub_Category <> '' ORDER BY c.Sub_Category ASC";
        return jdbc.query(sql, (rs, rowNum) -> {
            IndentBean bean = new IndentBean();
            bean.setCategory(rs.getString("Category"));
            bean.setSubCategory(rs.getString("Sub_Category"));
            return bean;
        }, department, category);
    }

    public List<IndentBean> getItemsByCategoryAndSubCategory(String category, String subCategory) {
        String sql = "SELECT im.Item_id, im.Item_name, im.UOM, im.Category, im.Sub_Category, COALESCE(s.balance_qty, 0) AS stock " +
                     "FROM item_master im LEFT JOIN stock s ON im.Item_id = s.item_id " +
                     "WHERE im.Category = ? AND im.Sub_Category = ? ORDER BY im.Item_name ASC";
        return jdbc.query(sql, this::mapItemRow, category, subCategory);
    }

    public IndentBean getMasterData(String role, String department) {
        IndentBean data = new IndentBean();
        data.setDepartments(getDepartments(role, department));
        return data;
    }

    public String saveIndent(String date, String department, String user, String indentType, List<IndentBean> items) {
        try {
            ensureIndentSequenceExists();

            Integer currentNo = jdbc.queryForObject("SELECT next_val FROM id_sequences WHERE seq_name='indent_no' FOR UPDATE", Integer.class);
            if (currentNo == null) throw new IllegalStateException("indent_no sequence was not found.");

            int nextNo = currentNo + 1;
            jdbc.update("UPDATE id_sequences SET next_val = ? WHERE seq_name='indent_no'", nextNo);

            String indentNo = String.valueOf(nextNo);
            insertIndentRows(date, department, user, indentType, items, indentNo);
            return indentNo;

        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException("No row found in id_sequences for seq_name='indent_no'.", e);
        }
    }

    /**
     * Initialize a missing sequence from existing indent numbers. INSERT IGNORE
     * makes simultaneous first saves safe when seq_name is a primary/unique key.
     */
    private void ensureIndentSequenceExists() {
        String sql = "INSERT IGNORE INTO id_sequences (seq_name, next_val) "
                + "SELECT 'indent_no', COALESCE(MAX(CAST(indent_no AS UNSIGNED)), 0) FROM indent";
        jdbc.update(sql);
    }

    private void insertIndentRows(String date, String department, String user, String indentType, List<IndentBean> items, String indentNo) {
        String sql = "INSERT INTO indent (indent_no, indent_date, item_id, item_name, qty, department, requested_by, purpose, remarks, uom, PurchaseorIssue, stock) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE((SELECT MAX(balance_qty) FROM stock WHERE stock.item_id = ?), 0))";

        jdbc.batchUpdate(sql, items, items.size(), (ps, item) -> {
            ps.setString(1, indentNo);
            ps.setString(2, date);
            ps.setInt(3, item.getItemId());
            ps.setString(4, item.getItemName());
            ps.setDouble(5, item.getQuantity());
            ps.setString(6, department);
            ps.setString(7, user);
            ps.setString(8, item.getPurpose());
            ps.setString(9, "");
            ps.setString(10, item.getUom());
            ps.setString(11, indentType);
            ps.setInt(12, item.getItemId());
        });
    }

    private IndentBean mapItemRow(ResultSet rs, int rowNum) throws SQLException {
        IndentBean bean = new IndentBean();
        bean.setItemId(rs.getInt("Item_id"));
        bean.setItemName(rs.getString("Item_name"));
        bean.setUom(rs.getString("UOM"));
        bean.setCategory(rs.getString("Category"));
        bean.setSubCategory(rs.getString("Sub_Category"));
        bean.setStock(rs.getDouble("stock"));
        return bean;
    }
}

package com.Inventory.DAO;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.Inventory.Bean.AccessUser;
import com.Inventory.DBUtil.DBUtil1;
import com.Inventory.Bean.PageCatalogEntry;

@Repository
public class PageAccessDAO {
    private final JdbcTemplate jdbc = new JdbcTemplate(DBUtil1.getDataSource("SRS"));

    public List<AccessUser> findUsers() {
        return jdbc.query("SELECT id, username, role FROM users ORDER BY username", (rs, row) -> {
            AccessUser user = new AccessUser();
            user.setId(rs.getInt("id")); user.setUsername(rs.getString("username")); user.setRole(rs.getString("role"));
            return user;
        });
    }

    public List<String> findRoles() {
        return jdbc.query("SELECT DISTINCT TRIM(role) FROM users WHERE role IS NOT NULL AND TRIM(role)<>'' ORDER BY TRIM(role)",
                (rs, row) -> rs.getString(1));
    }

    public boolean userHasPage(int userId, int pageId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM user_page_access WHERE user_id=? AND page_id=?", Integer.class, userId, pageId);
        return count != null && count > 0;
    }

    public boolean roleHasPage(String role, int pageId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM role_page_access WHERE role=? AND page_id=?", Integer.class, role, pageId);
        return count != null && count > 0;
    }

    public Integer findPageIdForPath(String path) {
        List<Integer> ids = jdbc.query("SELECT page_id FROM app_pages WHERE page_url=? "
                        + "OR ? LIKE CONCAT(page_url, '/%') ORDER BY LENGTH(page_url) DESC LIMIT 1",
                (rs, row) -> rs.getInt(1), path, path);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public boolean hasPageButton(int userId, String role, int pageId, String buttonCode) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM app_page_buttons b WHERE b.page_id=? "
                + "AND UPPER(b.button_code)=UPPER(?) AND ("
                + "EXISTS (SELECT 1 FROM user_page_button_access u WHERE u.user_id=? AND u.button_id=b.button_id) "
                + "OR EXISTS (SELECT 1 FROM role_page_button_access r WHERE r.role=? AND r.button_id=b.button_id))",
                Integer.class, pageId, buttonCode, userId, role == null ? "" : role.trim());
        return count != null && count > 0;
    }

    public Set<Integer> userButtons(int userId, int pageId) {
        return new HashSet<>(jdbc.query("SELECT a.button_id FROM user_page_button_access a "
                + "JOIN app_page_buttons b ON b.button_id=a.button_id WHERE a.user_id=? AND b.page_id=?",
                (rs, row) -> rs.getInt(1), userId, pageId));
    }

    public Set<Integer> roleButtons(String role, int pageId) {
        return new HashSet<>(jdbc.query("SELECT a.button_id FROM role_page_button_access a "
                + "JOIN app_page_buttons b ON b.button_id=a.button_id WHERE a.role=? AND b.page_id=?",
                (rs, row) -> rs.getInt(1), role, pageId));
    }

    public boolean userExists(int userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id=?", Integer.class, userId);
        return count != null && count > 0;
    }

    public String findUserRole(int userId) {
        return jdbc.query("SELECT role FROM users WHERE id=?", rs -> rs.next() ? rs.getString(1) : null, userId);
    }

    public boolean roleExists(String role) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE TRIM(role)=?", Integer.class, role);
        return count != null && count > 0;
    }

    public Set<Integer> buttonsForPage(int pageId) {
        return new HashSet<>(jdbc.query("SELECT button_id FROM app_page_buttons WHERE page_id=?", (rs, row) -> rs.getInt(1), pageId));
    }

    public void clearUserPageGrants(int userId, int pageId) {
        jdbc.update("DELETE FROM user_page_button_access WHERE user_id=? AND button_id IN "
                + "(SELECT button_id FROM app_page_buttons WHERE page_id=?)", userId, pageId);
        jdbc.update("DELETE FROM user_page_access WHERE user_id=? AND page_id=?", userId, pageId);
    }

    public void grantUserPage(int userId, int pageId) {
        jdbc.update("INSERT INTO user_page_access (user_id, page_id) VALUES (?, ?)", userId, pageId);
    }

    public void grantUserButton(int userId, int buttonId) {
        jdbc.update("INSERT INTO user_page_button_access (user_id, button_id) VALUES (?, ?)", userId, buttonId);
    }

    public void clearRolePageGrants(String role, int pageId) {
        jdbc.update("DELETE FROM role_page_button_access WHERE role=? AND button_id IN "
                + "(SELECT button_id FROM app_page_buttons WHERE page_id=?)", role, pageId);
        jdbc.update("DELETE FROM role_page_access WHERE role=? AND page_id=?", role, pageId);
    }

    public void grantRolePage(String role, int pageId) {
        jdbc.update("INSERT INTO role_page_access (role, page_id) VALUES (?, ?)", role, pageId);
    }

    public void grantRoleButton(String role, int buttonId) {
        jdbc.update("INSERT INTO role_page_button_access (role, button_id) VALUES (?, ?)", role, buttonId);
    }
}

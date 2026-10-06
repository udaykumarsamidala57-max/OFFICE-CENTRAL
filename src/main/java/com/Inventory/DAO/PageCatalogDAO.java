package com.Inventory.DAO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

import com.Inventory.Bean.PageButton;
import com.Inventory.Bean.PageCatalogEntry;
import com.Inventory.DBUtil.DBUtil1;

@Repository
public class PageCatalogDAO {
    private final JdbcTemplate jdbc = new JdbcTemplate(DBUtil1.getDataSource("SRS"));

    public PageCatalogDAO() { }

    public List<PageCatalogEntry> findAll() {
        Map<Integer, PageCatalogEntry> pages = new LinkedHashMap<>();
        jdbc.query("SELECT p.page_id, p.page_code, p.page_name, p.page_url, "
                        + "b.button_id, b.button_code, b.button_name "
                        + "FROM app_pages p LEFT JOIN app_page_buttons b ON b.page_id=p.page_id "
                        + "ORDER BY p.page_name, b.button_name",
                (RowCallbackHandler) rs -> {
                    int pageId = rs.getInt("page_id");
                    PageCatalogEntry page = pages.computeIfAbsent(pageId, key -> {
                        PageCatalogEntry entry = new PageCatalogEntry();
                        entry.setId(key);
                        try {
                            entry.setCode(rs.getString("page_code"));
                            entry.setName(rs.getString("page_name"));
                            entry.setUrl(rs.getString("page_url"));
                        } catch (java.sql.SQLException ex) {
                            throw new IllegalStateException(ex);
                        }
                        return entry;
                    });
                    int buttonId = rs.getInt("button_id");
                    if (!rs.wasNull()) {
                        PageButton button = new PageButton();
                        button.setId(buttonId);
                        button.setPageId(pageId);
                        button.setCode(rs.getString("button_code"));
                        button.setName(rs.getString("button_name"));
                        page.getButtons().add(button);
                    }
                });
        return new ArrayList<>(pages.values());
    }

    public PageCatalogEntry findPage(int id) {
        List<PageCatalogEntry> pages = jdbc.query(
                "SELECT page_id AS id, page_code AS code, page_name AS name, page_url AS url "
                        + "FROM app_pages WHERE page_id=?",
                (rs, row) -> {
                    PageCatalogEntry page = new PageCatalogEntry();
                    page.setId(rs.getInt("id")); page.setCode(rs.getString("code"));
                    page.setName(rs.getString("name")); page.setUrl(rs.getString("url"));
                    return page;
                }, id);
        return pages.isEmpty() ? null : pages.get(0);
    }

    public void savePage(int id, String code, String name, String url) {
        if (id > 0) jdbc.update("UPDATE app_pages SET page_code=?, page_name=?, page_url=? WHERE page_id=?", code, name, url, id);
        else jdbc.update("INSERT INTO app_pages (page_code, page_name, page_url) VALUES (?, ?, ?)", code, name, url);
    }

    public void deletePage(int id) { jdbc.update("DELETE FROM app_pages WHERE page_id=?", id); }

    public void saveButton(int id, int pageId, String code, String name) {
        if (id > 0) jdbc.update("UPDATE app_page_buttons SET page_id=?, button_code=?, button_name=? WHERE button_id=?", pageId, code, name, id);
        else jdbc.update("INSERT INTO app_page_buttons (page_id, button_code, button_name) VALUES (?, ?, ?)", pageId, code, name);
    }

    public void deleteButton(int id) { jdbc.update("DELETE FROM app_page_buttons WHERE button_id=?", id); }
}

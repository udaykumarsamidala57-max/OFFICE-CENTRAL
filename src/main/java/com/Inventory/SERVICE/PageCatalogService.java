package com.Inventory.SERVICE;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.Inventory.Bean.PageCatalogEntry;
import com.Inventory.DAO.PageCatalogDAO;

@Service
public class PageCatalogService {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,99}");
    private final PageCatalogDAO dao;

    public PageCatalogService(PageCatalogDAO dao) { this.dao = dao; }
    public List<PageCatalogEntry> findAll() { return dao.findAll(); }
    public PageCatalogEntry findPage(int id) { return dao.findPage(id); }

    public void savePage(int id, String code, String name, String url) {
        String safeCode = required(code, "Page code").toUpperCase();
        String safeName = required(name, "Page name");
        String safeUrl = required(url, "Page URL");
        while (safeUrl.length() > 1 && safeUrl.endsWith("/")) safeUrl = safeUrl.substring(0, safeUrl.length() - 1);
        if (!CODE.matcher(safeCode).matches()) throw new IllegalArgumentException("Use an uppercase page code with letters, numbers, and underscores.");
        if (safeName.length() > 150) throw new IllegalArgumentException("Page name must be 150 characters or fewer.");
        if (safeUrl.length() > 255 || !safeUrl.startsWith("/") || safeUrl.startsWith("//") || safeUrl.contains("..")) {
            throw new IllegalArgumentException("Enter a local application path beginning with /.");
        }
        dao.savePage(id, safeCode, safeName, safeUrl);
    }

    public void saveButton(int id, int pageId, String code, String name) {
        if (dao.findPage(pageId) == null) throw new IllegalArgumentException("Select a valid page for this button.");
        String safeCode = required(code, "Button code").toUpperCase();
        String safeName = required(name, "Button name");
        if (!CODE.matcher(safeCode).matches()) throw new IllegalArgumentException("Use an uppercase button code with letters, numbers, and underscores.");
        if (safeName.length() > 150) throw new IllegalArgumentException("Button name must be 150 characters or fewer.");
        dao.saveButton(id, pageId, safeCode, safeName);
    }

    public void deletePage(int id) { dao.deletePage(id); }
    public void deleteButton(int id) { dao.deleteButton(id); }

    private String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }
}

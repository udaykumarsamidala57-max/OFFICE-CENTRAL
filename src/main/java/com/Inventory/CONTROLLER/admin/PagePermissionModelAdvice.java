package com.Inventory.CONTROLLER.admin;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.Inventory.Bean.PageCatalogEntry;
import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.SESSION.SessionKeys;

@ControllerAdvice
public class PagePermissionModelAdvice {
    private final PageAccessService pageAccessService;

    public PagePermissionModelAdvice(PageAccessService pageAccessService) {
        this.pageAccessService = pageAccessService;
    }

    @ModelAttribute
    public void addPermissionModel(Model model, HttpServletRequest request, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute(SessionKeys.AUTHENTICATED))) {
            model.addAttribute("accessiblePages", Collections.emptyList());
            model.addAttribute("navigationPages", Collections.emptyList());
            model.addAttribute("navigationModules", Collections.emptyMap());
            model.addAttribute("navigationModuleIcons", navigationModuleIcons());
            model.addAttribute("currentNavigationModule", "");
            model.addAttribute("allowedButtonCodes", Set.of());
            model.addAttribute("isSuperAdmin", false);
            model.addAttribute("currentRequestPath", request.getRequestURI());
            return;
        }
        Object idValue = session.getAttribute(SessionKeys.USER_ID);
        Integer userId = idValue instanceof Number ? ((Number) idValue).intValue() : null;
        String role = (String) session.getAttribute(SessionKeys.ROLE);
        String path = request.getRequestURI().substring(request.getContextPath().length());
        List<PageCatalogEntry> pages = Collections.emptyList();
        Set<String> buttons = Set.of();
        try {
            pages = pageAccessService.findAccessiblePages(userId, role);
            buttons = pageAccessService.findAllowedButtonCodes(userId, role, path);
        } catch (DataAccessException ignored) {
            // The page controller/interceptor supplies the appropriate error for this condition.
        }
        model.addAttribute("accessiblePages", pages);
        // The menu mirrors the granted page list. Super Admin receives every registered page.
        model.addAttribute("navigationPages", pages);
        model.addAttribute("navigationModules", groupNavigationPages(pages));
        model.addAttribute("navigationModuleIcons", navigationModuleIcons());
        model.addAttribute("currentNavigationModule", moduleForPath(path));
        model.addAttribute("allowedButtonCodes", buttons);
        model.addAttribute("isSuperAdmin", Boolean.TRUE.equals(session.getAttribute(SessionKeys.SUPER_ADMIN))
                || com.Inventory.SESSION.RoleAccess.isSuperAdmin(role));
        model.addAttribute("currentRequestPath", path);
    }

    private Map<String, List<PageCatalogEntry>> groupNavigationPages(List<PageCatalogEntry> pages) {
        Map<String, List<PageCatalogEntry>> grouped = new LinkedHashMap<>();
        for (String name : List.of("Indent Records", "Stock Dispersal", "Purchase Execution", "Dining Hall Operations", "Analytics Hub", "System Masters", "Other")) {
            grouped.put(name, new ArrayList<>());
        }
        for (PageCatalogEntry page : pages) {
            grouped.get(moduleForPath(page.getUrl())).add(page);
        }
        grouped.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return grouped;
    }

    private String moduleForPath(String path) {
        if (path == null) return "Other";
        if (path.startsWith("/Indent") || path.startsWith("/AIndent")) return "Indent Records";
        if (path.startsWith("/issues")) return "Stock Dispersal";
        if (path.startsWith("/purchase-orders") || path.startsWith("/grn")) return "Purchase Execution";
        if (path.startsWith("/dining-hall")) return "Dining Hall Operations";
        if (path.startsWith("/stock")) return "Analytics Hub";
        if (path.startsWith("/admin")) return "System Masters";
        return "Other";
    }

    private Map<String, String> navigationModuleIcons() {
        Map<String, String> icons = new LinkedHashMap<>();
        icons.put("Indent Records", "▤");
        icons.put("Stock Dispersal", "▦");
        icons.put("Purchase Execution", "▣");
        icons.put("Dining Hall Operations", "♨");
        icons.put("Analytics Hub", "▥");
        icons.put("System Masters", "⚙");
        icons.put("Other", "•");
        return icons;
    }
}

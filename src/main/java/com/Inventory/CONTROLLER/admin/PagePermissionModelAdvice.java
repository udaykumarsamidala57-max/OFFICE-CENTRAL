package com.Inventory.CONTROLLER.admin;

import java.util.Collections;
import java.util.List;
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
        model.addAttribute("allowedButtonCodes", buttons);
        model.addAttribute("isSuperAdmin", Boolean.TRUE.equals(session.getAttribute(SessionKeys.SUPER_ADMIN))
                || com.Inventory.SESSION.RoleAccess.isSuperAdmin(role));
        model.addAttribute("currentRequestPath", path);
    }
}

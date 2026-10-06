package com.Inventory.CONTROLLER.admin;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.Bean.PageCatalogEntry;
import com.Inventory.SESSION.SessionKeys;
import com.Inventory.SESSION.RoleAccess;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class PageAccessController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PageAccessController.class);
    private final PageAccessService service;

    public PageAccessController(PageAccessService service) { this.service = service; }

    @GetMapping("/admin/page-access")
    public String page(@RequestParam(defaultValue = "USER") String subjectType,
                       @RequestParam(required = false) Integer userId,
                       @RequestParam(required = false) String role,
                       Model model,
                       @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String currentRole,
                       @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
                       @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {
        requireAdmin(currentRole);
        String type = "ROLE".equalsIgnoreCase(subjectType) ? "ROLE" : "USER";
        boolean superAdminSubject = false;
        String queryStage = "users table";
        try {
            model.addAttribute("users", service.findUsers());
            model.addAttribute("roles", service.findRoles());
            queryStage = "page catalog tables (app_pages / app_page_buttons)";
            List<PageCatalogEntry> pages = service.findPages();
            queryStage = "selected user role";
            superAdminSubject = service.isSuperAdminSubject(type, userId, role);
            queryStage = "page grants for the selected " + ("USER".equals(type) ? "user" : "role");
            model.addAttribute("accessPages", service.load(type, userId, role, pages, superAdminSubject));
        } catch (DataAccessException ex) {
            LOGGER.error("Page access page failed while loading {} from the SRS authentication database", queryStage, ex);
            model.addAttribute("users", java.util.Collections.emptyList());
            model.addAttribute("roles", java.util.Collections.emptyList());
            model.addAttribute("accessPages", java.util.Collections.emptyList());
            model.addAttribute("error", "Could not load " + queryStage
                    + ". Run page_catalog_schema.sql in the inventory database, then restart the app.");
        }
        model.addAttribute("subjectType", type);
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("selectedRole", role);
        model.addAttribute("superAdminSubject", superAdminSubject);
        model.addAttribute("loggedInUser", username);
        model.addAttribute("selectedCompany", company);
        model.addAttribute("userRole", currentRole);
        model.addAttribute("userDepartment", "");
        return "admin/page-access";
    }

    @PostMapping("/admin/page-access/save")
    public String save(@RequestParam String subjectType,
                       @RequestParam(required = false) Integer userId,
                       @RequestParam(required = false) String role,
                       @RequestParam int pageId,
                       @RequestParam(required = false) String pageAccess,
                       @RequestParam(required = false) List<Integer> buttonIds,
                       @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String currentRole,
                       @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer currentUserId,
                       RedirectAttributes flash) {
        requireAdmin(currentRole);
        if (!service.hasButtonAccess(currentUserId, currentRole, "/admin/page-access", "SAVE_ACCESS")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Saving access is not assigned to your account or role.");
        }
        String type = "ROLE".equalsIgnoreCase(subjectType) ? "ROLE" : "USER";
        try {
            service.save(type, userId, role, pageId, "true".equalsIgnoreCase(pageAccess), buttonIds);
            flash.addFlashAttribute("success", "Access permissions saved.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "Could not save these permissions. Refresh and try again.");
        } catch (DataAccessException ex) {
            LOGGER.error("Database failure while saving page/button access for page {}", pageId, ex);
            flash.addFlashAttribute("error", "Access tables are unavailable. Run the updated page_catalog_schema.sql on the SRS authentication database.");
        } catch (RuntimeException ex) {
            LOGGER.error("Failed saving page/button access for page {}", pageId, ex);
            flash.addFlashAttribute("error", "Could not save access. Check that page_catalog_schema.sql is installed on the SRS authentication database, then retry.");
        }
        flash.addAttribute("subjectType", type);
        if ("USER".equals(type) && userId != null) flash.addAttribute("userId", userId);
        if ("ROLE".equals(type) && role != null) flash.addAttribute("role", role);
        return "redirect:/admin/page-access";
    }

    private void requireAdmin(String role) {
        if (!RoleAccess.isPageAdministrator(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
    }
}

package com.Inventory.CONTROLLER.admin;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.Inventory.SERVICE.PageCatalogService;
import com.Inventory.SESSION.SessionKeys;
import com.Inventory.SESSION.RoleAccess;
import com.Inventory.SERVICE.PageAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PageCatalogController {
    private final PageCatalogService service;
    private final PageAccessService pageAccessService;

    public PageCatalogController(PageCatalogService service, PageAccessService pageAccessService) {
        this.service = service;
        this.pageAccessService = pageAccessService;
    }

    @GetMapping("/admin/page-catalog")
    public String page(Model model,
                       @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                       @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
                       @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {
        requireAdmin(role);
        model.addAttribute("pages", service.findAll());
        model.addAttribute("loggedInUser", username);
        model.addAttribute("selectedCompany", company);
        model.addAttribute("userRole", role);
        model.addAttribute("userDepartment", "");
        return "admin/page-catalog";
    }

    @PostMapping("/admin/page-catalog/page/save")
    public String savePage(@RequestParam(defaultValue = "0") int id,
                           @RequestParam String code, @RequestParam String name, @RequestParam String url,
                           @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                           @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                           RedirectAttributes flash) {
        requireAdmin(role);
        requireButton(userId, role, id == 0 ? "CREATE_PAGE" : "EDIT_PAGE");
        try {
            service.savePage(id, code, name, url);
            flash.addFlashAttribute("success", id == 0 ? "Page created." : "Page updated.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "That page code or URL is already registered.");
        }
        return "redirect:/admin/page-catalog";
    }

    @PostMapping("/admin/page-catalog/page/delete")
    public String deletePage(@RequestParam int id,
                             @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                             @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                             RedirectAttributes flash) {
        requireAdmin(role);
        requireButton(userId, role, "DELETE_PAGE");
        service.deletePage(id);
        flash.addFlashAttribute("success", "Page and its button definitions deleted.");
        return "redirect:/admin/page-catalog";
    }

    @PostMapping("/admin/page-catalog/button/save")
    public String saveButton(@RequestParam(defaultValue = "0") int id, @RequestParam int pageId,
                             @RequestParam String code, @RequestParam String name,
                             @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                             @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                             RedirectAttributes flash) {
        requireAdmin(role);
        requireButton(userId, role, id == 0 ? "ADD_BUTTON" : "EDIT_BUTTON");
        try {
            service.saveButton(id, pageId, code, name);
            flash.addFlashAttribute("success", id == 0 ? "Button added." : "Button updated.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "That button code is already registered on this page.");
        }
        return "redirect:/admin/page-catalog";
    }

    @PostMapping("/admin/page-catalog/button/delete")
    public String deleteButton(@RequestParam int id,
                               @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                               @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                               RedirectAttributes flash) {
        requireAdmin(role);
        requireButton(userId, role, "DELETE_BUTTON");
        service.deleteButton(id);
        flash.addFlashAttribute("success", "Button definition deleted.");
        return "redirect:/admin/page-catalog";
    }

    private void requireAdmin(String role) {
        if (!RoleAccess.isPageAdministrator(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
    }

    private void requireButton(Integer userId, String role, String code) {
        if (!pageAccessService.hasButtonAccess(userId, role, "/admin/page-catalog", code)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This page action is not assigned to your account or role.");
        }
    }
}

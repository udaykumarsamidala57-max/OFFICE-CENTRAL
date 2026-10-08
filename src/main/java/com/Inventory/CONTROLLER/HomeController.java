package com.Inventory.CONTROLLER;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.Inventory.Bean.PageCatalogEntry;
import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.SESSION.SessionKeys;

@Controller
public class HomeController {
    private final PageAccessService pageAccessService;

    public HomeController(PageAccessService pageAccessService) {
        this.pageAccessService = pageAccessService;
    }

    @GetMapping("/")
    public String start() {
        return "redirect:/login";
    }

    @GetMapping("/Home")
    public String home(HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute(SessionKeys.AUTHENTICATED))) {
            return "redirect:/login";
        }
        if (session.getAttribute(SessionKeys.SELECTED_COMPANY) == null) {
            return "redirect:/select-company";
        }

        Object idValue = session.getAttribute(SessionKeys.USER_ID);
        Integer userId = idValue instanceof Number ? ((Number) idValue).intValue() : null;
        String role = (String) session.getAttribute(SessionKeys.ROLE);
        try {
            List<PageCatalogEntry> pages = pageAccessService.findAccessiblePages(userId, role);
            for (PageCatalogEntry page : pages) {
                String url = page.getUrl();
                if (url != null && url.startsWith("/") && !url.startsWith("//")) return "redirect:" + url;
            }
            model.addAttribute("accessMessage", "No application pages are assigned to your account yet. Contact your administrator to request access.");
        } catch (DataAccessException ex) {
            model.addAttribute("accessMessage", "Your page access could not be loaded. Please contact your administrator.");
        }
        model.addAttribute("selectedCompany", session.getAttribute(SessionKeys.SELECTED_COMPANY));
        return "login/no-access";
    }
}

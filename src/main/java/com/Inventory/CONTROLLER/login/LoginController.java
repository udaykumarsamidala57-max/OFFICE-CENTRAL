package com.Inventory.CONTROLLER.login;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Inventory.Bean.UserAccount;
import com.Inventory.DBUtil.DBUtil1;
import com.Inventory.DAO.LoginDAO;
import com.Inventory.SERVICE.LoginService;
import com.Inventory.SESSION.SessionKeys;
import com.Inventory.SESSION.RoleAccess;

@Controller
public class LoginController {

    private final LoginService loginService;
    private final LoginDAO loginDAO;

    public LoginController(LoginService loginService, LoginDAO loginDAO) {
        this.loginService = loginService;
        this.loginDAO = loginDAO;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (Boolean.TRUE.equals(session.getAttribute(SessionKeys.AUTHENTICATED))) {
            return session.getAttribute(SessionKeys.SELECTED_COMPANY) == null
                    ? "redirect:/select-company" : "redirect:/Home";
        }
        return "login/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletRequest request,
                        Model model) {
        try {
            Optional<UserAccount> result = loginService.authenticate(username, password);
            if (result.isEmpty()) {
                model.addAttribute("error", "Username or password is incorrect.");
                return "login/login";
            }

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) oldSession.invalidate();
            HttpSession session = request.getSession(true);
            UserAccount user = result.get();
            session.setAttribute(SessionKeys.AUTHENTICATED, Boolean.TRUE);
            session.setAttribute(SessionKeys.USER_ID, user.getId());
            session.setAttribute(SessionKeys.SUPER_ADMIN, RoleAccess.isSuperAdmin(user.getRole()));
            session.setAttribute(SessionKeys.USERNAME, user.getUsername());
            session.setAttribute(SessionKeys.ROLE, user.getRole());
            session.setAttribute(SessionKeys.DEPARTMENT, user.getDepartment());
            session.setAttribute(SessionKeys.MAIL, user.getMail());
            session.setAttribute(SessionKeys.USER_BRANCH, user.getBranch());
            session.setMaxInactiveInterval(30 * 60);
            return "redirect:/select-company";
        } catch (DataAccessException | IllegalStateException ex) {
            model.addAttribute("error", "Login is temporarily unavailable. Check the database configuration.");
            return "login/login";
        }
    }

    @GetMapping("/select-company")
    public String companyPage(HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute(SessionKeys.AUTHENTICATED))) {
            return "redirect:/login";
        }
        populateCompanyPage(session, model, null);
        return "login/select-company";
    }

    @PostMapping("/select-company")
    public String selectCompany(@RequestParam String company,
                                HttpSession session,
                                Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute(SessionKeys.AUTHENTICATED))) {
            return "redirect:/login";
        }
        try {
            List<String> companies = loginDAO.findAllowedDatabases((Integer) session.getAttribute(SessionKeys.USER_ID));
            String canonical = DBUtil1.canonicalCompany(company);
            if (!companies.contains(canonical)) {
                populateCompanyPage(session, model, company);
                model.addAttribute("error", "You do not have access to that company. Contact your administrator.");
                return "login/select-company";
            }
            try (Connection ignored = DBUtil1.getConnection(canonical)) {
                session.setAttribute(SessionKeys.SELECTED_COMPANY, canonical);
            }
            return "redirect:/Home";
        } catch (org.springframework.dao.DataAccessException ex) {
            populateCompanyPage(session, model, company);
            model.addAttribute("error", "Your company access could not be loaded. Contact your administrator.");
            return "login/select-company";
        } catch (Exception ex) {
            populateCompanyPage(session, model, company);
            model.addAttribute("error", "Could not connect to that company database. Please try again.");
            return "login/select-company";
        }
    }

    private void populateCompanyPage(HttpSession session, Model model, String requested) {
        model.addAttribute("username", session.getAttribute(SessionKeys.USERNAME));
        try {
            List<String> companies = loginDAO.findAllowedDatabases((Integer) session.getAttribute(SessionKeys.USER_ID));
            model.addAttribute("companies", companies);
            Object current = session.getAttribute(SessionKeys.SELECTED_COMPANY);
            String selected = requested != null ? requested : (current == null ? null : current.toString());
            model.addAttribute("selectedCompany", selected);
            if (companies.isEmpty()) {
                model.addAttribute("error", "No company databases are assigned to your account. Contact your administrator.");
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            model.addAttribute("companies", java.util.Collections.emptyList());
            model.addAttribute("selectedCompany", null);
            model.addAttribute("error", "Your company access could not be loaded. Contact your administrator.");
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        redirectAttributes.addFlashAttribute("message", "You have been logged out.");
        return "redirect:/login";
    }
}

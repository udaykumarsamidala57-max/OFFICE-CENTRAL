package com.Inventory.CONTROLLER;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.Inventory.Bean.HomeIndentStage;
import com.Inventory.Bean.IndentListItem;
import com.Inventory.Bean.PageCatalogEntry;
import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.SESSION.SessionKeys;
import com.Inventory.modules.INDENT.DAO.IndentListDAO;

@Controller
public class HomeController {
    private final PageAccessService pageAccessService;
    private final IndentListDAO indentListDAO;

    public HomeController(PageAccessService pageAccessService, IndentListDAO indentListDAO) {
        this.pageAccessService = pageAccessService;
        this.indentListDAO = indentListDAO;
    }

    @GetMapping("/")
    public String start() {
        return "redirect:/Home";
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
        String department = (String) session.getAttribute(SessionKeys.DEPARTMENT);
        model.addAttribute("selectedCompany", session.getAttribute(SessionKeys.SELECTED_COMPANY));
        model.addAttribute("loggedInUser", session.getAttribute(SessionKeys.USERNAME));
        model.addAttribute("userRole", role == null ? "" : role);
        model.addAttribute("userDepartment", department == null ? "" : department);

        try {
            List<PageCatalogEntry> pages = pageAccessService.findAccessiblePages(userId, role);
            if (pages.isEmpty()) {
                model.addAttribute("accessMessage", "No application pages are assigned to your account yet. Contact your administrator to request access.");
                return "login/no-access";
            }

            boolean canViewIndentReport = pageAccessService.hasPageAccess(userId, role, "/IndentlistServlet");
            model.addAttribute("canViewIndentReport", canViewIndentReport);
            if (canViewIndentReport) {
                List<IndentListItem> rows = indentListDAO.findRecent(role, department);
                model.addAttribute("indentStages", buildStages(rows));
                Set<String> indentNumbers = new LinkedHashSet<>();
                for (IndentListItem row : rows) {
                    if (row.getIndentNo() != null) indentNumbers.add(row.getIndentNo());
                }
                model.addAttribute("totalIndentCount", indentNumbers.size());
            } else {
                model.addAttribute("indentStages", buildStages(List.of()));
                model.addAttribute("totalIndentCount", 0);
            }
        } catch (DataAccessException ex) {
            model.addAttribute("canViewIndentReport", false);
            model.addAttribute("indentStages", buildStages(List.of()));
            model.addAttribute("totalIndentCount", 0);
            model.addAttribute("dashboardMessage", "Indent stages could not be loaded. Please refresh or contact your administrator.");
        }
        return "home";
    }

    private List<HomeIndentStage> buildStages(List<IndentListItem> rows) {
        Map<String, List<IndentListItem>> grouped = new LinkedHashMap<>();
        grouped.put("level-one", new ArrayList<>());
        grouped.put("final-approval", new ArrayList<>());
        grouped.put("processing", new ArrayList<>());
        grouped.put("management-note", new ArrayList<>());
        grouped.put("closed", new ArrayList<>());

        for (IndentListItem row : rows) {
            grouped.get(stageKey(row)).add(row);
        }

        List<HomeIndentStage> stages = new ArrayList<>();
        stages.add(new HomeIndentStage("Awaiting Level 1 Approval", "Submitted and waiting for the first approval.", grouped.get("level-one")));
        stages.add(new HomeIndentStage("Awaiting Final Approval", "Level 1 is complete; final review is pending.", grouped.get("final-approval")));
        stages.add(new HomeIndentStage("Approved / Processing", "Approved indents moving to purchase or issue.", grouped.get("processing")));
        stages.add(new HomeIndentStage("Management Note", "Reviewed and routed as a management note.", grouped.get("management-note")));
        stages.add(new HomeIndentStage("Rejected / Cancelled", "Rejected or cancelled indents.", grouped.get("closed")));
        return stages;
    }

    private String stageKey(IndentListItem item) {
        String levelOne = clean(item.getLevelOneStatus());
        String levelTwo = clean(item.getLevelTwoStatus());
        String next = clean(item.getIndentStatus());

        if (levelOne.equals("rejected") || levelTwo.equals("rejected") || next.equals("rejected")
                || levelOne.equals("cancelled") || levelTwo.equals("cancelled") || next.equals("cancelled")) {
            return "closed";
        }
        if (next.equals("management note")) return "management-note";
        if (levelTwo.equals("approved") || next.equals("po") || next.equals("issue")
                || next.equals("issued") || next.equals("po raised")) {
            return "processing";
        }
        if (levelOne.equals("approved")) return "final-approval";
        return "level-one";
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
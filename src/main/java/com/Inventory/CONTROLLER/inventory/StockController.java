package com.Inventory.CONTROLLER.inventory;

import java.time.LocalDate;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.Inventory.SERVICE.IssueStockService;
import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.SESSION.SessionKeys;

@Controller
public class StockController {
    private final IssueStockService service;
    private final PageAccessService pageAccess;

    public StockController(IssueStockService service, PageAccessService pageAccess) {
        this.service = service; this.pageAccess = pageAccess;
    }

    @GetMapping("/stock")
    public String currentStock(@RequestParam(required=false) String category, @RequestParam(required=false) String search,
            Model model, @RequestAttribute(name=SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role,
            @RequestAttribute(name=SessionKeys.REQUEST_DEPARTMENT, required=false) String department,
            @RequestAttribute(name=SessionKeys.REQUEST_USER_ID, required=false) Integer userId,
            @RequestAttribute(name=SessionKeys.REQUEST_COMPANY) String company) {
        session(model, username, role, department, company);
        model.addAttribute("category", category == null ? "" : category);
        model.addAttribute("search", search == null ? "" : search);
        try {
            if (!blank(category) || !blank(search)) requireButton(userId, role, "/stock", "FILTER_STOCK");
            model.addAttribute("categories", service.categories());
            model.addAttribute("stockRows", service.currentStock(category, search));
        } catch (IllegalArgumentException ex) { model.addAttribute("stockRows", java.util.List.of()); model.addAttribute("error", ex.getMessage()); }
        catch (DataAccessException ex) { model.addAttribute("stockRows", java.util.List.of()); model.addAttribute("error", "Could not load stock. Check the selected company database tables."); }
        return "inventory/stock";
    }

    @GetMapping("/stock/report")
    public String stockReport(@RequestParam(required=false) String fromDate, @RequestParam(required=false) String toDate,
            @RequestParam(required=false) String category, @RequestParam(required=false) String subCategory,
            @RequestParam(required=false) String export, Model model,
            @RequestAttribute(name=SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role,
            @RequestAttribute(name=SessionKeys.REQUEST_DEPARTMENT, required=false) String department,
            @RequestAttribute(name=SessionKeys.REQUEST_USER_ID, required=false) Integer userId,
            @RequestAttribute(name=SessionKeys.REQUEST_COMPANY) String company) {
        session(model, username, role, department, company);
        LocalDate today = LocalDate.now();
        String from = blank(fromDate) ? today.withDayOfMonth(1).toString() : fromDate;
        String to = blank(toDate) ? today.toString() : toDate;
        model.addAttribute("fromDate", from); model.addAttribute("toDate", to);
        model.addAttribute("category", category == null ? "" : category);
        model.addAttribute("subCategory", subCategory == null ? "" : subCategory);
        try {
            if (!blank(fromDate) || !blank(toDate) || !blank(category) || !blank(subCategory))
                requireButton(userId, role, "/stock/report", "FILTER_STOCK_REPORT");
            if ("csv".equalsIgnoreCase(export)) requireButton(userId, role, "/stock/report", "EXPORT_STOCK_REPORT");
            model.addAttribute("categories", service.categories());
            model.addAttribute("stockRows", service.stockSummary(from, to, category, subCategory));
        } catch (IllegalArgumentException ex) { model.addAttribute("stockRows", java.util.List.of()); model.addAttribute("error", ex.getMessage()); }
        catch (DataAccessException ex) { model.addAttribute("stockRows", java.util.List.of()); model.addAttribute("error", "Could not load stock movements. Check the stock ledger and item tables."); }
        return "inventory/stock-report";
    }

    private void requireButton(Integer userId, String role, String path, String code) {
        if (!pageAccess.hasButtonAccess(userId, role, path, code))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission for this inventory action.");
    }
    private void session(Model model, String user, String role, String department, String company) {
        model.addAttribute("loggedInUser", user); model.addAttribute("userRole", role == null ? "" : role);
        model.addAttribute("userDepartment", department == null ? "" : department); model.addAttribute("selectedCompany", company);
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}

package com.Inventory.modules.stock.CONTROLLER;

import java.time.LocalDate;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Inventory.modules.stock.SERVICE.IssueStockService;
import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.SESSION.SessionKeys;

@Controller
public class IssueStockController {
    private final IssueStockService service;
    private final PageAccessService pageAccess;

    public IssueStockController(IssueStockService service, PageAccessService pageAccess) {
        this.service = service; this.pageAccess = pageAccess;
    }

    @GetMapping("/issues/entry")
    public String issueEntry(Model model,
            @RequestAttribute(name=SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role,
            @RequestAttribute(name=SessionKeys.REQUEST_DEPARTMENT, required=false) String department,
            @RequestAttribute(name=SessionKeys.REQUEST_COMPANY) String company) {
        session(model, username, role, department, company);
        model.addAttribute("today", LocalDate.now().toString());
        try { model.addAttribute("pendingIssues", service.pendingIssues()); }
        catch (DataAccessException ex) {
            model.addAttribute("pendingIssues", java.util.List.of());
            model.addAttribute("error", "Could not load approved indents or stock. Check the selected company database tables.");
        }
        return "modules/stock/issue-entry";
    }

    @PostMapping("/issues/entry")
    public String issue(@RequestParam int indentId, @RequestParam String qtyIssued,
            @RequestParam String unitPrice, @RequestParam String issueDate,
            @RequestAttribute(name=SessionKeys.REQUEST_USER_ID, required=false) Integer userId,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role,
            RedirectAttributes flash) {
        requireButton(userId, role, "/issues/entry", "PROCESS_ISSUE");
        try {
            String issueNo = service.issue(indentId, qtyIssued, unitPrice, issueDate);
            flash.addFlashAttribute("success", "Issue " + issueNo + " was posted and stock was updated.");
        } catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        catch (DataAccessException ex) { flash.addFlashAttribute("error", "Issue could not be posted. Check that the stock issue and stock ledger tables exist."); }
        return "redirect:/issues/entry";
    }

    @GetMapping("/issues/report")
    public String issueReport(@RequestParam(required=false) String fromDate,
            @RequestParam(required=false) String toDate, @RequestParam(required=false) String department,
            @RequestParam(required=false) String search, @RequestParam(required=false) String printIndentNo,
            Model model, @RequestAttribute(name=SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role,
            @RequestAttribute(name=SessionKeys.REQUEST_DEPARTMENT, required=false) String userDepartment,
            @RequestAttribute(name=SessionKeys.REQUEST_USER_ID, required=false) Integer userId,
            @RequestAttribute(name=SessionKeys.REQUEST_COMPANY) String company) {
        session(model, username, role, userDepartment, company);
        model.addAttribute("fromDate", fromDate == null ? "" : fromDate);
        model.addAttribute("toDate", toDate == null ? "" : toDate);
        model.addAttribute("department", department == null ? "" : department);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("issueRows", java.util.List.of());
        model.addAttribute("voucherRows", java.util.List.of());
        try {
            model.addAttribute("departments", service.departments());
            if (printIndentNo != null && !printIndentNo.isBlank()) {
                requireButton(userId, role, "/issues/report", "PRINT_ISSUE_VOUCHER");
                model.addAttribute("voucherRows", service.issueVoucher(printIndentNo.trim()));
                model.addAttribute("printIndentNo", printIndentNo.trim());
            } else {
                if (!blank(fromDate) || !blank(toDate) || !blank(department) || !blank(search))
                    requireButton(userId, role, "/issues/report", "FILTER_ISSUE_REPORT");
                model.addAttribute("issueRows", service.issueReport(fromDate, toDate, department, search));
            }
        } catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); }
        catch (DataAccessException ex) { model.addAttribute("error", "Could not load issue records. Check the selected company database tables."); }
        return "modules/stock/issue-report";
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

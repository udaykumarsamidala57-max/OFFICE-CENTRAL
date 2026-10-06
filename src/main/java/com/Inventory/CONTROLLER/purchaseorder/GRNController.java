package com.Inventory.CONTROLLER.purchaseorder;

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

import com.Inventory.Bean.GoodsReceivedNote;
import com.Inventory.SERVICE.GRNService;
import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.SESSION.SessionKeys;

@Controller
public class GRNController {
    private final GRNService grnService;
    private final PageAccessService pageAccessService;

    public GRNController(GRNService grnService, PageAccessService pageAccessService) {
        this.grnService = grnService;
        this.pageAccessService = pageAccessService;
    }

    @GetMapping("/grn/entry")
    public String entry(@RequestParam(required = false) String poNumber,
                        Model model,
                        @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
                        @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                        @RequestAttribute(name = SessionKeys.REQUEST_DEPARTMENT, required = false) String department,
                        @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                        @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {
        addSession(model, username, role, department, company);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("defaultReceivedBy", username);
        if (poNumber != null && !poNumber.isBlank()) {
            requireButton(userId, role, "/purchase-orders/approvals", "CREATE_GRN");
            try {
                model.addAttribute("entry", grnService.prepareEntry(poNumber));
            } catch (IllegalArgumentException ex) {
                model.addAttribute("error", ex.getMessage());
            } catch (DataAccessException ex) {
                model.addAttribute("error", "Could not load the purchase order or GRN tables. Run the GRN schema on the selected company database.");
            }
        }
        model.addAttribute("poNumber", poNumber == null ? "" : poNumber.trim());
        return "purchaseorder/grn-entry";
    }

    @PostMapping("/grn/entry")
    public String save(@RequestParam String poNumber,
                       @RequestParam String grnDate,
                       @RequestParam String invoiceNo,
                       @RequestParam String invoiceDate,
                       @RequestParam String receivedBy,
                       @RequestParam(required = false) String remarks,
                       @RequestParam(required = false) int[] poItemId,
                       @RequestParam(required = false) String[] qtyReceived,
                       @RequestParam(required = false) String[] qtyAccepted,
                       @RequestParam(required = false) String[] qtyRejected,
                       @RequestParam(required = false) String[] itemRemarks,
                       @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                       @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                       RedirectAttributes flash) {
        requireButton(userId, role, "/grn/entry", "SAVE_GRN");
        try {
            String grnNo = grnService.create(poNumber, grnDate, invoiceNo, invoiceDate, receivedBy,
                    remarks, poItemId, qtyReceived, qtyAccepted, qtyRejected, itemRemarks);
            flash.addFlashAttribute("success", "GRN " + grnNo + " was saved. Accepted quantities were added to stock.");
            return "redirect:/grn/report";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            flash.addFlashAttribute("error", "GRN could not be saved. Confirm the GRN schema and stock tables exist in the selected company database.");
        }
        flash.addAttribute("poNumber", poNumber);
        return "redirect:/grn/entry";
    }

    @GetMapping("/grn/report")
    public String report(@RequestParam(required = false) String search,
                         @RequestParam(required = false) String fromDate,
                         @RequestParam(required = false) String toDate,
                         @RequestParam(required = false) String printNumber,
                         Model model,
                         @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
                         @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                         @RequestAttribute(name = SessionKeys.REQUEST_DEPARTMENT, required = false) String department,
                         @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                         @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {
        addSession(model, username, role, department, company);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("fromDate", fromDate == null ? "" : fromDate);
        model.addAttribute("toDate", toDate == null ? "" : toDate);
        model.addAttribute("records", java.util.List.of());
        model.addAttribute("printGrn", null);
        try {
            if (printNumber != null && !printNumber.isBlank()) {
                requireButton(userId, role, "/grn/report", "PRINT_GRN");
                GoodsReceivedNote note = grnService.findByNumber(printNumber);
                if (note == null) model.addAttribute("error", "GRN was not found.");
                model.addAttribute("printGrn", note);
            } else {
                if ((search != null && !search.isBlank()) || (fromDate != null && !fromDate.isBlank())
                        || (toDate != null && !toDate.isBlank())) {
                    requireButton(userId, role, "/grn/report", "FILTER_GRN");
                }
                model.addAttribute("records", grnService.findReport(search, fromDate, toDate));
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("records", java.util.List.of());
            model.addAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            model.addAttribute("records", java.util.List.of());
            model.addAttribute("error", "Could not load GRNs. Run the GRN schema on the selected company database.");
        }
        return "purchaseorder/grn-report";
    }

    private void requireButton(Integer userId, String role, String path, String code) {
        if (!pageAccessService.hasButtonAccess(userId, role, path, code)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to use this GRN action.");
        }
    }

    private void addSession(Model model, String username, String role, String department, String company) {
        model.addAttribute("loggedInUser", username);
        model.addAttribute("userRole", role == null ? "" : role);
        model.addAttribute("userDepartment", department == null ? "" : department);
        model.addAttribute("selectedCompany", company);
    }
}

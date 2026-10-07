package com.Inventory.modules.PO.CONTROLLER;

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

import java.time.LocalDate;

import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.modules.PO.SERVICE.PurchaseOrderService;
import com.Inventory.SESSION.SessionKeys;
import com.Inventory.SESSION.RoleAccess;

@Controller
public class PurchaseOrderController {
    private final PurchaseOrderService service;
    private final PageAccessService pageAccessService;

    public PurchaseOrderController(PurchaseOrderService service, PageAccessService pageAccessService) {
        this.service = service;
        this.pageAccessService = pageAccessService;
    }

    @GetMapping("/purchase-orders/create")
    public String createPage(@RequestParam(required = false) String[] selectedIds,
                             Model model,
                             @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
                             @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                             @RequestAttribute(name = SessionKeys.REQUEST_DEPARTMENT, required = false) String department,
                             @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                             @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {
        addSessionModel(model, username, role, department, company);
        model.addAttribute("today", LocalDate.now().toString());
        try {
            model.addAttribute("eligibleIndents", service.findEligibleIndents());
            model.addAttribute("nextPONumber", service.nextNumber());
            if (selectedIds != null && selectedIds.length > 0) {
                requireButton(userId, role, "/purchase-orders/create", "PREPARE_PO");
                model.addAttribute("draftItems", service.selectedEligibleIndents(selectedIds));
                model.addAttribute("vendors", service.findVendors());
                model.addAttribute("draftMode", true);
            } else {
                model.addAttribute("draftMode", false);
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("draftMode", false);
        } catch (DataAccessException ex) {
            model.addAttribute("eligibleIndents", java.util.List.of());
            model.addAttribute("draftMode", false);
            model.addAttribute("error", "Could not load the Purchase Order data. Check the selected company database and its PInventory tables.");
        }
        return "modules/PO/create-purchase-order";
    }

    @PostMapping("/purchase-orders/create")
    public String savePurchaseOrder(@RequestParam String[] indentId,
                                    @RequestParam String[] qty,
                                    @RequestParam String[] rate,
                                    @RequestParam String[] discPercent,
                                    @RequestParam String[] gstPercent,
                                    @RequestParam String vendorName,
                                    @RequestParam(required = false) String quotationNo,
                                    @RequestParam String poDate,
                                    @RequestParam(required = false) String billingAddress,
                                    @RequestParam(required = false) String serviceCharge,
                                    @RequestParam(required = false) String serviceGst,
                                    @RequestParam(required = false) String termsConditions,
                                    @RequestParam(required = false) String generalConditions,
                                    @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                                    @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                                    RedirectAttributes flash) {
        requireButton(userId, role, "/purchase-orders/create", "SAVE_PO");
        try {
            String number = service.create(indentId, qty, rate, discPercent, gstPercent, vendorName,
                    quotationNo, poDate, billingAddress, serviceCharge, serviceGst, termsConditions, generalConditions);
            flash.addFlashAttribute("success", "Purchase order " + number + " was created and sent for approval.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            flash.addFlashAttribute("error", "Could not save the purchase order. Check that the selected company database has the PInventory PO tables.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", ex.getMessage() == null ? "Could not save the purchase order." : ex.getMessage());
        }
        return "redirect:/purchase-orders/create";
    }

    @GetMapping("/purchase-orders/approvals")
    public String approvals(@RequestParam(required = false) String search,
                            Model model,
                            @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
                            @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                            @RequestAttribute(name = SessionKeys.REQUEST_DEPARTMENT, required = false) String department,
                            @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                            @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {
        addSessionModel(model, username, role, department, company);
        boolean canPrintPurchaseOrder = pageAccessService.hasButtonAccess(
                userId, role, "/purchase-orders/approvals", "VIEW_PO")
                || pageAccessService.hasButtonAccess(
                        userId, role, "/purchase-orders/report", "PRINT_PO");
        model.addAttribute("canPrintPurchaseOrder", canPrintPurchaseOrder);
        model.addAttribute("canApproveByRole", RoleAccess.isGlobal(role));
        try { model.addAttribute("purchaseOrders", service.findApprovals(search)); }
        catch (DataAccessException ex) {
            model.addAttribute("purchaseOrders", java.util.List.of());
            model.addAttribute("error", "Could not load purchase orders. Check that the selected company database has the PInventory PO tables.");
        }
        model.addAttribute("search", search == null ? "" : search);
        return "modules/PO/approve-purchase-orders";
    }

    @PostMapping("/purchase-orders/approvals")
    public String updateApproval(@RequestParam String action,
                                 @RequestParam String poNumber,
                                 @RequestParam(required = false) String search,
                                 @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                                 @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                                 RedirectAttributes flash) {
        if ("approve".equalsIgnoreCase(action)) {
            if (!RoleAccess.isGlobal(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Global or Super Admin can approve purchase orders.");
            requireButton(userId, role, "/purchase-orders/approvals", "APPROVE_PO");
            try { service.approve(poNumber); flash.addFlashAttribute("success", "Purchase order " + poNumber + " approved."); }
            catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
            catch (DataAccessException ex) { flash.addFlashAttribute("error", "Could not approve this purchase order. Check the company database."); }
        } else if ("delete".equalsIgnoreCase(action)) {
            requireButton(userId, role, "/purchase-orders/approvals", "DELETE_PO");
            try { service.deletePending(poNumber); flash.addFlashAttribute("success", "Pending purchase order " + poNumber + " deleted."); }
            catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
            catch (DataAccessException ex) { flash.addFlashAttribute("error", "Could not delete this purchase order. Check the company database."); }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown purchase order action.");
        }
        if (search != null && !search.isBlank()) flash.addAttribute("search", search);
        return "redirect:/purchase-orders/approvals";
    }

    @GetMapping("/purchase-orders/report")
    public String report(@RequestParam(required = false) String search,
                         @RequestParam(required = false) String fromDate,
                         @RequestParam(required = false) String toDate,
                         @RequestParam(required = false) String approval,
                         @RequestParam(required = false) String printNumber,
                         Model model,
                         @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
                         @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
                         @RequestAttribute(name = SessionKeys.REQUEST_DEPARTMENT, required = false) String department,
                         @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
                         @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {
        addSessionModel(model, username, role, department, company);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("fromDate", fromDate == null ? "" : fromDate);
        model.addAttribute("toDate", toDate == null ? "" : toDate);
        model.addAttribute("approval", approval == null ? "" : approval);
        try {
            if (printNumber != null && !printNumber.isBlank()) {
                requirePrintAccess(userId, role);
                var order = service.findByNumber(printNumber);
                if (order == null) model.addAttribute("error", "Purchase order was not found.");
                boolean hasDiscount = order != null && order.getItems() != null
                        && order.getItems().stream().anyMatch(item ->
                                (item.getDiscountPercent() != null && item.getDiscountPercent().signum() != 0)
                                || (item.getDiscountValue() != null && item.getDiscountValue().signum() != 0));
                model.addAttribute("hasDiscount", hasDiscount);
                model.addAttribute("printOrder", order);
            } else {
                model.addAttribute("purchaseOrders", service.findReport(search, fromDate, toDate, approval));
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("purchaseOrders", java.util.List.of());
            model.addAttribute("error", ex.getMessage());
        } catch (java.time.DateTimeException ex) {
            model.addAttribute("purchaseOrders", java.util.List.of());
            model.addAttribute("error", "Enter valid report dates.");
        } catch (DataAccessException ex) {
            model.addAttribute("purchaseOrders", java.util.List.of());
            model.addAttribute("error", "Could not load purchase orders. Check that the selected company database has the PInventory PO tables.");
        }
        return "modules/PO/purchase-order-report";
    }

    private void requirePrintAccess(Integer userId, String role) {
        boolean allowedFromReport = pageAccessService.hasButtonAccess(
                userId, role, "/purchase-orders/report", "PRINT_PO");
        boolean allowedFromApprovals = pageAccessService.hasButtonAccess(
                userId, role, "/purchase-orders/approvals", "VIEW_PO");
        if (!allowedFromReport && !allowedFromApprovals) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to view or print this purchase order.");
        }
    }
    private void requireButton(Integer userId, String role, String path, String code) {
        if (!pageAccessService.hasButtonAccess(userId, role, path, code)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this purchase order action.");
        }
    }

    private void addSessionModel(Model model, String username, String role, String department, String company) {
        model.addAttribute("loggedInUser", username);
        model.addAttribute("userRole", role == null ? "" : role);
        model.addAttribute("userDepartment", department == null ? "" : department);
        model.addAttribute("selectedCompany", company);
    }
}




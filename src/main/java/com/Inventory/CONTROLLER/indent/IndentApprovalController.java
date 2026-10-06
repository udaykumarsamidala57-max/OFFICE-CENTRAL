package com.Inventory.CONTROLLER.indent;

import java.sql.Date;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.Inventory.SERVICE.IndentApprovalService;
import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.SESSION.SessionKeys;
import com.Inventory.SESSION.RoleAccess;

@Controller
public class IndentApprovalController {
    private final IndentApprovalService service;
    private final PageAccessService pageAccessService;

    public IndentApprovalController(IndentApprovalService service, PageAccessService pageAccessService) {
        this.service = service;
        this.pageAccessService = pageAccessService;
    }

    @GetMapping("/AIndentListServlet")
    public String list(
            Model model,
            @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
            @RequestAttribute(name = SessionKeys.REQUEST_DEPARTMENT, required = false) String department,
            @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {
        requireApprover(role);
        model.addAttribute("approvalGroups", service.getApprovalGroups(role, department));
        model.addAttribute("pendingPerItem", service.getPendingByItem());
        model.addAttribute("loggedInUser", username);
        model.addAttribute("selectedCompany", company);
        model.addAttribute("userRole", role == null ? "" : role);
        model.addAttribute("userDepartment", department == null ? "" : department);
        model.addAttribute("canFinalApprove", RoleAccess.isGlobal(role));
        return "indent/indent-approvals";
    }

    @PostMapping("/AIndentListServlet/approve-level-one")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveLevelOne(
            @RequestParam int id,
            @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
            @RequestAttribute(name = SessionKeys.REQUEST_DEPARTMENT, required = false) String department,
            @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId) {
        if (!pageAccessService.hasButtonAccess(userId, role, "/AIndentListServlet", "APPROVE_L1")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission for L1 approval."));
        }
        if (!isApprover(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You cannot approve this indent."));
        try {
            Date date = service.approveLevelOne(id, username, role, department);
            return ResponseEntity.ok(Map.of("approvedBy", username, "approvedDate", date.toString()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/AIndentListServlet/approve-final")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveFinal(
            @RequestParam int id,
            @RequestParam String indentnext,
            @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
            @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId) {
        if (!pageAccessService.hasButtonAccess(userId, role, "/AIndentListServlet", "CONFIRM_L2")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission for L2 approval."));
        }
        if (!RoleAccess.isGlobal(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only Global users can complete final approval."));
        try {
            return ResponseEntity.ok(service.approveFinal(id, indentnext, role, username));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private void requireApprover(String role) {
        if (!isApprover(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
    }

    private boolean isApprover(String role) {
        return RoleAccess.isGlobal(role) || "Admin".equalsIgnoreCase(role) || "Incharge".equalsIgnoreCase(role);
    }
}

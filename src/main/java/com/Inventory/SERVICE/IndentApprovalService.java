package com.Inventory.SERVICE;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inventory.Bean.IndentApprovalGroup;
import com.Inventory.Bean.IndentListItem;
import com.Inventory.DAO.IndentApprovalDAO;
import com.Inventory.SESSION.RoleAccess;

@Service
public class IndentApprovalService {
    private final IndentApprovalDAO dao;

    public IndentApprovalService(IndentApprovalDAO dao) { this.dao = dao; }

    public List<IndentApprovalGroup> getApprovalGroups(String role, String department) {
        Map<String, IndentApprovalGroup> grouped = new LinkedHashMap<>();
        for (IndentListItem item : dao.findActive(role, department)) {
            IndentApprovalGroup group = grouped.computeIfAbsent(item.getIndentNo(), key -> {
                IndentApprovalGroup created = new IndentApprovalGroup();
                created.setIndentNo(key);
                created.setDate(item.getDate());
                created.setType(item.getType());
                created.setDepartment(item.getDepartment());
                created.setRequestedBy(item.getRequestedBy());
                created.setStatus(item.getLevelTwoStatus());
                return created;
            });
            group.getItems().add(item);
        }
        return List.copyOf(grouped.values());
    }

    public Map<Integer, Double> getPendingByItem() { return dao.pendingByItem(); }

    @Transactional
    public Date approveLevelOne(int id, String username, String role, String department) {
        IndentListItem item = dao.findById(id);
        if (item == null || !withinApprovalScope(item, role, department) || !isActive(item)
                || "Approved".equalsIgnoreCase(item.getLevelOneStatus())) {
            throw new IllegalStateException("This indent is missing or already approved at L1.");
        }
        Date today = Date.valueOf(LocalDate.now());
        if (dao.approveLevelOne(id, username, today) == 0) {
            throw new IllegalStateException("This indent was already updated. Refresh and try again.");
        }
        return today;
    }

    @Transactional
    public Map<String, Object> approveFinal(int id, String nextStep, String role, String username) {
        IndentListItem item = dao.findById(id);
        if (item == null) throw new IllegalStateException("Indent not found.");
        if (!RoleAccess.isGlobal(role) || !withinApprovalScope(item, role, null) || !isActive(item)) {
            throw new IllegalStateException("This indent is outside your approval scope.");
        }
        if (!"Approved".equalsIgnoreCase(item.getLevelOneStatus())) {
            throw new IllegalStateException("L1 approval is required before final approval.");
        }
        if ("Approved".equalsIgnoreCase(item.getLevelTwoStatus())) {
            throw new IllegalStateException("This indent is already finally approved.");
        }
        if (nextStep == null) throw new IllegalArgumentException("Select the next step.");
        String target = nextStep.trim();
        boolean issue = "Issue".equalsIgnoreCase(target);
        boolean purchaseOrder = "PO".equalsIgnoreCase(target);
        boolean allowedOther = "Cancelled".equalsIgnoreCase(target) || "Management Note".equalsIgnoreCase(target);
        if (!(issue && "Issue".equalsIgnoreCase(item.getType()))
                && !(purchaseOrder && "Purchase".equalsIgnoreCase(item.getType())) && !allowedOther) {
            throw new IllegalArgumentException("Select a valid next step for this indent type.");
        }
        if (issue) {
            double pending = dao.sumApprovedIssueQty(item.getItemId(), id);
            double requested = item.getQty();
            if (pending + requested > item.getBalanceQty()) {
                throw new IllegalStateException("Stock insufficient. Available: " + item.getBalanceQty()
                        + ", already approved: " + pending + ", requested: " + requested + ".");
            }
        }
        Date today = Date.valueOf(LocalDate.now());
        if (dao.setFinalDecision(id, target, today, username) == 0) {
            throw new IllegalStateException("The indent could not be updated. Refresh and try again.");
        }
        return Map.of("nextStep", target, "date", today.toString(), "approvedBy", username);
    }

    private boolean isActive(IndentListItem item) {
        String next = item.getIndentStatus();
        return (next == null || !(next.trim().equalsIgnoreCase("Issue") || next.trim().equalsIgnoreCase("PO")
                || next.trim().equalsIgnoreCase("Issued") || next.trim().equalsIgnoreCase("Cancelled")
                || next.trim().equalsIgnoreCase("PO Raised")))
                && (item.getLevelTwoStatus() == null || !item.getLevelTwoStatus().trim().equalsIgnoreCase("Cancelled"));
    }

    private boolean withinApprovalScope(IndentListItem item, String role, String department) {
        if (RoleAccess.isSuperAdmin(role)) return true;
        if ("Global".equalsIgnoreCase(role)) return !"Dining Hall".equalsIgnoreCase(item.getDepartment());
        if ("Admin".equalsIgnoreCase(role)) {
            return List.of("Electrical", "Housekeeping", "Plumbing", "Dining Hall", "RO Plant", "Store", "Dhobi")
                    .stream().anyMatch(allowed -> allowed.equalsIgnoreCase(item.getDepartment()));
        }
        return department != null && item.getDepartment() != null
                && department.trim().equalsIgnoreCase(item.getDepartment().trim());
    }
}

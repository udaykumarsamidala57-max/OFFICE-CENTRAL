package com.Inventory.modules.stock.SERVICE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inventory.modules.stock.DAO.IssueStockDAO;

@Service
public class IssueStockService {
    private final IssueStockDAO dao;
    public IssueStockService(IssueStockDAO dao) { this.dao = dao; }

    public List<Map<String, Object>> pendingIssues() { return dao.findPendingIssues(); }
    public List<String> departments() { return dao.findDepartments(); }
    public List<String> categories() { return dao.findCategories(); }
    public List<Map<String, Object>> currentStock(String category, String search) { return dao.findCurrentStock(category, search); }
    public List<Map<String, Object>> issueReport(String from, String to, String department, String search) {
        LocalDate[] range = dates(from, to); return dao.findIssueReport(range[0], range[1], department, search);
    }
    public List<Map<String, Object>> issueVoucher(String indentNo) { return dao.findIssueVoucher(indentNo); }
    public List<Map<String, Object>> stockSummary(String from, String to, String category, String subCategory) {
        LocalDate[] range = dates(from, to); return dao.findStockSummary(range[0], range[1], category, subCategory);
    }

    @Transactional
    public String issue(int indentId, String quantityValue, String unitPriceValue, String dateValue) {
        BigDecimal qty = decimal(quantityValue, "Issue quantity", true);
        BigDecimal price = decimal(unitPriceValue, "Unit price", false);
        LocalDate date = date(dateValue, "Issue date");
        if (qty.signum() <= 0) throw new IllegalArgumentException("Issue quantity must be greater than zero.");
        Map<String, Object> indent = dao.lockIssueIndent(indentId);
        if (indent == null) throw new IllegalArgumentException("Indent was not found.");
        if (!"Approved".equalsIgnoreCase(String.valueOf(indent.get("status")))
                || !"Issue".equalsIgnoreCase(String.valueOf(indent.get("indentNext")))
                || "Issued".equalsIgnoreCase(String.valueOf(indent.get("issuedStatus")))) {
            throw new IllegalArgumentException("This indent is not approved and pending issue.");
        }
        BigDecimal requested = (BigDecimal) indent.get("qty");
        if (qty.compareTo(requested) > 0) throw new IllegalArgumentException("Issue quantity cannot exceed the approved indent quantity of " + requested.toPlainString() + ".");
        BigDecimal available = dao.lockStockBalance((Integer) indent.get("itemId"));
        if (qty.compareTo(available) > 0) throw new IllegalArgumentException("Insufficient stock. Available: " + available.toPlainString() + ".");
        String issueNo = dao.nextIssueNumber();
        dao.insertIssue(issueNo, indent, qty, price, date);
        return issueNo;
    }

    private LocalDate[] dates(String from, String to) {
        LocalDate start = blank(from) ? null : date(from, "From date");
        LocalDate end = blank(to) ? null : date(to, "To date");
        if (start != null && end != null && start.isAfter(end)) throw new IllegalArgumentException("From date must be on or before To date.");
        return new LocalDate[]{start, end};
    }
    private LocalDate date(String raw, String label) {
        try { return LocalDate.parse(raw == null ? "" : raw.trim()); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Enter a valid " + label.toLowerCase() + "."); }
    }
    private BigDecimal decimal(String raw, String label, boolean positiveScale) {
        try {
            BigDecimal value = new BigDecimal(raw == null ? "" : raw.trim()).setScale(2, RoundingMode.UNNECESSARY);
            if (value.signum() < 0 || (positiveScale && value.signum() == 0)) throw new NumberFormatException();
            return value;
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a valid non-negative amount with at most two decimal places.");
        }
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}

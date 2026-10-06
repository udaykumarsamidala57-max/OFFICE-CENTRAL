package com.Inventory.SERVICE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inventory.Bean.GRNLine;
import com.Inventory.Bean.GoodsReceivedNote;
import com.Inventory.DAO.GRNDAO;

@Service
public class GRNService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final GRNDAO dao;

    public GRNService(GRNDAO dao) { this.dao = dao; }

    public GoodsReceivedNote prepareEntry(String poNumber) {
        if (blank(poNumber)) throw new IllegalArgumentException("Purchase order number is required.");
        GoodsReceivedNote note = dao.findPurchaseOrder(poNumber.trim());
        if (note == null) throw new IllegalArgumentException("Purchase order was not found.");
        requireApprovedOpen(note);
        List<GRNLine> outstanding = note.getItems().stream()
                .filter(item -> item.getRemainingQty().signum() > 0).toList();
        if (outstanding.isEmpty()) throw new IllegalArgumentException("This purchase order has no remaining quantities to receive.");
        note.setItems(new ArrayList<>(outstanding));
        return note;
    }

    @Transactional
    public String create(String poNumber, String grnDateValue, String invoiceNo, String invoiceDateValue,
                         String receivedBy, String masterRemarks, int[] poItemIds,
                         String[] receivedValues, String[] acceptedValues, String[] rejectedValues,
                         String[] itemRemarks) {
        LocalDate grnDate = date(grnDateValue, "GRN date");
        LocalDate invoiceDate = date(invoiceDateValue, "Invoice date");
        if (blank(invoiceNo)) throw new IllegalArgumentException("Invoice number is required.");
        if (blank(receivedBy)) throw new IllegalArgumentException("Received by is required.");
        if (poItemIds == null || poItemIds.length == 0 || receivedValues == null || acceptedValues == null
                || rejectedValues == null || receivedValues.length != poItemIds.length
                || acceptedValues.length != poItemIds.length || rejectedValues.length != poItemIds.length
                || (itemRemarks != null && itemRemarks.length != poItemIds.length)) {
            throw new IllegalArgumentException("GRN item details are incomplete. Reload the purchase order and try again.");
        }

        GoodsReceivedNote note = dao.lockPurchaseOrder(poNumber == null ? "" : poNumber.trim());
        if (note == null) throw new IllegalArgumentException("Purchase order was not found.");
        requireApprovedOpen(note);
        dao.lockPurchaseOrderItems(note.getPoId());

        Map<Integer, GRNLine> remainingById = new HashMap<>();
        for (GRNLine line : dao.findOutstandingItems(note.getPoId())) {
            if (line.getRemainingQty().signum() > 0) remainingById.put(line.getPoItemId(), line);
        }
        if (remainingById.isEmpty()) throw new IllegalArgumentException("This purchase order has already been fully received.");

        Set<Integer> submittedIds = new HashSet<>();
        List<GRNLine> receivedLines = new ArrayList<>();
        boolean anyReceived = false;
        for (int i = 0; i < poItemIds.length; i++) {
            int poItemId = poItemIds[i];
            if (!submittedIds.add(poItemId)) throw new IllegalArgumentException("Duplicate purchase order item in GRN details.");
            GRNLine original = remainingById.get(poItemId);
            if (original == null) throw new IllegalArgumentException("A GRN item is not part of this purchase order or has already been received.");

            BigDecimal received = quantity(receivedValues[i], "Received quantity");
            BigDecimal accepted = quantity(acceptedValues[i], "Accepted quantity");
            BigDecimal rejected = quantity(rejectedValues[i], "Rejected quantity");
            if (received.compareTo(accepted.add(rejected)) != 0) {
                throw new IllegalArgumentException("Received quantity must equal accepted quantity plus rejected quantity for " + original.getDescription() + ".");
            }
            if (accepted.compareTo(original.getRemainingQty()) > 0) {
                throw new IllegalArgumentException("Accepted quantity for " + original.getDescription()
                        + " exceeds the remaining PO balance of " + original.getRemainingQty().toPlainString() + ".");
            }
            if (received.signum() > 0) anyReceived = true;
            original.setQtyReceived(received);
            original.setQtyAccepted(accepted);
            original.setQtyRejected(rejected);
            original.setRemarks(itemRemarks == null ? null : trimToNull(itemRemarks[i]));
            receivedLines.add(original);
        }
        if (!anyReceived) throw new IllegalArgumentException("Enter a received quantity for at least one item.");

        note.setGrnNo("GRN" + System.currentTimeMillis());
        note.setGrnDate(grnDate);
        note.setInvoiceNo(invoiceNo.trim());
        note.setInvoiceDate(invoiceDate);
        note.setReceivedBy(receivedBy.trim());
        note.setRemarks(trimToNull(masterRemarks));
        note.setItems(receivedLines);

        int grnId = dao.insertMaster(note);
        dao.insertItems(grnId, receivedLines);
        for (GRNLine item : receivedLines) dao.receiveAcceptedStock(grnId, note.getGrnNo(), grnDate, item);
        dao.updatePurchaseOrderReceiptStatus(note.getPoId());
        return note.getGrnNo();
    }

    public List<GoodsReceivedNote> findReport(String search, String fromDate, String toDate) {
        LocalDate from = blank(fromDate) ? null : date(fromDate, "From date");
        LocalDate to = blank(toDate) ? null : date(toDate, "To date");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be on or before To date.");
        }
        return dao.findReport(search, from, to);
    }

    public GoodsReceivedNote findByNumber(String number) {
        if (blank(number)) throw new IllegalArgumentException("GRN number is required.");
        return dao.findByNumber(number.trim());
    }

    private void requireApprovedOpen(GoodsReceivedNote note) {
        String approval = note.getPoApproval();
        if (!"Approved".equalsIgnoreCase(approval)) {
            throw new IllegalArgumentException("Only approved purchase orders can receive a GRN.");
        }
        String status = note.getPoStatus();
        if (status != null && "Closed".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("This purchase order is already closed.");
        }
        if (status != null && !status.isBlank() && !"Open".equalsIgnoreCase(status)
                && !"Partially Received".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("This purchase order is not open for receiving.");
        }
    }

    private BigDecimal quantity(String raw, String label) {
        try {
            BigDecimal value = new BigDecimal(raw == null ? "" : raw.trim()).setScale(2, RoundingMode.UNNECESSARY);
            if (value.signum() < 0) throw new NumberFormatException();
            return value;
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a non-negative number with at most two decimal places.");
        }
    }

    private LocalDate date(String raw, String label) {
        try { return LocalDate.parse(raw == null ? "" : raw.trim()); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Enter a valid " + label.toLowerCase() + "."); }
    }

    private String trimToNull(String value) { return blank(value) ? null : value.trim(); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}

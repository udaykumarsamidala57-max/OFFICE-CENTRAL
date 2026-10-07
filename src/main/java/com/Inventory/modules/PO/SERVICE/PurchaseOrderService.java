package com.Inventory.modules.PO.SERVICE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inventory.Bean.PurchaseOrder;
import com.Inventory.Bean.PurchaseOrderLine;
import com.Inventory.Bean.VendorOption;
import com.Inventory.modules.PO.DAO.PurchaseOrderDAO;

@Service
public class PurchaseOrderService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private final PurchaseOrderDAO dao;

    public PurchaseOrderService(PurchaseOrderDAO dao) { this.dao = dao; }

    public List<PurchaseOrderLine> findEligibleIndents() { return dao.findEligibleIndents(); }
    public List<VendorOption> findVendors() { return dao.findVendors(); }
    public String nextNumber() { return dao.nextNumber(); }

    public List<PurchaseOrderLine> selectedEligibleIndents(String[] rawIds) {
        List<Integer> ids = parseIds(rawIds);
        if (ids.isEmpty()) throw new IllegalArgumentException("Select at least one approved indent item.");
        List<PurchaseOrderLine> items = dao.findEligibleIndents(ids);
        if (items.size() != ids.size()) throw new IllegalArgumentException("One or more selected indents are no longer available for a purchase order.");
        return items;
    }

    public List<PurchaseOrder> findApprovals(String search) {
        List<PurchaseOrder> orders = dao.findOrders(true, search, null, null, null);
        orders.sort(java.util.Comparator.comparing(po -> "Approved".equalsIgnoreCase(po.getApproval()) ? 1 : 0));
        return orders;
    }

    public List<PurchaseOrder> findReport(String search, String fromDate, String toDate, String approval) {
        if (!blank(fromDate)) LocalDate.parse(fromDate);
        if (!blank(toDate)) LocalDate.parse(toDate);
        if (!blank(fromDate) && !blank(toDate) && fromDate.compareTo(toDate) > 0) {
            throw new IllegalArgumentException("From date must be on or before the To date.");
        }
        return dao.findOrders(false, search, fromDate, toDate, approval);
    }

    public PurchaseOrder findByNumber(String number) { return dao.findByNumber(number); }

    @Transactional
    public String create(String[] indentIds, String[] quantities, String[] rates, String[] discounts, String[] gstRates,
                         String vendorName, String quotationNumber, String poDate, String billingAddress,
                         String serviceChargeValue, String serviceGstValue, String terms, String general) {
        List<Integer> ids = parseIds(indentIds);
        if (ids.isEmpty()) throw new IllegalArgumentException("Select at least one approved indent item.");
        if (quantities == null || rates == null || discounts == null || gstRates == null
                || quantities.length != ids.size() || rates.length != ids.size()
                || discounts.length != ids.size() || gstRates.length != ids.size()) {
            throw new IllegalArgumentException("The purchase order item details are incomplete. Reload the selected indents and try again.");
        }
        if (blank(vendorName)) throw new IllegalArgumentException("Select a vendor.");
        if (blank(poDate)) throw new IllegalArgumentException("Purchase order date is required.");
        try { LocalDate.parse(poDate); } catch (DateTimeParseException ex) { throw new IllegalArgumentException("Enter a valid purchase order date."); }
        VendorOption vendor = dao.findVendors().stream().filter(v -> vendorName.equals(v.getName())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select a vendor from the current company vendor list."));
        List<PurchaseOrderLine> eligible = dao.findEligibleIndents(ids);
        if (eligible.size() != ids.size()) throw new IllegalArgumentException("One or more selected indents are no longer available for a purchase order.");

        Map<Integer, PurchaseOrderLine> merged = new LinkedHashMap<>();
        Set<Integer> uniqueIds = new LinkedHashSet<>(ids);
        for (int i = 0; i < eligible.size(); i++) {
            PurchaseOrderLine source = eligible.get(i);
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setIndentId(source.getIndentId());
            line.setIndentNo(source.getIndentNo());
            line.setItemId(source.getItemId());
            line.setDescription(source.getDescription());
            line.setUom(source.getUom());
            BigDecimal quantity = decimal(quantities[i], "Quantity").setScale(2, RoundingMode.HALF_UP);
            if (quantity.signum() <= 0) {
                throw new IllegalArgumentException("Quantity for " + source.getDescription() + " must be greater than zero.");
            }
            line.setQuantity(quantity);
            line.setRate(decimal(rates[i], "Rate").setScale(2, RoundingMode.HALF_UP));
            line.setDiscountPercent(percent(discounts[i], "Discount"));
            line.setGstPercent(percent(gstRates[i], "GST"));
            PurchaseOrderLine old = merged.get(line.getItemId());
            if (old != null) old.setQuantity(old.getQuantity().add(line.getQuantity()));
            else merged.put(line.getItemId(), line);
        }

        BigDecimal discountTotal = ZERO;
        BigDecimal gstTotal = ZERO;
        BigDecimal itemNetTotal = ZERO;
        for (PurchaseOrderLine line : merged.values()) {
            BigDecimal amount = money(line.getQuantity().multiply(line.getRate()));
            BigDecimal discount = money(amount.multiply(line.getDiscountPercent()).divide(HUNDRED, 8, RoundingMode.HALF_UP));
            BigDecimal taxable = money(amount.subtract(discount));
            BigDecimal gst = money(taxable.multiply(line.getGstPercent()).divide(HUNDRED, 8, RoundingMode.HALF_UP));
            line.setAmount(amount);
            line.setDiscountValue(discount);
            line.setGstValue(gst);
            line.setNetAmount(money(taxable.add(gst)));
            discountTotal = discountTotal.add(discount);
            gstTotal = gstTotal.add(gst);
            itemNetTotal = itemNetTotal.add(line.getNetAmount());
        }

        BigDecimal serviceCharge = decimalOrZero(serviceChargeValue, "Service charge");
        BigDecimal serviceGstPercent = percent(blank(serviceGstValue) ? "0" : serviceGstValue, "Service GST");
        BigDecimal serviceGst = money(serviceCharge.multiply(serviceGstPercent).divide(HUNDRED, 8, RoundingMode.HALF_UP));
        BigDecimal serviceTotal = money(serviceCharge.add(serviceGst));
        gstTotal = money(gstTotal.add(serviceGst));
        BigDecimal total = itemNetTotal.add(serviceTotal).setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);

        PurchaseOrder po = new PurchaseOrder();
        po.setNumber(dao.nextNumber());
        po.setDate(poDate);
        po.setVendorName(vendor.getName());
        po.setVendorGstin(vendor.getGstin());
        po.setVendorAddress(vendor.getAddress());
        po.setQuotationNumber(trimToNull(quotationNumber));
        po.setBillingAddress(trimToNull(billingAddress));
        po.setTotalGst(gstTotal);
        po.setTotalDiscount(money(discountTotal));
        po.setTotalAmount(total);
        po.setServiceCharge(serviceTotal);
        po.setTermsConditions(trimToNull(terms));
        po.setGeneralConditions(trimToNull(general));

        int poId = dao.insertMaster(po);
        List<PurchaseOrderLine> lines = new ArrayList<>(merged.values());
        dao.insertItems(poId, po.getNumber(), lines);
        int changed = dao.markIndentsRaised(new ArrayList<>(uniqueIds));
        if (changed != uniqueIds.size()) throw new IllegalStateException("Some selected indents changed while the PO was being saved. No changes were committed; reload and retry.");
        return po.getNumber();
    }

    @Transactional
    public void approve(String number) {
        PurchaseOrder po = dao.findByNumber(number);
        if (po == null) throw new IllegalArgumentException("Purchase order was not found.");
        if ("Approved".equalsIgnoreCase(po.getApproval())) throw new IllegalArgumentException("This purchase order is already approved.");
        if (dao.approve(number) != 1) throw new IllegalStateException("The purchase order could not be approved. Refresh and try again.");
    }

    @Transactional
    public void deletePending(String number) {
        if (dao.deletePending(number) != 1) throw new IllegalArgumentException("Only a pending purchase order can be deleted.");
    }

    private List<Integer> parseIds(String[] rawIds) {
        if (rawIds == null) return List.of();
        Set<Integer> ids = new LinkedHashSet<>();
        try {
            for (String raw : rawIds) {
                if (blank(raw)) continue;
                int id = Integer.parseInt(raw.trim());
                if (id <= 0) throw new NumberFormatException();
                ids.add(id);
            }
        } catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid indent selection."); }
        return new ArrayList<>(ids);
    }

    private BigDecimal decimal(String value, String label) {
        try {
            BigDecimal result = new BigDecimal(value == null ? "" : value.trim());
            if (result.signum() < 0) throw new NumberFormatException();
            return result;
        } catch (NumberFormatException ex) { throw new IllegalArgumentException(label + " must be a valid non-negative number."); }
    }

    private BigDecimal decimalOrZero(String value, String label) { return blank(value) ? ZERO : decimal(value, label); }
    private BigDecimal percent(String value, String label) {
        BigDecimal result = decimal(value, label + " percentage");
        if (result.compareTo(HUNDRED) > 0) throw new IllegalArgumentException(label + " percentage cannot exceed 100.");
        return result;
    }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private String trimToNull(String value) { return blank(value) ? null : value.trim(); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}




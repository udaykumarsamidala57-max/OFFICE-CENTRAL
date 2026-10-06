package com.Inventory.Bean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder {
    private int id;
    private String number;
    private String date;
    private String vendorName;
    private String vendorGstin;
    private String vendorAddress;
    private String quotationNumber;
    private String billingAddress;
    private String termsConditions;
    private String generalConditions;
    private String status;
    private String approval;
    private BigDecimal totalGst = BigDecimal.ZERO;
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal serviceCharge = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private List<PurchaseOrderLine> items = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getDateIso() { return formatDate("yyyy-MM-dd"); }
    public String getFormattedDate() { return formatDate("dd-MMMM-yyyy"); }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public String getVendorGstin() { return vendorGstin; }
    public void setVendorGstin(String vendorGstin) { this.vendorGstin = vendorGstin; }
    public String getVendorAddress() { return vendorAddress; }
    public void setVendorAddress(String vendorAddress) { this.vendorAddress = vendorAddress; }
    public String getQuotationNumber() { return quotationNumber; }
    public void setQuotationNumber(String quotationNumber) { this.quotationNumber = quotationNumber; }
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
    public String getTermsConditions() { return termsConditions; }
    public void setTermsConditions(String termsConditions) { this.termsConditions = termsConditions; }
    public String getGeneralConditions() { return generalConditions; }
    public void setGeneralConditions(String generalConditions) { this.generalConditions = generalConditions; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApproval() { return approval; }
    public void setApproval(String approval) { this.approval = approval; }
    public BigDecimal getTotalGst() { return totalGst; }
    public void setTotalGst(BigDecimal totalGst) { this.totalGst = totalGst; }
    public BigDecimal getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; }
    public BigDecimal getServiceCharge() { return serviceCharge; }
    public void setServiceCharge(BigDecimal serviceCharge) { this.serviceCharge = serviceCharge; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public List<PurchaseOrderLine> getItems() { return items; }
    public void setItems(List<PurchaseOrderLine> items) { this.items = items; }

    private String formatDate(String pattern) {
        if (date == null || date.isBlank()) return "-";
        String[] patterns = {"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss.S", "yyyy-MM-dd HH:mm:ss", "dd-MM-yyyy"};
        for (String input : patterns) {
            try {
                java.text.SimpleDateFormat parser = new java.text.SimpleDateFormat(input);
                parser.setLenient(false);
                java.util.Date parsed = parser.parse(date.trim());
                return new java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH).format(parsed);
            } catch (java.text.ParseException ignored) { }
        }
        return date.trim();
    }
}

package com.Inventory.Bean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GoodsReceivedNote {
    private int id;
    private int poId;
    private String grnNo;
    private LocalDate grnDate;
    private String poNumber;
    private String vendorName;
    private String vendorGstin;
    private String vendorAddress;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private String receivedBy;
    private String remarks;
    private String status = "Completed";
    private String poApproval;
    private String poStatus;
    private List<GRNLine> items = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPoId() { return poId; }
    public void setPoId(int poId) { this.poId = poId; }
    public String getGrnNo() { return grnNo; }
    public void setGrnNo(String grnNo) { this.grnNo = grnNo; }
    public LocalDate getGrnDate() { return grnDate; }
    public void setGrnDate(LocalDate grnDate) { this.grnDate = grnDate; }
    public String getFormattedGrnDate() { return format(grnDate); }
    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public String getVendorGstin() { return vendorGstin; }
    public void setVendorGstin(String vendorGstin) { this.vendorGstin = vendorGstin; }
    public String getVendorAddress() { return vendorAddress; }
    public void setVendorAddress(String vendorAddress) { this.vendorAddress = vendorAddress; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public String getFormattedInvoiceDate() { return format(invoiceDate); }
    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPoApproval() { return poApproval; }
    public void setPoApproval(String poApproval) { this.poApproval = poApproval; }
    public String getPoStatus() { return poStatus; }
    public void setPoStatus(String poStatus) { this.poStatus = poStatus; }
    public List<GRNLine> getItems() { return items; }
    public void setItems(List<GRNLine> items) { this.items = items == null ? new ArrayList<>() : items; }

    private String format(LocalDate value) {
        return value == null ? "-" : value.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }
}

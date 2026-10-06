package com.Inventory.Bean;

import java.math.BigDecimal;

public class PurchaseOrderLine {
    private int indentId;
    private String indentNo;
    private int itemId;
    private String description;
    private String uom;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal rate = BigDecimal.ZERO;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal discountPercent = BigDecimal.ZERO;
    private BigDecimal discountValue = BigDecimal.ZERO;
    private BigDecimal gstPercent = BigDecimal.ZERO;
    private BigDecimal gstValue = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;
    private BigDecimal receivedQty = BigDecimal.ZERO;
    private BigDecimal balanceQty = BigDecimal.ZERO;

    public int getIndentId() { return indentId; }
    public void setIndentId(int indentId) { this.indentId = indentId; }
    public String getIndentNo() { return indentNo; }
    public void setIndentNo(String indentNo) { this.indentNo = indentNo; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getGstPercent() { return gstPercent; }
    public void setGstPercent(BigDecimal gstPercent) { this.gstPercent = gstPercent; }
    public BigDecimal getGstValue() { return gstValue; }
    public void setGstValue(BigDecimal gstValue) { this.gstValue = gstValue; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public BigDecimal getReceivedQty() { return receivedQty; }
    public void setReceivedQty(BigDecimal receivedQty) { this.receivedQty = receivedQty; }
    public BigDecimal getBalanceQty() { return balanceQty; }
    public void setBalanceQty(BigDecimal balanceQty) { this.balanceQty = balanceQty; }
    public BigDecimal getBalanceToReceive() { return quantity.subtract(receivedQty); }
}

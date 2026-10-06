package com.Inventory.Bean;

import java.math.BigDecimal;

public class GRNLine {
    private int grnItemId;
    private int poItemId;
    private int itemId;
    private String description;
    private String uom;
    private BigDecimal orderedQty = BigDecimal.ZERO;
    private BigDecimal alreadyAccepted = BigDecimal.ZERO;
    private BigDecimal qtyReceived = BigDecimal.ZERO;
    private BigDecimal qtyAccepted = BigDecimal.ZERO;
    private BigDecimal qtyRejected = BigDecimal.ZERO;
    private String remarks;

    public int getGrnItemId() { return grnItemId; }
    public void setGrnItemId(int grnItemId) { this.grnItemId = grnItemId; }
    public int getPoItemId() { return poItemId; }
    public void setPoItemId(int poItemId) { this.poItemId = poItemId; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }
    public BigDecimal getOrderedQty() { return orderedQty; }
    public void setOrderedQty(BigDecimal orderedQty) { this.orderedQty = value(orderedQty); }
    public BigDecimal getAlreadyAccepted() { return alreadyAccepted; }
    public void setAlreadyAccepted(BigDecimal alreadyAccepted) { this.alreadyAccepted = value(alreadyAccepted); }
    public BigDecimal getRemainingQty() { return orderedQty.subtract(alreadyAccepted).max(BigDecimal.ZERO); }
    public BigDecimal getQtyReceived() { return qtyReceived; }
    public void setQtyReceived(BigDecimal qtyReceived) { this.qtyReceived = value(qtyReceived); }
    public BigDecimal getQtyAccepted() { return qtyAccepted; }
    public void setQtyAccepted(BigDecimal qtyAccepted) { this.qtyAccepted = value(qtyAccepted); }
    public BigDecimal getQtyRejected() { return qtyRejected; }
    public void setQtyRejected(BigDecimal qtyRejected) { this.qtyRejected = value(qtyRejected); }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    private BigDecimal value(BigDecimal number) { return number == null ? BigDecimal.ZERO : number; }
}

package com.Inventory.Bean;

import java.sql.Date;

public class IndentListItem {
    private int id;
    private String indentNo;
    private Date date;
    private String type;
    private String itemName;
    private int itemId;
    private double balanceQty;
    private double qty;
    private String uom;
    private String department;
    private String requestedBy;
    private String purpose;
    private String levelOneStatus;
    private String levelOneApprovedBy;
    private Date levelOneApprovedDate;
    private String levelTwoStatus;
    private Date levelTwoApprovedDate;
    private String levelTwoApprovedBy;
    private String indentStatus;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getIndentNo() { return indentNo; }
    public void setIndentNo(String indentNo) { this.indentNo = indentNo; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public double getBalanceQty() { return balanceQty; }
    public void setBalanceQty(double balanceQty) { this.balanceQty = balanceQty; }
    public double getQty() { return qty; }
    public void setQty(double qty) { this.qty = qty; }
    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getLevelOneStatus() { return levelOneStatus; }
    public void setLevelOneStatus(String levelOneStatus) { this.levelOneStatus = levelOneStatus; }
    public String getLevelOneApprovedBy() { return levelOneApprovedBy; }
    public void setLevelOneApprovedBy(String levelOneApprovedBy) { this.levelOneApprovedBy = levelOneApprovedBy; }
    public Date getLevelOneApprovedDate() { return levelOneApprovedDate; }
    public void setLevelOneApprovedDate(Date levelOneApprovedDate) { this.levelOneApprovedDate = levelOneApprovedDate; }
    public String getLevelTwoStatus() { return levelTwoStatus; }
    public void setLevelTwoStatus(String levelTwoStatus) { this.levelTwoStatus = levelTwoStatus; }
    public Date getLevelTwoApprovedDate() { return levelTwoApprovedDate; }
    public void setLevelTwoApprovedDate(Date levelTwoApprovedDate) { this.levelTwoApprovedDate = levelTwoApprovedDate; }
    public String getLevelTwoApprovedBy() { return levelTwoApprovedBy; }
    public void setLevelTwoApprovedBy(String levelTwoApprovedBy) { this.levelTwoApprovedBy = levelTwoApprovedBy; }
    public String getIndentStatus() { return indentStatus; }
    public void setIndentStatus(String indentStatus) { this.indentStatus = indentStatus; }
}

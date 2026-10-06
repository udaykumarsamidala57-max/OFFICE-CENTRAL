package com.Inventory.Bean;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class IndentApprovalGroup {
    private String indentNo;
    private Date date;
    private String type;
    private String department;
    private String requestedBy;
    private String status;
    private final List<IndentListItem> items = new ArrayList<>();

    public String getIndentNo() { return indentNo; }
    public void setIndentNo(String indentNo) { this.indentNo = indentNo; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<IndentListItem> getItems() { return items; }
}

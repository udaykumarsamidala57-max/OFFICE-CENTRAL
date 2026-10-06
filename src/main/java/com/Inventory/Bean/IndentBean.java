package com.Inventory.Bean;

import java.util.List;

public class IndentBean {

    private int indentNo;
    private String date;
    private String department;
    private String requestedBy;
    private String indentType;

    private int itemId;
    private String itemName;
    private double quantity;
    private String purpose;
    private String uom;
    private double stock;

    private String category;
    private String subCategory;
    private String departmentName;

    private List<IndentBean> departments;
    private List<IndentBean> categories;
    private List<IndentBean> subCategories;
    private List<IndentBean> items;

    public IndentBean() {
    }

    public IndentBean(int itemId, String itemName,
                      double quantity, String purpose,
                      String uom) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.purpose = purpose;
        this.uom = uom;
    }

    public int getIndentNo() {
        return indentNo;
    }

    public void setIndentNo(int indentNo) {
        this.indentNo = indentNo;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getIndentType() {
        return indentType;
    }

    public void setIndentType(String indentType) {
        this.indentType = indentType;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public double getStock() {
        return stock;
    }

    public void setStock(double stock) {
        this.stock = stock;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public List<IndentBean> getDepartments() {
        return departments;
    }

    public void setDepartments(List<IndentBean> departments) {
        this.departments = departments;
    }

    public List<IndentBean> getCategories() {
        return categories;
    }

    public void setCategories(List<IndentBean> categories) {
        this.categories = categories;
    }

    public List<IndentBean> getSubCategories() {
        return subCategories;
    }

    public void setSubCategories(List<IndentBean> subCategories) {
        this.subCategories = subCategories;
    }

    public List<IndentBean> getItems() {
        return items;
    }

    public void setItems(List<IndentBean> items) {
        this.items = items;
    }
}
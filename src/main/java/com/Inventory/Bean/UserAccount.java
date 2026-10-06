package com.Inventory.Bean;

public class UserAccount {
    private int id;
    private String username;
    private String password;
    private String role;
    private String department;
    private String mail;
    private String branch;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
}

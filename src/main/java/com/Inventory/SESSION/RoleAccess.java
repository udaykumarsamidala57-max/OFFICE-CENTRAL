package com.Inventory.SESSION;

public final class RoleAccess {
    private RoleAccess() { }

    public static boolean isSuperAdmin(String role) {
        return role != null && "Super Admin".equalsIgnoreCase(role.trim());
    }

    public static boolean isGlobal(String role) {
        return "Global".equalsIgnoreCase(role) || isSuperAdmin(role);
    }

    public static boolean isPageAdministrator(String role) {
        return "Admin".equalsIgnoreCase(role) || "Global".equalsIgnoreCase(role) || isSuperAdmin(role);
    }
}

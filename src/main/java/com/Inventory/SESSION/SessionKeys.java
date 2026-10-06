package com.Inventory.SESSION;

public final class SessionKeys {
    public static final String AUTHENTICATED = "authenticated";
    public static final String USERNAME = "username";
    public static final String USER_ID = "userId";
    public static final String SUPER_ADMIN = "superAdmin";
    public static final String ROLE = "role";
    public static final String DEPARTMENT = "department";
    public static final String MAIL = "mail";
    public static final String USER_BRANCH = "userBranch";
    public static final String SELECTED_COMPANY = "selectedCompany";

    // Request-scoped values exposed by LoginSessionInterceptor to controllers.
    public static final String REQUEST_USERNAME = "currentUsername";
    public static final String REQUEST_USER_ID = "currentUserId";
    public static final String REQUEST_ROLE = "currentUserRole";
    public static final String REQUEST_DEPARTMENT = "currentUserDepartment";
    public static final String REQUEST_COMPANY = "currentCompany";

    private SessionKeys() { }
}

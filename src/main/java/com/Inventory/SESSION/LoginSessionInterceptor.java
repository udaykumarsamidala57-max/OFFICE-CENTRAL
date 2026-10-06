package com.Inventory.SESSION;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.Inventory.SERVICE.PageAccessService;

@Component
public class LoginSessionInterceptor implements HandlerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginSessionInterceptor.class);
    private final PageAccessService pageAccessService;

    public LoginSessionInterceptor(PageAccessService pageAccessService) {
        this.pageAccessService = pageAccessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute(SessionKeys.AUTHENTICATED))) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        if (session.getAttribute(SessionKeys.SELECTED_COMPANY) == null) {
            response.sendRedirect(request.getContextPath() + "/select-company");
            return false;
        }

        // Keep HttpSession handling here; controllers receive only request-scoped values.
        request.setAttribute(SessionKeys.REQUEST_USERNAME, session.getAttribute(SessionKeys.USERNAME));
        request.setAttribute(SessionKeys.REQUEST_USER_ID, session.getAttribute(SessionKeys.USER_ID));
        request.setAttribute(SessionKeys.REQUEST_ROLE, session.getAttribute(SessionKeys.ROLE));
        request.setAttribute(SessionKeys.REQUEST_DEPARTMENT, session.getAttribute(SessionKeys.DEPARTMENT));
        request.setAttribute(SessionKeys.REQUEST_COMPANY, session.getAttribute(SessionKeys.SELECTED_COMPANY));

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean superAdmin = Boolean.TRUE.equals(session.getAttribute(SessionKeys.SUPER_ADMIN))
                || RoleAccess.isSuperAdmin((String) session.getAttribute(SessionKeys.ROLE));
        if (!superAdmin && !"/".equals(path) && !"/Home".equals(path)) {
            Object userIdValue = session.getAttribute(SessionKeys.USER_ID);
            Integer userId = userIdValue instanceof Number ? ((Number) userIdValue).intValue() : null;
            String role = (String) session.getAttribute(SessionKeys.ROLE);
            try {
                boolean hasPageAccess = pageAccessService.hasPageAccess(userId, role, path);
                boolean approvalPrintRequest = "/purchase-orders/report".equals(path)
                        && request.getParameter("printNumber") != null
                        && !request.getParameter("printNumber").isBlank();
                boolean hasApprovalPrintAccess = approvalPrintRequest
                        && pageAccessService.hasButtonAccess(userId, role,
                                "/purchase-orders/approvals", "VIEW_PO");
                if (!hasPageAccess && !hasApprovalPrintAccess) {
                    LOGGER.warn("Blocked page {} for user {} with role [{}]", path,
                            session.getAttribute(SessionKeys.USERNAME), role);
                    response.sendError(HttpStatus.FORBIDDEN.value(), "This page is not assigned to your account or role.");
                    return false;
                }
            } catch (DataAccessException ex) {
                LOGGER.error("Unable to check page permission for {}", path, ex);
                response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Page permissions could not be loaded.");
                return false;
            }
        }
        return true;
    }
}


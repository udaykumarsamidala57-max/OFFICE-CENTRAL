package com.Inventory.SERVICE;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inventory.Bean.AccessUser;
import com.Inventory.Bean.PageAccessView;
import com.Inventory.Bean.PageCatalogEntry;
import com.Inventory.DAO.PageAccessDAO;
import com.Inventory.SESSION.RoleAccess;

@Service
public class PageAccessService {
    private final PageAccessDAO accessDAO;
    private final PageCatalogService catalogService;

    public PageAccessService(PageAccessDAO accessDAO, PageCatalogService catalogService) {
        this.accessDAO = accessDAO;
        this.catalogService = catalogService;
    }

    public List<AccessUser> findUsers() { return accessDAO.findUsers(); }
    public List<String> findRoles() { return accessDAO.findRoles(); }
    public List<PageCatalogEntry> findPages() { return catalogService.findAll(); }

    public boolean isSuperAdminSubject(String subjectType, Integer userId, String role) {
        if ("ROLE".equals(subjectType)) return RoleAccess.isSuperAdmin(role);
        return "USER".equals(subjectType) && userId != null
                && RoleAccess.isSuperAdmin(accessDAO.findUserRole(userId));
    }

    public List<PageAccessView> load(String subjectType, Integer userId, String role) {
        return load(subjectType, userId, role, catalogService.findAll(), isSuperAdminSubject(subjectType, userId, role));
    }

    public List<PageAccessView> load(String subjectType, Integer userId, String role,
                                     List<PageCatalogEntry> pages, boolean superAdmin) {
        List<PageAccessView> views = new java.util.ArrayList<>();
        for (PageCatalogEntry page : pages) {
            PageAccessView view = new PageAccessView();
            view.setPage(page);
            if (superAdmin) {
                view.setPageGranted(true);
                Set<Integer> everyButton = new java.util.HashSet<>();
                page.getButtons().forEach(button -> everyButton.add(button.getId()));
                view.setGrantedButtonIds(everyButton);
            } else if ("USER".equals(subjectType) && userId != null) {
                view.setPageGranted(accessDAO.userHasPage(userId, page.getId()));
                view.setGrantedButtonIds(accessDAO.userButtons(userId, page.getId()));
            } else if ("ROLE".equals(subjectType) && role != null && !role.isBlank()) {
                view.setPageGranted(accessDAO.roleHasPage(role, page.getId()));
                view.setGrantedButtonIds(accessDAO.roleButtons(role, page.getId()));
            }
            views.add(view);
        }
        return views;
    }

    public boolean hasPageAccess(Integer userId, String role, String requestPath) {
        if (RoleAccess.isSuperAdmin(role)) return true;
        if (userId == null || requestPath == null) return false;
        Integer pageId = accessDAO.findPageIdForPath(normalizePath(requestPath));
        return pageId != null && (accessDAO.userHasPage(userId, pageId) || accessDAO.roleHasPage(role, pageId));
    }

    public boolean hasButtonAccess(Integer userId, String role, String requestPath, String buttonCode) {
        if (RoleAccess.isSuperAdmin(role)) return true;
        if (userId == null || buttonCode == null) return false;
        Integer pageId = accessDAO.findPageIdForPath(normalizePath(requestPath));
        return pageId != null && (accessDAO.userHasPage(userId, pageId) || accessDAO.roleHasPage(role, pageId))
                && accessDAO.hasPageButton(userId, role, pageId, buttonCode);
    }

    public List<PageCatalogEntry> findAccessiblePages(Integer userId, String role) {
        List<PageCatalogEntry> visible = new java.util.ArrayList<>();
        for (PageCatalogEntry page : catalogService.findAll()) {
            boolean pageAllowed = RoleAccess.isSuperAdmin(role) || (userId != null
                    && (accessDAO.userHasPage(userId, page.getId()) || accessDAO.roleHasPage(role, page.getId())));
            if (!pageAllowed) continue;
            if (!RoleAccess.isSuperAdmin(role)) {
                Set<Integer> buttonIds = new java.util.HashSet<>();
                if (userId != null) buttonIds.addAll(accessDAO.userButtons(userId, page.getId()));
                if (role != null) buttonIds.addAll(accessDAO.roleButtons(role, page.getId()));
                page.getButtons().removeIf(button -> !buttonIds.contains(button.getId()));
            }
            visible.add(page);
        }
        return visible;
    }

    public Set<String> findAllowedButtonCodes(Integer userId, String role, String requestPath) {
        if (requestPath == null) return Set.of();
        List<PageCatalogEntry> pages = findAccessiblePages(userId, role);
        String path = normalizePath(requestPath);
        return pages.stream()
                .filter(page -> path.equals(page.getUrl()) || path.startsWith(page.getUrl() + "/"))
                .max(java.util.Comparator.comparingInt(page -> page.getUrl().length()))
                .map(page -> page.getButtons().stream().map(button -> button.getCode().toUpperCase())
                        .collect(java.util.stream.Collectors.toSet()))
                .orElse(Set.of());
    }

    private String normalizePath(String path) {
        String normalized = path.trim();
        if (normalized.length() > 1 && normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    @Transactional(transactionManager = "authTransactionManager")
    public void save(String subjectType, Integer userId, String role, int pageId,
                     boolean pageGranted, List<Integer> buttonIds) {
        if (isSuperAdminSubject(subjectType, userId, role)) {
            throw new IllegalArgumentException("Super Admin always has access to every registered page and button.");
        }
        if (catalogService.findPage(pageId) == null) throw new IllegalArgumentException("Page not found.");
        List<Integer> selected = buttonIds == null ? List.of() : buttonIds.stream().distinct().toList();
        Set<Integer> validButtons = accessDAO.buttonsForPage(pageId);
        if (!validButtons.containsAll(selected)) throw new IllegalArgumentException("A selected button does not belong to this page.");
        if (!pageGranted && !selected.isEmpty()) throw new IllegalArgumentException("Grant page access before granting its buttons.");

        if ("USER".equals(subjectType)) {
            if (userId == null || !accessDAO.userExists(userId)) throw new IllegalArgumentException("Select a valid user.");
            accessDAO.clearUserPageGrants(userId, pageId);
            if (pageGranted) {
                accessDAO.grantUserPage(userId, pageId);
                selected.forEach(buttonId -> accessDAO.grantUserButton(userId, buttonId));
            }
        } else if ("ROLE".equals(subjectType)) {
            String safeRole = role == null ? "" : role.trim();
            if (safeRole.isEmpty() || safeRole.length() > 20 || !accessDAO.roleExists(safeRole)) {
                throw new IllegalArgumentException("Select a valid role from the users table.");
            }
            accessDAO.clearRolePageGrants(safeRole, pageId);
            if (pageGranted) {
                accessDAO.grantRolePage(safeRole, pageId);
                selected.forEach(buttonId -> accessDAO.grantRoleButton(safeRole, buttonId));
            }
        } else {
            throw new IllegalArgumentException("Select a user or role.");
        }
    }
}

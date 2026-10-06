package com.Inventory.Bean;

import java.util.HashSet;
import java.util.Set;

public class PageAccessView {
    private PageCatalogEntry page;
    private boolean pageGranted;
    private Set<Integer> grantedButtonIds = new HashSet<>();
    public PageCatalogEntry getPage() { return page; }
    public void setPage(PageCatalogEntry page) { this.page = page; }
    public boolean isPageGranted() { return pageGranted; }
    public void setPageGranted(boolean pageGranted) { this.pageGranted = pageGranted; }
    public Set<Integer> getGrantedButtonIds() { return grantedButtonIds; }
    public void setGrantedButtonIds(Set<Integer> grantedButtonIds) { this.grantedButtonIds = grantedButtonIds; }
}

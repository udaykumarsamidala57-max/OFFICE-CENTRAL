package com.Inventory.Bean;

import java.util.ArrayList;
import java.util.List;

public class PageCatalogEntry {
    private int id;
    private String code;
    private String name;
    private String url;
    private List<PageButton> buttons = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<PageButton> getButtons() { return buttons; }
    public void setButtons(List<PageButton> buttons) { this.buttons = buttons; }
}

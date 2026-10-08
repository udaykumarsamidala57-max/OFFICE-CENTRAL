package com.Inventory.Bean;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HomeIndentStage {
    private final String title;
    private final String description;
    private final List<IndentListItem> items;
    private final int indentCount;

    public HomeIndentStage(String title, String description, List<IndentListItem> items) {
        this.title = title;
        this.description = description;
        this.items = List.copyOf(items);
        Set<String> numbers = new LinkedHashSet<>();
        for (IndentListItem item : items) {
            if (item.getIndentNo() != null) numbers.add(item.getIndentNo());
        }
        this.indentCount = numbers.size();
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<IndentListItem> getItems() { return items; }
    public List<IndentListItem> getVisibleItems() { return items.subList(0, Math.min(items.size(), 6)); }
    public int getIndentCount() { return indentCount; }
    public int getItemCount() { return items.size(); }
    public boolean isEmpty() { return items.isEmpty(); }
}
package com.virnor.expedition.loot;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LootSet {

    private final String id;
    private List<ItemStack> items;

    public LootSet(String id) {
        this.id = id;
        this.items = new ArrayList<>();
    }

    public LootSet(String id, List<ItemStack> items) {
        this.id = id;
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
    }

    public void addItem(ItemStack item) {
        this.items.add(item);
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public void clearItems() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}

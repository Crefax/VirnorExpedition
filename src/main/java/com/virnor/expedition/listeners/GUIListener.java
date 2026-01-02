package com.virnor.expedition.listeners;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.config.ConfigManager;
import com.virnor.expedition.gui.GUIManager;
import com.virnor.expedition.loot.LootManager;
import com.virnor.expedition.loot.LootSet;
import com.virnor.expedition.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIListener implements Listener {

    private final VirnorExpedition plugin;
    private final Map<Player, String> editingLoot;

    public GUIListener(VirnorExpedition plugin) {
        this.plugin = plugin;
        this.editingLoot = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        
        // Check if it's one of our GUIs
        if (title.equals(GUIManager.MAIN_MENU_TITLE)) {
            handleMainMenu(event, player);
        } else if (title.equals(GUIManager.MOB_SETTINGS_TITLE)) {
            handleMobSettings(event, player);
        } else if (title.equals(GUIManager.MOB_TYPE_TITLE)) {
            handleMobTypeSelection(event, player);
        } else if (title.equals(GUIManager.TIME_SETTINGS_TITLE)) {
            handleTimeSettings(event, player);
        } else if (title.equals(GUIManager.DISTANCE_SETTINGS_TITLE)) {
            handleDistanceSettings(event, player);
        } else if (title.equals(GUIManager.LOOT_MENU_TITLE)) {
            handleLootMenu(event, player);
        } else if (title.startsWith(GUIManager.LOOT_EDIT_TITLE)) {
            handleLootEditor(event, player, title);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        
        // Allow dragging in loot editor for top slots only
        if (title.startsWith(GUIManager.LOOT_EDIT_TITLE)) {
            for (int slot : event.getRawSlots()) {
                if (slot >= 45) {
                    event.setCancelled(true);
                    return;
                }
            }
            // Allow drag in top 45 slots
            return;
        }
        
        // Cancel dragging in other GUIs
        if (title.equals(GUIManager.MAIN_MENU_TITLE) ||
            title.equals(GUIManager.MOB_SETTINGS_TITLE) ||
            title.equals(GUIManager.MOB_TYPE_TITLE) ||
            title.equals(GUIManager.TIME_SETTINGS_TITLE) ||
            title.equals(GUIManager.DISTANCE_SETTINGS_TITLE) ||
            title.equals(GUIManager.LOOT_MENU_TITLE)) {
            event.setCancelled(true);
        }
    }

    private void handleMainMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        GUIManager gui = plugin.getGuiManager();
        
        switch (slot) {
            case 10 -> gui.openMobSettings(player);
            case 12 -> gui.openTimeSettings(player);
            case 14 -> gui.openDistanceSettings(player);
            case 16 -> gui.openLootMenu(player);
        }
    }

    private void handleMobSettings(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        ConfigManager config = plugin.getConfigManager();
        GUIManager gui = plugin.getGuiManager();
        
        switch (slot) {
            // Mob type
            case 10 -> gui.openMobTypeSelection(player);
            
            // Health decrease
            case 12 -> {
                double newHealth = Math.max(10, config.getMobHealth() - 10);
                config.setMobHealth(newHealth);
                gui.openMobSettings(player);
            }
            // Health increase
            case 14 -> {
                double newHealth = Math.min(500, config.getMobHealth() + 10);
                config.setMobHealth(newHealth);
                gui.openMobSettings(player);
            }
            
            // Damage decrease
            case 19 -> {
                double newDamage = Math.max(1, config.getMobDamage() - 1);
                config.setMobDamage(newDamage);
                gui.openMobSettings(player);
            }
            // Damage increase
            case 21 -> {
                double newDamage = Math.min(50, config.getMobDamage() + 1);
                config.setMobDamage(newDamage);
                gui.openMobSettings(player);
            }
            
            // Mob count decrease
            case 23 -> {
                int newCount = Math.max(1, config.getMobCount() - 1);
                config.setMobCount(newCount);
                gui.openMobSettings(player);
            }
            // Mob count increase
            case 25 -> {
                int newCount = Math.min(10, config.getMobCount() + 1);
                config.setMobCount(newCount);
                gui.openMobSettings(player);
            }
            
            // Back button
            case 31 -> gui.openMainMenu(player);
        }
    }

    private void handleMobTypeSelection(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        GUIManager gui = plugin.getGuiManager();
        ConfigManager config = plugin.getConfigManager();
        
        if (slot == 49) {
            gui.openMobSettings(player);
            return;
        }
        
        if (slot >= 0 && slot < 45) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = ColorUtils.stripColor(item.getItemMeta().getDisplayName());
                try {
                    EntityType type = EntityType.valueOf(displayName);
                    config.setMobType(type);
                    player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + 
                        config.getMsgMobTypeChanged().replace("%type%", type.name())));
                    gui.openMobSettings(player);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void handleTimeSettings(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        ConfigManager config = plugin.getConfigManager();
        GUIManager gui = plugin.getGuiManager();
        
        switch (slot) {
            case 10 -> {
                int newDuration = Math.max(30, config.getOwnershipDuration() - 30);
                config.setOwnershipDuration(newDuration);
                gui.openTimeSettings(player);
            }
            case 12 -> {
                int newDuration = Math.min(1800, config.getOwnershipDuration() + 30);
                config.setOwnershipDuration(newDuration);
                gui.openTimeSettings(player);
            }
            case 14 -> {
                int newDuration = Math.max(15, config.getCooldownDuration() - 15);
                config.setCooldownDuration(newDuration);
                gui.openTimeSettings(player);
            }
            case 16 -> {
                int newDuration = Math.min(600, config.getCooldownDuration() + 15);
                config.setCooldownDuration(newDuration);
                gui.openTimeSettings(player);
            }
            case 22 -> gui.openMainMenu(player);
        }
    }

    private void handleDistanceSettings(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        ConfigManager config = plugin.getConfigManager();
        GUIManager gui = plugin.getGuiManager();
        
        switch (slot) {
            case 10 -> {
                double newDistance = Math.max(3, config.getSpawnDistance() - 2);
                config.setSpawnDistance(newDistance);
                gui.openDistanceSettings(player);
            }
            case 12 -> {
                double newDistance = Math.min(50, config.getSpawnDistance() + 2);
                config.setSpawnDistance(newDistance);
                gui.openDistanceSettings(player);
            }
            case 14 -> {
                double newDistance = Math.max(10, config.getTeleportDistance() - 5);
                config.setTeleportDistance(newDistance);
                gui.openDistanceSettings(player);
            }
            case 16 -> {
                double newDistance = Math.min(100, config.getTeleportDistance() + 5);
                config.setTeleportDistance(newDistance);
                gui.openDistanceSettings(player);
            }
            case 22 -> gui.openMainMenu(player);
        }
    }

    private void handleLootMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        LootManager lootManager = plugin.getLootManager();
        GUIManager gui = plugin.getGuiManager();
        ConfigManager config = plugin.getConfigManager();
        
        if (slot == 45) {
            gui.openMainMenu(player);
            return;
        }
        
        if (slot == 49) {
            String newId = lootManager.generateLootId();
            LootSet newLoot = new LootSet(newId);
            lootManager.addLootSet(newLoot);
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + 
                config.getMsgLootSetCreated().replace("%loot%", newId)));
            gui.openLootMenu(player);
            return;
        }
        
        if (slot >= 0 && slot < 45) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.CHEST && item.hasItemMeta()) {
                String displayName = ColorUtils.stripColor(item.getItemMeta().getDisplayName());
                
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    lootManager.removeLootSet(displayName);
                    player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + 
                        config.getMsgLootSetDeleted().replace("%loot%", displayName)));
                    gui.openLootMenu(player);
                } else {
                    editingLoot.put(player, displayName);
                    gui.openLootEditor(player, displayName);
                }
            }
        }
    }

    private void handleLootEditor(InventoryClickEvent event, Player player, String title) {
        int slot = event.getRawSlot();
        int inventorySize = event.getView().getTopInventory().getSize();
        
        // Allow all item operations in slots 0-44 (top inventory item area)
        if (slot >= 0 && slot < 45) {
            // Allow normal click operations - don't cancel
            return;
        }
        
        // Allow clicking in player inventory (bottom) to pick up items
        if (slot >= inventorySize) {
            // Allow picking up items from player inventory
            return;
        }
        
        // Cancel clicks on control buttons (slots 45-53)
        event.setCancelled(true);
        
        GUIManager gui = plugin.getGuiManager();
        ConfigManager config = plugin.getConfigManager();
        String lootId = title.replace(GUIManager.LOOT_EDIT_TITLE, "");
        
        if (slot == 45) {
            // Back button - save and go back
            saveLootFromInventory(player, event.getView().getTopInventory(), lootId);
            editingLoot.remove(player);
            gui.openLootMenu(player);
        } else if (slot == 49) {
            // Save button
            saveLootFromInventory(player, event.getView().getTopInventory(), lootId);
            editingLoot.remove(player);
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + 
                config.getMsgLootSetSaved().replace("%loot%", lootId)));
            gui.openLootMenu(player);
        }
    }

    private void saveLootFromInventory(Player player, Inventory inventory, String lootId) {
        List<ItemStack> items = new ArrayList<>();
        
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                // Check if it's not a GUI filler item
                if (!isGuiItem(item)) {
                    items.add(item.clone());
                }
            }
        }
        
        plugin.getLootManager().updateLootSet(lootId, items);
    }
    
    private boolean isGuiItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (item.getType() == Material.GRAY_STAINED_GLASS_PANE) return true;
        return false;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        
        // Auto-save loot when closing editor
        if (title.startsWith(GUIManager.LOOT_EDIT_TITLE) && editingLoot.containsKey(player)) {
            String lootId = editingLoot.remove(player);
            saveLootFromInventory(player, event.getInventory(), lootId);
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMsgPrefix() + 
                plugin.getConfigManager().getMsgLootSetSaved().replace("%loot%", lootId)));
        }
    }
    
    public Map<Player, String> getEditingLoot() {
        return editingLoot;
    }
}

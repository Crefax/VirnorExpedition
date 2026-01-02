package com.virnor.expedition.gui;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.config.ConfigManager;
import com.virnor.expedition.loot.LootManager;
import com.virnor.expedition.loot.LootSet;
import com.virnor.expedition.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GUIManager {

    private final VirnorExpedition plugin;
    
    public static final String MAIN_MENU_TITLE = "§8Expedition Admin";
    public static final String MOB_SETTINGS_TITLE = "§8Mob Ayarları";
    public static final String MOB_TYPE_TITLE = "§8Mob Tipi Seç";
    public static final String TIME_SETTINGS_TITLE = "§8Süre Ayarları";
    public static final String DISTANCE_SETTINGS_TITLE = "§8Mesafe Ayarları";
    public static final String LOOT_MENU_TITLE = "§8Loot Yönetimi";
    public static final String LOOT_EDIT_TITLE = "§8Loot Düzenle: ";

    public GUIManager(VirnorExpedition plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_MENU_TITLE);
        
        // Mob settings
        inv.setItem(10, createItem(Material.ZOMBIE_HEAD, "&aMob Ayarları", 
            "&7Mob tipi, canı ve sayısını ayarla"));
        
        // Time settings
        inv.setItem(12, createItem(Material.CLOCK, "&eSüre Ayarları",
            "&7Sahiplik ve cooldown sürelerini ayarla"));
        
        // Distance settings
        inv.setItem(14, createItem(Material.COMPASS, "&bMesafe Ayarları",
            "&7Spawn ve teleport mesafelerini ayarla"));
        
        // Loot settings
        inv.setItem(16, createItem(Material.CHEST, "&6Loot Yönetimi",
            "&7Loot setlerini düzenle"));
        
        // Fill empty slots
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
        
        player.openInventory(inv);
    }

    public void openMobSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MOB_SETTINGS_TITLE);
        ConfigManager config = plugin.getConfigManager();
        
        // Mob type
        Material mobMaterial = getMobMaterial(config.getMobType());
        inv.setItem(10, createItem(mobMaterial, "&aMob Tipi: &f" + config.getMobType().name(),
            "&7Tıkla değiştirmek için"));
        
        // Mob health - decrease
        inv.setItem(12, createItem(Material.RED_DYE, "&cCan Azalt (-10)",
            "&7Mevcut: &f" + config.getMobHealth()));
        
        // Mob health display
        inv.setItem(13, createItem(Material.RED_TERRACOTTA, "&cMob Canı: &f" + config.getMobHealth(),
            "&7Sol: -10, Sağ: +10"));
        
        // Mob health - increase
        inv.setItem(14, createItem(Material.LIME_DYE, "&aCan Arttır (+10)",
            "&7Mevcut: &f" + config.getMobHealth()));
        
        // Mob count - decrease
        inv.setItem(15, createItem(Material.RED_DYE, "&cMob Sayısı Azalt (-1)",
            "&7Mevcut: &f" + config.getMobCount()));
        
        // Mob count display
        inv.setItem(16, createItem(Material.SPAWNER, "&eMob Sayısı: &f" + config.getMobCount(),
            "&7Sol: -1, Sağ: +1"));
        
        // Back button
        inv.setItem(22, createItem(Material.ARROW, "&cGeri Dön"));
        
        // Fill empty slots
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
        
        player.openInventory(inv);
    }

    public void openMobTypeSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MOB_TYPE_TITLE);
        
        // Add common mob types
        EntityType[] mobTypes = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
            EntityType.CREEPER, EntityType.ENDERMAN, EntityType.BLAZE,
            EntityType.WITCH, EntityType.VINDICATOR, EntityType.PILLAGER,
            EntityType.RAVAGER, EntityType.WARDEN, EntityType.PIGLIN_BRUTE,
            EntityType.WITHER_SKELETON, EntityType.HUSK, EntityType.DROWNED,
            EntityType.STRAY, EntityType.PHANTOM, EntityType.VEX,
            EntityType.EVOKER, EntityType.ILLUSIONER, EntityType.GIANT,
            EntityType.ELDER_GUARDIAN, EntityType.GUARDIAN, EntityType.SHULKER,
            EntityType.HOGLIN, EntityType.ZOGLIN, EntityType.PIGLIN
        };
        
        int slot = 0;
        for (EntityType type : mobTypes) {
            if (slot >= 45) break;
            Material material = getMobMaterial(type);
            inv.setItem(slot, createItem(material, "&a" + type.name(),
                "&7Tıkla seçmek için"));
            slot++;
        }
        
        // Back button
        inv.setItem(49, createItem(Material.ARROW, "&cGeri Dön"));
        
        // Fill empty slots
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 49; i++) {
            inv.setItem(i, filler);
        }
        for (int i = 50; i < 54; i++) {
            inv.setItem(i, filler);
        }
        
        player.openInventory(inv);
    }

    public void openTimeSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TIME_SETTINGS_TITLE);
        ConfigManager config = plugin.getConfigManager();
        
        // Ownership duration
        inv.setItem(10, createItem(Material.RED_DYE, "&cSahiplik Süresi -30s",
            "&7Mevcut: &f" + config.getOwnershipDuration() + "s"));
        inv.setItem(11, createItem(Material.GOLDEN_APPLE, "&eSahiplik Süresi: &f" + config.getOwnershipDuration() + "s",
            "&7(" + formatTime(config.getOwnershipDuration()) + ")"));
        inv.setItem(12, createItem(Material.LIME_DYE, "&aSahiplik Süresi +30s",
            "&7Mevcut: &f" + config.getOwnershipDuration() + "s"));
        
        // Cooldown duration
        inv.setItem(14, createItem(Material.RED_DYE, "&cCooldown Süresi -15s",
            "&7Mevcut: &f" + config.getCooldownDuration() + "s"));
        inv.setItem(15, createItem(Material.CLOCK, "&6Cooldown Süresi: &f" + config.getCooldownDuration() + "s",
            "&7(" + formatTime(config.getCooldownDuration()) + ")"));
        inv.setItem(16, createItem(Material.LIME_DYE, "&aCooldown Süresi +15s",
            "&7Mevcut: &f" + config.getCooldownDuration() + "s"));
        
        // Back button
        inv.setItem(22, createItem(Material.ARROW, "&cGeri Dön"));
        
        // Fill empty slots
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
        
        player.openInventory(inv);
    }

    public void openDistanceSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, DISTANCE_SETTINGS_TITLE);
        ConfigManager config = plugin.getConfigManager();
        
        // Spawn distance
        inv.setItem(10, createItem(Material.RED_DYE, "&cSpawn Mesafesi -2",
            "&7Mevcut: &f" + config.getSpawnDistance()));
        inv.setItem(11, createItem(Material.ENDER_PEARL, "&aSpawn Mesafesi: &f" + config.getSpawnDistance(),
            "&7Oyuncu bu mesafeye yaklaşınca moblar spawn olur"));
        inv.setItem(12, createItem(Material.LIME_DYE, "&aSpawn Mesafesi +2",
            "&7Mevcut: &f" + config.getSpawnDistance()));
        
        // Teleport distance
        inv.setItem(14, createItem(Material.RED_DYE, "&cTeleport Mesafesi -5",
            "&7Mevcut: &f" + config.getTeleportDistance()));
        inv.setItem(15, createItem(Material.CHORUS_FRUIT, "&dTeleport Mesafesi: &f" + config.getTeleportDistance(),
            "&7Moblar bu mesafeden uzaklaşırsa geri ışınlanır"));
        inv.setItem(16, createItem(Material.LIME_DYE, "&aTeleport Mesafesi +5",
            "&7Mevcut: &f" + config.getTeleportDistance()));
        
        // Back button
        inv.setItem(22, createItem(Material.ARROW, "&cGeri Dön"));
        
        // Fill empty slots
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
        
        player.openInventory(inv);
    }

    public void openLootMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LOOT_MENU_TITLE);
        LootManager lootManager = plugin.getLootManager();
        
        int slot = 0;
        for (String lootId : lootManager.getLootSets().keySet()) {
            if (slot >= 45) break;
            LootSet lootSet = lootManager.getLootSet(lootId);
            inv.setItem(slot, createItem(Material.CHEST, "&6" + lootId,
                "&7Item sayısı: &f" + lootSet.getItems().size(),
                "&7Tıkla düzenlemek için",
                "&cShift+Tıkla silmek için"));
            slot++;
        }
        
        // Add new loot button
        inv.setItem(49, createItem(Material.EMERALD, "&aYeni Loot Ekle",
            "&7Tıkla yeni loot seti oluştur"));
        
        // Back button
        inv.setItem(45, createItem(Material.ARROW, "&cGeri Dön"));
        
        // Fill empty slots in bottom row
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 46; i < 49; i++) {
            inv.setItem(i, filler);
        }
        for (int i = 50; i < 54; i++) {
            inv.setItem(i, filler);
        }
        
        player.openInventory(inv);
    }

    public void openLootEditor(Player player, String lootId) {
        Inventory inv = Bukkit.createInventory(null, 54, LOOT_EDIT_TITLE + lootId);
        LootManager lootManager = plugin.getLootManager();
        LootSet lootSet = lootManager.getLootSet(lootId);
        
        // Load existing items
        if (lootSet != null) {
            List<ItemStack> items = lootSet.getItems();
            for (int i = 0; i < Math.min(items.size(), 45); i++) {
                inv.setItem(i, items.get(i).clone());
            }
        }
        
        // Bottom row controls (slots 45-53)
        // Back button
        inv.setItem(45, createItem(Material.ARROW, "&cGeri Dön & Kaydet",
            "&7Değişiklikleri kaydeder"));
        
        // Fill slots 46-48
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 46; i < 49; i++) {
            inv.setItem(i, filler);
        }
        
        // Save button
        inv.setItem(49, createItem(Material.LIME_WOOL, "&aKaydet",
            "&7Değişiklikleri kaydet ve çık"));
        
        // Fill slots 50-52
        for (int i = 50; i < 53; i++) {
            inv.setItem(i, filler);
        }
        
        // Info
        inv.setItem(53, createItem(Material.BOOK, "&eNasıl Kullanılır?",
            "&7Üst 45 slota envanterinden",
            "&7item sürükle bırak",
            "&7veya shift+tıkla",
            "&7Kaydet'e tıkla",
            "&7Bu loot seti oyunculara",
            "&7rastgele verilecek"));
        
        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtils.colorize(name));
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ColorUtils.colorize(line));
            }
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Material getMobMaterial(EntityType type) {
        return switch (type) {
            case ZOMBIE -> Material.ZOMBIE_HEAD;
            case SKELETON -> Material.SKELETON_SKULL;
            case CREEPER -> Material.CREEPER_HEAD;
            case SPIDER -> Material.SPIDER_EYE;
            case ENDERMAN -> Material.ENDER_PEARL;
            case BLAZE -> Material.BLAZE_ROD;
            case WITCH -> Material.POTION;
            case VINDICATOR, PILLAGER -> Material.IRON_AXE;
            case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
            case WARDEN -> Material.SCULK;
            case GUARDIAN, ELDER_GUARDIAN -> Material.PRISMARINE_SHARD;
            case PIGLIN, PIGLIN_BRUTE -> Material.GOLD_INGOT;
            default -> Material.SPAWNER;
        };
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
}

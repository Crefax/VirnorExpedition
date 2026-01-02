package com.virnor.expedition.config;

import com.virnor.expedition.VirnorExpedition;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

public class ConfigManager {

    private final VirnorExpedition plugin;
    
    // Mob settings
    private EntityType mobType;
    private double mobHealth;
    private int mobCount;
    private String mobName;
    
    // Distance settings
    private double spawnDistance;
    private double teleportDistance;
    private double mobSpawnRadius;
    
    // Time settings
    private int ownershipDuration;
    private int cooldownDuration;
    
    // Hologram settings
    private boolean hologramEnabled;
    private double hologramHeightOffset;
    private double hologramLineSpacing;
    private String hologramTitle;
    private String hologramStatusReady;
    private String hologramStatusFighting;
    private String hologramStatusOwner;
    private String hologramStatusTimeLeft;
    private String hologramStatusCooldown;
    
    // Messages
    private String msgPrefix;
    private String msgMobsSpawned;
    private String msgChestConquered;
    private String msgLootReceived;
    private String msgNotOwner;
    private String msgCooldown;
    private String msgOwnershipExpired;
    private String msgNoPermission;
    private String msgChestCreated;
    private String msgChestRemoved;
    private String msgChestNotFound;
    private String msgConfigReloaded;
    private String msgApproachToActivate;
    private String msgKillMobsFirst;
    private String msgCannotBreakChest;
    private String msgLootSetCreated;
    private String msgLootSetDeleted;
    private String msgLootSetSaved;
    private String msgMobTypeChanged;

    public ConfigManager(VirnorExpedition plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        // Mob settings
        String mobTypeStr = config.getString("mob.type", "ZOMBIE");
        try {
            mobType = EntityType.valueOf(mobTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            mobType = EntityType.ZOMBIE;
            plugin.getLogger().warning("Invalid mob type in config, using ZOMBIE");
        }
        mobHealth = config.getDouble("mob.health", 50.0);
        mobCount = config.getInt("mob.count", 3);
        mobName = config.getString("mob.name", "&c&lExpedition Guardian");
        
        // Distance settings
        spawnDistance = config.getDouble("distances.spawn", 10.0);
        teleportDistance = config.getDouble("distances.teleport", 20.0);
        mobSpawnRadius = config.getDouble("distances.mobSpawnRadius", 3.0);
        
        // Time settings
        ownershipDuration = config.getInt("times.ownershipDuration", 300);
        cooldownDuration = config.getInt("times.cooldownDuration", 90);
        
        // Hologram settings
        hologramEnabled = config.getBoolean("hologram.enabled", true);
        hologramHeightOffset = config.getDouble("hologram.heightOffset", 2.0);
        hologramLineSpacing = config.getDouble("hologram.lineSpacing", 0.3);
        hologramTitle = config.getString("hologram.title", "&6&lExpedition Chest");
        hologramStatusReady = config.getString("hologram.statusReady", "&aHazır!");
        hologramStatusFighting = config.getString("hologram.statusFighting", "&c&lSavaş!");
        hologramStatusOwner = config.getString("hologram.statusOwner", "&aSahip: &f%player%");
        hologramStatusTimeLeft = config.getString("hologram.statusTimeLeft", "&eKalan: &f%time%");
        hologramStatusCooldown = config.getString("hologram.statusCooldown", "&cCooldown: &f%time%");
        
        // Messages
        msgPrefix = config.getString("messages.prefix", "&8[&6Expedition&8] &r");
        msgMobsSpawned = config.getString("messages.mobsSpawned", "&eMoblar doğdu! Sandığı koruyorlar!");
        msgChestConquered = config.getString("messages.chestConquered", "&aSandığı ele geçirdin! 5 dakikan var!");
        msgLootReceived = config.getString("messages.lootReceived", "&aLoot başarıyla alındı!");
        msgNotOwner = config.getString("messages.notOwner", "&cBu sandığın sahibi sen değilsin!");
        msgCooldown = config.getString("messages.cooldown", "&cSandık cooldown'da! Kalan süre: &e%time%");
        msgOwnershipExpired = config.getString("messages.ownershipExpired", "&cSandık sahipliğin sona erdi!");
        msgNoPermission = config.getString("messages.noPermission", "&cBu işlemi yapmak için yetkin yok!");
        msgChestCreated = config.getString("messages.chestCreated", "&aExpedition chest başarıyla oluşturuldu!");
        msgChestRemoved = config.getString("messages.chestRemoved", "&aExpedition chest başarıyla silindi!");
        msgChestNotFound = config.getString("messages.chestNotFound", "&cExpedition chest bulunamadı!");
        msgConfigReloaded = config.getString("messages.configReloaded", "&aConfig yeniden yüklendi!");
        msgApproachToActivate = config.getString("messages.approachToActivate", "&eSandığa yaklaşarak mobları tetikle!");
        msgKillMobsFirst = config.getString("messages.killMobsFirst", "&cÖnce tüm mobları öldürmelisin!");
        msgCannotBreakChest = config.getString("messages.cannotBreakChest", "&cExpedition chest'i kıramazsın! &e/expedition remove &ckullan.");
        msgLootSetCreated = config.getString("messages.lootSetCreated", "&aYeni loot seti oluşturuldu: &f%loot%");
        msgLootSetDeleted = config.getString("messages.lootSetDeleted", "&cLoot seti silindi: &f%loot%");
        msgLootSetSaved = config.getString("messages.lootSetSaved", "&aLoot seti kaydedildi: &f%loot%");
        msgMobTypeChanged = config.getString("messages.mobTypeChanged", "&aMob tipi değiştirildi: &f%type%");
    }

    public void saveConfig() {
        FileConfiguration config = plugin.getConfig();
        
        config.set("mob.type", mobType.name());
        config.set("mob.health", mobHealth);
        config.set("mob.count", mobCount);
        config.set("mob.name", mobName);
        
        config.set("distances.spawn", spawnDistance);
        config.set("distances.teleport", teleportDistance);
        config.set("distances.mobSpawnRadius", mobSpawnRadius);
        
        config.set("times.ownershipDuration", ownershipDuration);
        config.set("times.cooldownDuration", cooldownDuration);
        
        config.set("hologram.enabled", hologramEnabled);
        config.set("hologram.heightOffset", hologramHeightOffset);
        config.set("hologram.lineSpacing", hologramLineSpacing);
        config.set("hologram.title", hologramTitle);
        config.set("hologram.statusReady", hologramStatusReady);
        config.set("hologram.statusFighting", hologramStatusFighting);
        config.set("hologram.statusOwner", hologramStatusOwner);
        config.set("hologram.statusTimeLeft", hologramStatusTimeLeft);
        config.set("hologram.statusCooldown", hologramStatusCooldown);
        
        config.set("messages.prefix", msgPrefix);
        config.set("messages.mobsSpawned", msgMobsSpawned);
        config.set("messages.chestConquered", msgChestConquered);
        config.set("messages.lootReceived", msgLootReceived);
        config.set("messages.notOwner", msgNotOwner);
        config.set("messages.cooldown", msgCooldown);
        config.set("messages.ownershipExpired", msgOwnershipExpired);
        config.set("messages.noPermission", msgNoPermission);
        config.set("messages.chestCreated", msgChestCreated);
        config.set("messages.chestRemoved", msgChestRemoved);
        config.set("messages.chestNotFound", msgChestNotFound);
        config.set("messages.configReloaded", msgConfigReloaded);
        config.set("messages.approachToActivate", msgApproachToActivate);
        config.set("messages.killMobsFirst", msgKillMobsFirst);
        config.set("messages.cannotBreakChest", msgCannotBreakChest);
        config.set("messages.lootSetCreated", msgLootSetCreated);
        config.set("messages.lootSetDeleted", msgLootSetDeleted);
        config.set("messages.lootSetSaved", msgLootSetSaved);
        config.set("messages.mobTypeChanged", msgMobTypeChanged);
        
        plugin.saveConfig();
    }

    // Getters and Setters
    public EntityType getMobType() { return mobType; }
    public void setMobType(EntityType mobType) { this.mobType = mobType; saveConfig(); }
    
    public double getMobHealth() { return mobHealth; }
    public void setMobHealth(double mobHealth) { this.mobHealth = mobHealth; saveConfig(); }
    
    public int getMobCount() { return mobCount; }
    public void setMobCount(int mobCount) { this.mobCount = mobCount; saveConfig(); }
    
    public String getMobName() { return mobName; }
    public void setMobName(String mobName) { this.mobName = mobName; saveConfig(); }
    
    public double getSpawnDistance() { return spawnDistance; }
    public void setSpawnDistance(double spawnDistance) { this.spawnDistance = spawnDistance; saveConfig(); }
    
    public double getTeleportDistance() { return teleportDistance; }
    public void setTeleportDistance(double teleportDistance) { this.teleportDistance = teleportDistance; saveConfig(); }
    
    public double getMobSpawnRadius() { return mobSpawnRadius; }
    public void setMobSpawnRadius(double mobSpawnRadius) { this.mobSpawnRadius = mobSpawnRadius; saveConfig(); }
    
    public int getOwnershipDuration() { return ownershipDuration; }
    public void setOwnershipDuration(int ownershipDuration) { this.ownershipDuration = ownershipDuration; saveConfig(); }
    
    public int getCooldownDuration() { return cooldownDuration; }
    public void setCooldownDuration(int cooldownDuration) { this.cooldownDuration = cooldownDuration; saveConfig(); }
    
    // Hologram getters
    public boolean isHologramEnabled() { return hologramEnabled; }
    public double getHologramHeightOffset() { return hologramHeightOffset; }
    public double getHologramLineSpacing() { return hologramLineSpacing; }
    public String getHologramTitle() { return hologramTitle; }
    public String getHologramStatusReady() { return hologramStatusReady; }
    public String getHologramStatusFighting() { return hologramStatusFighting; }
    public String getHologramStatusOwner() { return hologramStatusOwner; }
    public String getHologramStatusTimeLeft() { return hologramStatusTimeLeft; }
    public String getHologramStatusCooldown() { return hologramStatusCooldown; }
    
    // Message getters
    public String getMsgPrefix() { return msgPrefix; }
    public String getMsgMobsSpawned() { return msgMobsSpawned; }
    public String getMsgChestConquered() { return msgChestConquered; }
    public String getMsgLootReceived() { return msgLootReceived; }
    public String getMsgNotOwner() { return msgNotOwner; }
    public String getMsgCooldown() { return msgCooldown; }
    public String getMsgOwnershipExpired() { return msgOwnershipExpired; }
    public String getMsgNoPermission() { return msgNoPermission; }
    public String getMsgChestCreated() { return msgChestCreated; }
    public String getMsgChestRemoved() { return msgChestRemoved; }
    public String getMsgChestNotFound() { return msgChestNotFound; }
    public String getMsgConfigReloaded() { return msgConfigReloaded; }
    public String getMsgApproachToActivate() { return msgApproachToActivate; }
    public String getMsgKillMobsFirst() { return msgKillMobsFirst; }
    public String getMsgCannotBreakChest() { return msgCannotBreakChest; }
    public String getMsgLootSetCreated() { return msgLootSetCreated; }
    public String getMsgLootSetDeleted() { return msgLootSetDeleted; }
    public String getMsgLootSetSaved() { return msgLootSetSaved; }
    public String getMsgMobTypeChanged() { return msgMobTypeChanged; }
}

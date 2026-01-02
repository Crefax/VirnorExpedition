package com.virnor.expedition.managers;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.config.ConfigManager;
import com.virnor.expedition.data.ExpeditionChest;
import com.virnor.expedition.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks damage dealt by players to expedition mobs and manages health bar holograms
 */
public class DamageTracker {

    private final VirnorExpedition plugin;
    
    // ChestID -> (PlayerUUID -> TotalDamage)
    private final Map<String, Map<UUID, Double>> chestDamageMap;
    
    // MobUUID -> HealthBar ArmorStand UUID
    private final Map<UUID, UUID> mobHealthBarMap;

    public DamageTracker(VirnorExpedition plugin) {
        this.plugin = plugin;
        this.chestDamageMap = new ConcurrentHashMap<>();
        this.mobHealthBarMap = new ConcurrentHashMap<>();
    }

    /**
     * Record damage dealt by a player to an expedition mob
     */
    public void recordDamage(String chestId, UUID playerUUID, double damage) {
        chestDamageMap.computeIfAbsent(chestId, k -> new ConcurrentHashMap<>())
                      .merge(playerUUID, damage, Double::sum);
    }

    /**
     * Get total damage dealt by a player to mobs of a chest
     */
    public double getPlayerDamage(String chestId, UUID playerUUID) {
        Map<UUID, Double> damages = chestDamageMap.get(chestId);
        if (damages == null) return 0.0;
        return damages.getOrDefault(playerUUID, 0.0);
    }

    /**
     * Get the player who dealt the most damage to a chest's mobs
     */
    public UUID getTopDamagePlayer(String chestId) {
        Map<UUID, Double> damages = chestDamageMap.get(chestId);
        if (damages == null || damages.isEmpty()) return null;
        
        return damages.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Get total damage dealt by top player
     */
    public double getTopDamageAmount(String chestId) {
        Map<UUID, Double> damages = chestDamageMap.get(chestId);
        if (damages == null || damages.isEmpty()) return 0.0;
        
        return damages.values().stream()
                .max(Double::compare)
                .orElse(0.0);
    }

    /**
     * Get all damage stats for a chest
     */
    public Map<UUID, Double> getAllDamages(String chestId) {
        return chestDamageMap.getOrDefault(chestId, new HashMap<>());
    }

    /**
     * Get sorted damage leaderboard
     */
    public List<Map.Entry<UUID, Double>> getDamageLeaderboard(String chestId) {
        Map<UUID, Double> damages = chestDamageMap.get(chestId);
        if (damages == null || damages.isEmpty()) return new ArrayList<>();
        
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(damages.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return sorted;
    }

    /**
     * Clear damage data for a chest
     */
    public void clearChestDamage(String chestId) {
        chestDamageMap.remove(chestId);
    }

    /**
     * Determine the loot winner based on config settings
     * Returns the last hitter if mode is LAST_HIT, or top damage dealer if MOST_DAMAGE
     */
    public Player determineLootWinner(String chestId, Player lastHitter) {
        ConfigManager config = plugin.getConfigManager();
        
        if (config.isLastHitMode() || lastHitter == null) {
            return lastHitter;
        }
        
        // MOST_DAMAGE mode
        UUID topDamagePlayerUUID = getTopDamagePlayer(chestId);
        if (topDamagePlayerUUID == null) {
            return lastHitter;
        }
        
        Player topDamagePlayer = Bukkit.getPlayer(topDamagePlayerUUID);
        return topDamagePlayer != null ? topDamagePlayer : lastHitter;
    }

    /**
     * Announce damage results to all players who participated
     */
    public void announceDamageResults(String chestId, Player winner) {
        ConfigManager config = plugin.getConfigManager();
        Map<UUID, Double> damages = chestDamageMap.get(chestId);
        
        if (damages == null || damages.isEmpty()) return;
        
        double topDamage = getTopDamageAmount(chestId);
        String winnerName = winner != null ? winner.getName() : "Bilinmiyor";
        
        for (Map.Entry<UUID, Double> entry : damages.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            
            double playerDamage = entry.getValue();
            
            if (winner != null && player.getUniqueId().equals(winner.getUniqueId())) {
                // Winner message
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgYouWonLoot()));
            } else {
                // Loser message
                String msg = config.getMsgLostLoot()
                        .replace("%winner%", winnerName)
                        .replace("%your_damage%", String.format("%.1f", playerDamage));
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + msg));
            }
            
            // Show top damage
            String topMsg = config.getMsgTopDamage()
                    .replace("%player%", winnerName)
                    .replace("%damage%", String.format("%.1f", topDamage));
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + topMsg));
        }
    }

    // ==================== HEALTH BAR HOLOGRAM METHODS ====================

    /**
     * Create health bar hologram for a mob
     */
    public void createHealthBar(LivingEntity mob) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isShowHealthBar()) return;
        
        Location loc = getHealthBarLocation(mob);
        
        ArmorStand healthBar = (ArmorStand) mob.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        healthBar.setVisible(false);
        healthBar.setCustomNameVisible(true);
        healthBar.setGravity(false);
        healthBar.setSmall(true);
        healthBar.setMarker(true);
        healthBar.setInvulnerable(true);
        
        // Set initial health bar
        updateHealthBarDisplay(healthBar, mob);
        
        mobHealthBarMap.put(mob.getUniqueId(), healthBar.getUniqueId());
    }

    /**
     * Update health bar display for a mob
     */
    public void updateHealthBar(LivingEntity mob) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isShowHealthBar()) return;
        
        UUID healthBarUUID = mobHealthBarMap.get(mob.getUniqueId());
        if (healthBarUUID == null) {
            createHealthBar(mob);
            return;
        }
        
        Entity entity = Bukkit.getEntity(healthBarUUID);
        if (entity == null || entity.isDead()) {
            mobHealthBarMap.remove(mob.getUniqueId());
            createHealthBar(mob);
            return;
        }
        
        ArmorStand healthBar = (ArmorStand) entity;
        
        // Update position
        healthBar.teleport(getHealthBarLocation(mob));
        
        // Update display
        updateHealthBarDisplay(healthBar, mob);
    }

    /**
     * Update the visual display of a health bar
     */
    private void updateHealthBarDisplay(ArmorStand healthBar, LivingEntity mob) {
        ConfigManager config = plugin.getConfigManager();
        
        double maxHealth = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHealth = mob.getHealth();
        double healthPercent = currentHealth / maxHealth;
        
        int totalBars = config.getHealthBarTotalBars();
        int filledBars = (int) Math.ceil(healthPercent * totalBars);
        int emptyBars = totalBars - filledBars;
        
        String symbol = config.getHealthBarSymbol();
        StringBuilder healthBarText = new StringBuilder();
        
        // Determine color based on health percentage
        String color;
        if (healthPercent > 0.5) {
            color = config.getHealthBarHighColor();
        } else if (healthPercent > 0.25) {
            color = config.getHealthBarMediumColor();
        } else {
            color = config.getHealthBarLowColor();
        }
        
        // Build health bar string
        healthBarText.append(color);
        for (int i = 0; i < filledBars; i++) {
            healthBarText.append(symbol);
        }
        
        healthBarText.append(config.getHealthBarBackgroundColor());
        for (int i = 0; i < emptyBars; i++) {
            healthBarText.append(symbol);
        }
        
        // Add health numbers
        healthBarText.append(" &f").append((int) currentHealth).append("&7/&f").append((int) maxHealth);
        
        healthBar.setCustomName(ColorUtils.colorize(healthBarText.toString()));
    }

    /**
     * Get location for health bar above mob's head
     */
    private Location getHealthBarLocation(LivingEntity mob) {
        ConfigManager config = plugin.getConfigManager();
        Location loc = mob.getLocation().clone();
        loc.add(0, mob.getHeight() + config.getHealthBarHeightOffset(), 0);
        return loc;
    }

    /**
     * Remove health bar for a mob
     */
    public void removeHealthBar(UUID mobUUID) {
        UUID healthBarUUID = mobHealthBarMap.remove(mobUUID);
        if (healthBarUUID != null) {
            Entity entity = Bukkit.getEntity(healthBarUUID);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    /**
     * Remove all health bars for a chest's mobs
     */
    public void removeAllHealthBars(ExpeditionChest chest) {
        for (UUID mobUUID : chest.getMobUUIDs()) {
            removeHealthBar(mobUUID);
        }
    }

    /**
     * Update all health bars (called periodically)
     */
    public void updateAllHealthBars() {
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(mobHealthBarMap).entrySet()) {
            UUID mobUUID = entry.getKey();
            Entity mobEntity = Bukkit.getEntity(mobUUID);
            
            if (mobEntity == null || mobEntity.isDead() || !(mobEntity instanceof LivingEntity)) {
                removeHealthBar(mobUUID);
                continue;
            }
            
            updateHealthBar((LivingEntity) mobEntity);
        }
    }

    /**
     * Clean up all health bars
     */
    public void removeAllHealthBars() {
        for (UUID healthBarUUID : mobHealthBarMap.values()) {
            Entity entity = Bukkit.getEntity(healthBarUUID);
            if (entity != null) {
                entity.remove();
            }
        }
        mobHealthBarMap.clear();
    }
}

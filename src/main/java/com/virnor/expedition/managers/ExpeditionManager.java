package com.virnor.expedition.managers;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.config.ConfigManager;
import com.virnor.expedition.data.ExpeditionChest;
import com.virnor.expedition.data.ExpeditionState;
import com.virnor.expedition.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ExpeditionManager {

    private final VirnorExpedition plugin;
    private final Set<UUID> playersNearChests;
    private BukkitTask mainTask;
    private BukkitTask healthBarTask;

    public ExpeditionManager(VirnorExpedition plugin) {
        this.plugin = plugin;
        this.playersNearChests = new HashSet<>();
    }

    public void startTasks() {
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkPlayerDistances();
            checkOwnershipExpirations();
            checkCooldownExpirations();
            plugin.getMobManager().checkMobDistances();
            updateHolograms();
        }, 20L, 20L);
        
        // Health bar update task - runs every 5 ticks for smoother updates
        healthBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            plugin.getDamageTracker().updateAllHealthBars();
        }, 5L, 5L);
    }

    public void stopTasks() {
        if (mainTask != null) {
            mainTask.cancel();
        }
        if (healthBarTask != null) {
            healthBarTask.cancel();
        }
    }

    public boolean createExpeditionChest(Location location) {
        Block block = location.getBlock();
        block.setType(Material.CHEST);
        
        String id = plugin.getDataManager().generateChestId();
        ExpeditionChest chest = new ExpeditionChest(id, location);
        plugin.getDataManager().addExpeditionChest(chest);
        
        ConfigManager config = plugin.getConfigManager();
        plugin.getHologramManager().createHologram(chest, 
            config.getHologramTitle(), 
            config.getHologramStatusReady());
        
        return true;
    }

    public boolean removeExpeditionChest(String id) {
        ExpeditionChest chest = plugin.getDataManager().getExpeditionChest(id);
        if (chest == null) return false;
        
        plugin.getMobManager().removeMobsForChest(chest);
        plugin.getHologramManager().removeHologram(chest);
        chest.getLocation().getBlock().setType(Material.AIR);
        plugin.getDataManager().removeExpeditionChest(id);
        
        return true;
    }

    public boolean removeExpeditionChestByLocation(Location location) {
        ExpeditionChest chest = plugin.getDataManager().getChestByLocation(location);
        if (chest == null) return false;
        return removeExpeditionChest(chest.getId());
    }

    private void checkPlayerDistances() {
        ConfigManager config = plugin.getConfigManager();
        double spawnDistance = config.getSpawnDistance();
        
        for (ExpeditionChest chest : plugin.getDataManager().getExpeditionChests().values()) {
            if (chest.getState() != ExpeditionState.READY) continue;
            
            Location chestLoc = chest.getLocation();
            if (chestLoc.getWorld() == null) continue;
            
            for (Player player : chestLoc.getWorld().getPlayers()) {
                double distance = player.getLocation().distance(chestLoc);
                
                if (distance <= spawnDistance) {
                    activateChest(chest, player);
                    break;
                }
            }
        }
    }

    public void activateChest(ExpeditionChest chest, Player triggerPlayer) {
        if (chest.getState() != ExpeditionState.READY) return;
        
        ConfigManager config = plugin.getConfigManager();
        
        chest.setState(ExpeditionState.ACTIVE);
        plugin.getMobManager().spawnMobsForChest(chest);
        
        plugin.getHologramManager().updateHologram(chest, 
            config.getHologramTitle(), 
            config.getHologramStatusFighting());
        
        triggerPlayer.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgMobsSpawned()));
    }

    public void onAllMobsKilled(ExpeditionChest chest, Player killer) {
        ConfigManager config = plugin.getConfigManager();
        
        chest.setState(ExpeditionState.CONQUERED);
        chest.setOwnerUUID(killer.getUniqueId());
        chest.setOwnershipExpireTime(System.currentTimeMillis() + (config.getOwnershipDuration() * 1000L));
        
        plugin.getHologramManager().updateHologram(chest, 
            config.getHologramTitle(),
            config.getHologramStatusOwner().replace("%player%", killer.getName()),
            config.getHologramStatusTimeLeft().replace("%time%", ColorUtils.formatTime(chest.getRemainingOwnershipTime())));
        
        killer.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgChestConquered()));
    }

    public void onLootClaimed(ExpeditionChest chest, Player player) {
        ConfigManager config = plugin.getConfigManager();
        
        chest.setState(ExpeditionState.COOLDOWN);
        chest.setCooldownExpireTime(System.currentTimeMillis() + (config.getCooldownDuration() * 1000L));
        chest.setOwnerUUID(null);
        
        plugin.getHologramManager().updateHologram(chest, 
            config.getHologramTitle(),
            config.getHologramStatusCooldown().replace("%time%", ColorUtils.formatTime(chest.getRemainingCooldown())));
        
        player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgLootReceived()));
        
        plugin.getDataManager().saveData();
    }

    private void checkOwnershipExpirations() {
        ConfigManager config = plugin.getConfigManager();
        
        for (ExpeditionChest chest : plugin.getDataManager().getExpeditionChests().values()) {
            if (chest.getState() == ExpeditionState.CONQUERED && chest.isOwnershipExpired()) {
                UUID ownerUUID = chest.getOwnerUUID();
                if (ownerUUID != null) {
                    Player owner = Bukkit.getPlayer(ownerUUID);
                    if (owner != null && owner.isOnline()) {
                        owner.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgOwnershipExpired()));
                    }
                }
                
                chest.setState(ExpeditionState.READY);
                chest.setOwnerUUID(null);
                
                plugin.getHologramManager().updateHologram(chest, 
                    config.getHologramTitle(), 
                    config.getHologramStatusReady());
            }
        }
    }

    private void checkCooldownExpirations() {
        ConfigManager config = plugin.getConfigManager();
        
        for (ExpeditionChest chest : plugin.getDataManager().getExpeditionChests().values()) {
            if (chest.getState() == ExpeditionState.COOLDOWN && chest.isCooldownExpired()) {
                chest.setState(ExpeditionState.READY);
                
                plugin.getHologramManager().updateHologram(chest, 
                    config.getHologramTitle(), 
                    config.getHologramStatusReady());
            }
        }
    }

    private void updateHolograms() {
        ConfigManager config = plugin.getConfigManager();
        
        for (ExpeditionChest chest : plugin.getDataManager().getExpeditionChests().values()) {
            switch (chest.getState()) {
                case CONQUERED -> {
                    if (chest.getOwnerUUID() != null) {
                        Player owner = Bukkit.getPlayer(chest.getOwnerUUID());
                        String ownerName = owner != null ? owner.getName() : "Bilinmiyor";
                        plugin.getHologramManager().updateHologram(chest, 
                            config.getHologramTitle(),
                            config.getHologramStatusOwner().replace("%player%", ownerName),
                            config.getHologramStatusTimeLeft().replace("%time%", ColorUtils.formatTime(chest.getRemainingOwnershipTime())));
                    }
                }
                case COOLDOWN -> {
                    plugin.getHologramManager().updateHologram(chest, 
                        config.getHologramTitle(),
                        config.getHologramStatusCooldown().replace("%time%", ColorUtils.formatTime(chest.getRemainingCooldown())));
                }
            }
        }
    }

    public ExpeditionChest getNearbyExpeditionChest(Location location, double radius) {
        for (ExpeditionChest chest : plugin.getDataManager().getExpeditionChests().values()) {
            Location chestLoc = chest.getLocation();
            if (chestLoc.getWorld() != null && chestLoc.getWorld().equals(location.getWorld())) {
                if (chestLoc.distance(location) <= radius) {
                    return chest;
                }
            }
        }
        return null;
    }
}

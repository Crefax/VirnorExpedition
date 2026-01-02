package com.virnor.expedition.hologram;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.config.ConfigManager;
import com.virnor.expedition.data.ExpeditionChest;
import com.virnor.expedition.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.*;

public class HologramManager {

    private final VirnorExpedition plugin;
    private final Map<String, List<ArmorStand>> holograms;

    public HologramManager(VirnorExpedition plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();
    }

    public void createHologram(ExpeditionChest chest, String... lines) {
        if (!plugin.getConfigManager().isHologramEnabled()) return;
        
        removeHologram(chest);
        
        ConfigManager config = plugin.getConfigManager();
        Location baseLoc = chest.getLocation().clone().add(0.5, config.getHologramHeightOffset(), 0.5);
        List<ArmorStand> stands = new ArrayList<>();
        
        double lineSpacing = config.getHologramLineSpacing();
        
        for (int i = 0; i < lines.length; i++) {
            Location lineLoc = baseLoc.clone().subtract(0, i * lineSpacing, 0);
            ArmorStand stand = createHologramLine(lineLoc, lines[i]);
            stands.add(stand);
        }
        
        holograms.put(chest.getId(), stands);
    }

    public void updateHologram(ExpeditionChest chest, String... lines) {
        if (!plugin.getConfigManager().isHologramEnabled()) return;
        
        List<ArmorStand> stands = holograms.get(chest.getId());
        
        if (stands == null || stands.size() != lines.length) {
            createHologram(chest, lines);
            return;
        }
        
        for (int i = 0; i < lines.length; i++) {
            if (i < stands.size()) {
                ArmorStand stand = stands.get(i);
                if (stand != null && !stand.isDead()) {
                    stand.setCustomName(ColorUtils.colorize(lines[i]));
                }
            }
        }
    }

    public void removeHologram(ExpeditionChest chest) {
        List<ArmorStand> stands = holograms.remove(chest.getId());
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
            }
        }
    }

    public void removeAllHolograms() {
        for (List<ArmorStand> stands : holograms.values()) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
            }
        }
        holograms.clear();
    }

    private ArmorStand createHologramLine(Location location, String text) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setCustomName(ColorUtils.colorize(text));
        stand.setCustomNameVisible(true);
        stand.setSmall(true);
        stand.setMarker(true);
        stand.setInvulnerable(true);
        stand.setPersistent(true);
        
        return stand;
    }

    public void reloadHolograms() {
        removeAllHolograms();
        
        if (!plugin.getConfigManager().isHologramEnabled()) return;
        
        ConfigManager config = plugin.getConfigManager();
        
        for (ExpeditionChest chest : plugin.getDataManager().getExpeditionChests().values()) {
            switch (chest.getState()) {
                case READY -> createHologram(chest, 
                    config.getHologramTitle(), 
                    config.getHologramStatusReady());
                case ACTIVE -> createHologram(chest, 
                    config.getHologramTitle(), 
                    config.getHologramStatusFighting());
                case CONQUERED -> {
                    String ownerName = "Bilinmiyor";
                    if (chest.getOwnerUUID() != null) {
                        var player = plugin.getServer().getPlayer(chest.getOwnerUUID());
                        if (player != null) {
                            ownerName = player.getName();
                        }
                    }
                    createHologram(chest, 
                        config.getHologramTitle(),
                        config.getHologramStatusOwner().replace("%player%", ownerName),
                        config.getHologramStatusTimeLeft().replace("%time%", ColorUtils.formatTime(chest.getRemainingOwnershipTime())));
                }
                case COOLDOWN -> createHologram(chest, 
                    config.getHologramTitle(),
                    config.getHologramStatusCooldown().replace("%time%", ColorUtils.formatTime(chest.getRemainingCooldown())));
            }
        }
    }
    
    // Helper methods for creating holograms with config messages
    public void createReadyHologram(ExpeditionChest chest) {
        ConfigManager config = plugin.getConfigManager();
        createHologram(chest, config.getHologramTitle(), config.getHologramStatusReady());
    }
    
    public void createFightingHologram(ExpeditionChest chest) {
        ConfigManager config = plugin.getConfigManager();
        createHologram(chest, config.getHologramTitle(), config.getHologramStatusFighting());
    }
    
    public void createConqueredHologram(ExpeditionChest chest, String playerName) {
        ConfigManager config = plugin.getConfigManager();
        updateHologram(chest, 
            config.getHologramTitle(),
            config.getHologramStatusOwner().replace("%player%", playerName),
            config.getHologramStatusTimeLeft().replace("%time%", ColorUtils.formatTime(chest.getRemainingOwnershipTime())));
    }
    
    public void createCooldownHologram(ExpeditionChest chest) {
        ConfigManager config = plugin.getConfigManager();
        updateHologram(chest, 
            config.getHologramTitle(),
            config.getHologramStatusCooldown().replace("%time%", ColorUtils.formatTime(chest.getRemainingCooldown())));
    }
    
    public void updateConqueredTime(ExpeditionChest chest, String playerName) {
        ConfigManager config = plugin.getConfigManager();
        updateHologram(chest, 
            config.getHologramTitle(),
            config.getHologramStatusOwner().replace("%player%", playerName),
            config.getHologramStatusTimeLeft().replace("%time%", ColorUtils.formatTime(chest.getRemainingOwnershipTime())));
    }
    
    public void updateCooldownTime(ExpeditionChest chest) {
        ConfigManager config = plugin.getConfigManager();
        updateHologram(chest, 
            config.getHologramTitle(),
            config.getHologramStatusCooldown().replace("%time%", ColorUtils.formatTime(chest.getRemainingCooldown())));
    }
}

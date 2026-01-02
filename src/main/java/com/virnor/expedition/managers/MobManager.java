package com.virnor.expedition.managers;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.config.ConfigManager;
import com.virnor.expedition.data.ExpeditionChest;
import com.virnor.expedition.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

public class MobManager {

    private final VirnorExpedition plugin;
    private final Map<UUID, String> mobToChestMap; // Mob UUID -> Chest ID

    public MobManager(VirnorExpedition plugin) {
        this.plugin = plugin;
        this.mobToChestMap = new HashMap<>();
    }

    public void spawnMobsForChest(ExpeditionChest chest) {
        ConfigManager config = plugin.getConfigManager();
        Location chestLoc = chest.getLocation();
        
        int mobCount = config.getMobCount();
        double radius = config.getMobSpawnRadius();
        
        chest.clearMobs();
        
        for (int i = 0; i < mobCount; i++) {
            // Calculate spawn position around chest
            double angle = (2 * Math.PI / mobCount) * i;
            double x = chestLoc.getX() + 0.5 + (radius * Math.cos(angle));
            double z = chestLoc.getZ() + 0.5 + (radius * Math.sin(angle));
            Location spawnLoc = new Location(chestLoc.getWorld(), x, chestLoc.getY() + 1, z);
            
            // Make sure location is safe
            while (!spawnLoc.getBlock().isPassable() && spawnLoc.getY() < chestLoc.getY() + 5) {
                spawnLoc.add(0, 1, 0);
            }
            
            // Spawn the mob
            LivingEntity mob = (LivingEntity) chestLoc.getWorld().spawnEntity(spawnLoc, config.getMobType());
            
            // Configure mob
            configureMob(mob, chest);
            
            // Track mob
            chest.addMobUUID(mob.getUniqueId());
            mobToChestMap.put(mob.getUniqueId(), chest.getId());
        }
        
        chest.setMobsAlive(mobCount);
    }

    private void configureMob(LivingEntity mob, ExpeditionChest chest) {
        ConfigManager config = plugin.getConfigManager();
        
        // Set custom name
        mob.setCustomName(ColorUtils.colorize(config.getMobName()));
        mob.setCustomNameVisible(true);
        
        // Set health
        if (mob.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(config.getMobHealth());
            mob.setHealth(config.getMobHealth());
        }
        
        // Set metadata
        mob.setMetadata("expedition_mob", new FixedMetadataValue(plugin, chest.getId()));
        mob.setMetadata("expedition_chest_id", new FixedMetadataValue(plugin, chest.getId()));
        
        // Prevent mob from despawning
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
        
        // Make mob aggressive if possible
        if (mob instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
        }
        if (mob instanceof Skeleton skeleton) {
            skeleton.setShouldBurnInDay(false);
        }
    }

    public void removeMobsForChest(ExpeditionChest chest) {
        for (UUID mobUUID : chest.getMobUUIDs()) {
            Entity entity = plugin.getServer().getEntity(mobUUID);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
            mobToChestMap.remove(mobUUID);
        }
        chest.clearMobs();
    }

    public void removeAllMobs() {
        for (ExpeditionChest chest : plugin.getDataManager().getExpeditionChests().values()) {
            removeMobsForChest(chest);
        }
        mobToChestMap.clear();
    }

    public String getChestIdByMob(UUID mobUUID) {
        return mobToChestMap.get(mobUUID);
    }

    public boolean isExpeditionMob(Entity entity) {
        return entity.hasMetadata("expedition_mob");
    }

    public void teleportMobsToChest(ExpeditionChest chest) {
        ConfigManager config = plugin.getConfigManager();
        Location chestLoc = chest.getLocation();
        double radius = config.getMobSpawnRadius();
        
        List<UUID> mobUUIDs = new ArrayList<>(chest.getMobUUIDs());
        int index = 0;
        
        for (UUID mobUUID : mobUUIDs) {
            Entity entity = plugin.getServer().getEntity(mobUUID);
            if (entity != null && !entity.isDead()) {
                double angle = (2 * Math.PI / mobUUIDs.size()) * index;
                double x = chestLoc.getX() + 0.5 + (radius * Math.cos(angle));
                double z = chestLoc.getZ() + 0.5 + (radius * Math.sin(angle));
                Location teleportLoc = new Location(chestLoc.getWorld(), x, chestLoc.getY() + 1, z);
                
                entity.teleport(teleportLoc);
                index++;
            }
        }
    }

    public void checkMobDistances() {
        ConfigManager config = plugin.getConfigManager();
        double teleportDistance = config.getTeleportDistance();
        
        for (ExpeditionChest chest : plugin.getDataManager().getExpeditionChests().values()) {
            if (chest.getMobsAlive() <= 0) continue;
            
            Location chestLoc = chest.getLocation();
            
            for (UUID mobUUID : chest.getMobUUIDs()) {
                Entity entity = plugin.getServer().getEntity(mobUUID);
                if (entity != null && !entity.isDead()) {
                    double distance = entity.getLocation().distance(chestLoc);
                    if (distance > teleportDistance) {
                        // Teleport mob back to chest
                        double angle = Math.random() * 2 * Math.PI;
                        double radius = config.getMobSpawnRadius();
                        double x = chestLoc.getX() + 0.5 + (radius * Math.cos(angle));
                        double z = chestLoc.getZ() + 0.5 + (radius * Math.sin(angle));
                        Location teleportLoc = new Location(chestLoc.getWorld(), x, chestLoc.getY() + 1, z);
                        
                        entity.teleport(teleportLoc);
                    }
                }
            }
        }
    }

    public void onMobDeath(UUID mobUUID) {
        String chestId = mobToChestMap.remove(mobUUID);
        if (chestId != null) {
            ExpeditionChest chest = plugin.getDataManager().getExpeditionChest(chestId);
            if (chest != null) {
                chest.removeMobUUID(mobUUID);
                chest.decrementMobsAlive();
            }
        }
    }

    public Map<UUID, String> getMobToChestMap() {
        return mobToChestMap;
    }
}

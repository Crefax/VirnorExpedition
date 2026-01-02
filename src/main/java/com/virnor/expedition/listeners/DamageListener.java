package com.virnor.expedition.listeners;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.managers.MobManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Listens for damage events on expedition mobs to track player damage and update health bars
 */
public class DamageListener implements Listener {

    private final VirnorExpedition plugin;

    public DamageListener(VirnorExpedition plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        
        // Check if damaged entity is an expedition mob
        MobManager mobManager = plugin.getMobManager();
        if (!mobManager.isExpeditionMob(damaged)) {
            return;
        }
        
        // Get the chest ID for this mob
        String chestId = mobManager.getChestIdByMob(damaged.getUniqueId());
        if (chestId == null) return;
        
        // Get the damager (handle projectiles)
        Player damager = getDamager(event.getDamager());
        if (damager == null) return;
        
        // Record the damage
        double finalDamage = event.getFinalDamage();
        plugin.getDamageTracker().recordDamage(chestId, damager.getUniqueId(), finalDamage);
        
        // Update health bar after a tick (to account for damage being applied)
        if (damaged instanceof LivingEntity livingEntity) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!livingEntity.isDead()) {
                    plugin.getDamageTracker().updateHealthBar(livingEntity);
                }
            }, 1L);
        }
    }

    /**
     * Get the player who caused damage (handles direct hits and projectiles)
     */
    private Player getDamager(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        
        return null;
    }
}

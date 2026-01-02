package com.virnor.expedition.listeners;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.data.ExpeditionChest;
import com.virnor.expedition.data.ExpeditionState;
import com.virnor.expedition.managers.DamageTracker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobDeathListener implements Listener {

    private final VirnorExpedition plugin;

    public MobDeathListener(VirnorExpedition plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Check if this is an expedition mob
        if (!plugin.getMobManager().isExpeditionMob(entity)) {
            return;
        }

        // Remove default drops
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Get the chest ID this mob belongs to
        String chestId = plugin.getMobManager().getChestIdByMob(entity.getUniqueId());
        if (chestId == null) return;

        ExpeditionChest chest = plugin.getDataManager().getExpeditionChest(chestId);
        if (chest == null) return;

        // Update mob tracking
        plugin.getMobManager().onMobDeath(entity.getUniqueId());

        // Check if all mobs are dead
        if (chest.getMobsAlive() <= 0 && chest.getState() == ExpeditionState.ACTIVE) {
            // Get the killer (last hitter)
            Player lastHitter = entity.getKiller();
            
            // Determine the winner based on config (MOST_DAMAGE or LAST_HIT)
            DamageTracker damageTracker = plugin.getDamageTracker();
            Player winner = damageTracker.determineLootWinner(chestId, lastHitter);
            
            if (winner != null) {
                // Announce damage results to all participants
                damageTracker.announceDamageResults(chestId, winner);
                
                // Give ownership to winner
                plugin.getExpeditionManager().onAllMobsKilled(chest, winner);
            } else {
                // No player killed the last mob, reset the chest
                chest.setState(ExpeditionState.READY);
                plugin.getHologramManager().updateHologram(chest, 
                    plugin.getConfigManager().getHologramTitle(), 
                    plugin.getConfigManager().getHologramStatusReady());
            }
            
            // Clear damage tracking for this chest
            damageTracker.clearChestDamage(chestId);
        }
    }
}

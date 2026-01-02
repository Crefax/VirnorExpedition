package com.virnor.expedition.listeners;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.data.ExpeditionChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

public class MobTargetListener implements Listener {

    private final VirnorExpedition plugin;

    public MobTargetListener(VirnorExpedition plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();
        
        // Check if this is an expedition mob
        if (!plugin.getMobManager().isExpeditionMob(entity)) {
            return;
        }

        // Only allow targeting players
        if (!(event.getTarget() instanceof Player)) {
            event.setCancelled(true);
            return;
        }

        // Make sure mob targets players near the chest
        String chestId = plugin.getMobManager().getChestIdByMob(entity.getUniqueId());
        if (chestId != null) {
            ExpeditionChest chest = plugin.getDataManager().getExpeditionChest(chestId);
            if (chest != null) {
                Player target = (Player) event.getTarget();
                double distance = target.getLocation().distance(chest.getLocation());
                
                // Only target if player is within teleport distance
                if (distance > plugin.getConfigManager().getTeleportDistance()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}

package com.virnor.expedition.listeners;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.data.ExpeditionChest;
import com.virnor.expedition.data.ExpeditionState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final VirnorExpedition plugin;

    public PlayerMoveListener(VirnorExpedition plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if player actually moved to a new block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        double spawnDistance = plugin.getConfigManager().getSpawnDistance();

        for (ExpeditionChest chest : plugin.getDataManager().getExpeditionChests().values()) {
            if (chest.getState() != ExpeditionState.READY) continue;
            if (!chest.getLocation().getWorld().equals(player.getWorld())) continue;

            double distance = player.getLocation().distance(chest.getLocation());
            if (distance <= spawnDistance) {
                plugin.getExpeditionManager().activateChest(chest, player);
            }
        }
    }
}

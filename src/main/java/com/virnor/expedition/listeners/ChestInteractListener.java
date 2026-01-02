package com.virnor.expedition.listeners;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.config.ConfigManager;
import com.virnor.expedition.data.ExpeditionChest;
import com.virnor.expedition.data.ExpeditionState;
import com.virnor.expedition.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ChestInteractListener implements Listener {

    private final VirnorExpedition plugin;

    public ChestInteractListener(VirnorExpedition plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;

        ExpeditionChest chest = plugin.getDataManager().getChestByLocation(block.getLocation());
        if (chest == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        ConfigManager config = plugin.getConfigManager();

        // Check loot permission
        if (!player.hasPermission("expedition.loot")) {
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNoPermission()));
            return;
        }

        switch (chest.getState()) {
            case READY -> {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgApproachToActivate()));
            }
            case ACTIVE -> {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgKillMobsFirst()));
            }
            case CONQUERED -> {
                // Check ownership bypass permission
                boolean canBypass = player.hasPermission("expedition.bypass.ownership");
                
                if (!canBypass && (chest.getOwnerUUID() == null || !chest.getOwnerUUID().equals(player.getUniqueId()))) {
                    player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNotOwner()));
                    return;
                }

                if (chest.isOwnershipExpired()) {
                    player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgOwnershipExpired()));
                    chest.setState(ExpeditionState.READY);
                    chest.setOwnerUUID(null);
                    plugin.getHologramManager().updateHologram(chest, 
                        config.getHologramTitle(), 
                        config.getHologramStatusReady());
                    return;
                }

                plugin.getLootManager().giveLootToPlayer(player);
                plugin.getExpeditionManager().onLootClaimed(chest, player);
            }
            case COOLDOWN -> {
                // Check cooldown bypass permission
                if (player.hasPermission("expedition.bypass.cooldown")) {
                    // Bypass cooldown, give loot anyway
                    chest.setState(ExpeditionState.CONQUERED);
                    chest.setOwnerUUID(player.getUniqueId());
                    chest.setOwnershipExpireTime(System.currentTimeMillis() + (config.getOwnershipDuration() * 1000L));
                    
                    plugin.getLootManager().giveLootToPlayer(player);
                    plugin.getExpeditionManager().onLootClaimed(chest, player);
                    return;
                }
                
                String timeLeft = ColorUtils.formatTime(chest.getRemainingCooldown());
                String message = config.getMsgCooldown().replace("%time%", timeLeft);
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + message));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) return;

        ExpeditionChest chest = plugin.getDataManager().getChestByLocation(block.getLocation());
        if (chest == null) return;

        Player player = event.getPlayer();
        ConfigManager config = plugin.getConfigManager();

        if (!player.hasPermission("expedition.admin")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgCannotBreakChest()));
            return;
        }

        event.setCancelled(true);
        plugin.getExpeditionManager().removeExpeditionChest(chest.getId());
        player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgChestRemoved()));
    }
}

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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChestInteractListener implements Listener {

    private final VirnorExpedition plugin;
    // Prevent double-click exploits
    private final Set<UUID> claimingPlayers = new HashSet<>();

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
                // Prevent double-click exploit
                if (claimingPlayers.contains(player.getUniqueId())) {
                    return;
                }
                
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

                // Double-check state hasn't changed (thread safety)
                if (chest.getState() != ExpeditionState.CONQUERED) {
                    return;
                }
                
                // Mark player as claiming to prevent double-click
                claimingPlayers.add(player.getUniqueId());
                
                // FIRST set state to COOLDOWN, THEN give loot
                plugin.getExpeditionManager().onLootClaimed(chest, player);
                plugin.getLootManager().giveLootToPlayer(player);
                
                // Remove from claiming set after a short delay
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    claimingPlayers.remove(player.getUniqueId());
                }, 20L);
            }
            case COOLDOWN -> {
                // Check cooldown bypass permission
                if (player.hasPermission("expedition.bypass.cooldown")) {
                    // Prevent double-click exploit
                    if (claimingPlayers.contains(player.getUniqueId())) {
                        return;
                    }
                    
                    claimingPlayers.add(player.getUniqueId());
                    
                    // Give loot and reset cooldown
                    plugin.getLootManager().giveLootToPlayer(player);
                    
                    // Reset cooldown timer
                    chest.setCooldownExpireTime(System.currentTimeMillis() + (config.getCooldownDuration() * 1000L));
                    plugin.getHologramManager().updateHologram(chest, 
                        config.getHologramTitle(),
                        config.getHologramStatusCooldown().replace("%time%", ColorUtils.formatTime(chest.getRemainingCooldown())));
                    
                    player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgLootReceived()));
                    
                    // Remove from claiming set after a short delay
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        claimingPlayers.remove(player.getUniqueId());
                    }, 20L);
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

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
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashSet;
import java.util.Set;

public class ChestInteractListener implements Listener {

    private final VirnorExpedition plugin;
    // Prevent double-click exploits - stores chest IDs currently being claimed
    private final Set<String> chestsBeingClaimed = new HashSet<>();

    public ChestInteractListener(VirnorExpedition plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle main hand to prevent double event
        if (event.getHand() != EquipmentSlot.HAND) return;
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
                handleConqueredChest(player, chest, config);
            }
            case COOLDOWN -> {
                handleCooldownChest(player, chest, config);
            }
        }
    }

    private void handleConqueredChest(Player player, ExpeditionChest chest, ConfigManager config) {
        // Check if this chest is already being claimed
        synchronized (chestsBeingClaimed) {
            if (chestsBeingClaimed.contains(chest.getId())) {
                return;
            }
        }
        
        // Check ownership - only owner or bypass can claim
        boolean canBypass = player.hasPermission("expedition.bypass.ownership");
        
        if (!canBypass) {
            if (chest.getOwnerUUID() == null) {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNotOwner()));
                return;
            }
            if (!chest.getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNotOwner()));
                return;
            }
        }

        // Check if ownership expired
        if (chest.isOwnershipExpired()) {
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgOwnershipExpired()));
            chest.setState(ExpeditionState.READY);
            chest.setOwnerUUID(null);
            plugin.getHologramManager().updateHologram(chest, 
                config.getHologramTitle(), 
                config.getHologramStatusReady());
            return;
        }

        // Double-check state hasn't changed
        if (chest.getState() != ExpeditionState.CONQUERED) {
            return;
        }
        
        // Lock this chest for claiming
        synchronized (chestsBeingClaimed) {
            if (chestsBeingClaimed.contains(chest.getId())) {
                return;
            }
            chestsBeingClaimed.add(chest.getId());
        }
        
        try {
            // FIRST change state to COOLDOWN
            chest.setState(ExpeditionState.COOLDOWN);
            chest.setCooldownExpireTime(System.currentTimeMillis() + (config.getCooldownDuration() * 1000L));
            chest.setOwnerUUID(null);
            
            // Update hologram
            plugin.getHologramManager().updateHologram(chest, 
                config.getHologramTitle(),
                config.getHologramStatusCooldown().replace("%time%", ColorUtils.formatTime(chest.getRemainingCooldown())));
            
            // THEN give loot
            plugin.getLootManager().giveLootToPlayer(player);
            
            // Send message
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgLootReceived()));
            
            // Save data
            plugin.getDataManager().saveData();
        } finally {
            // Unlock chest after a delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                synchronized (chestsBeingClaimed) {
                    chestsBeingClaimed.remove(chest.getId());
                }
            }, 20L);
        }
    }

    private void handleCooldownChest(Player player, ExpeditionChest chest, ConfigManager config) {
        // Check cooldown bypass permission
        if (player.hasPermission("expedition.bypass.cooldown")) {
            // Check if this chest is already being claimed
            synchronized (chestsBeingClaimed) {
                if (chestsBeingClaimed.contains(chest.getId())) {
                    return;
                }
                chestsBeingClaimed.add(chest.getId());
            }
            
            try {
                // Give loot (state stays COOLDOWN)
                plugin.getLootManager().giveLootToPlayer(player);
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgLootReceived()));
            } finally {
                // Unlock chest after a delay
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    synchronized (chestsBeingClaimed) {
                        chestsBeingClaimed.remove(chest.getId());
                    }
                }, 20L);
            }
            return;
        }
        
        // Show cooldown message
        String timeLeft = ColorUtils.formatTime(chest.getRemainingCooldown());
        String message = config.getMsgCooldown().replace("%time%", timeLeft);
        player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + message));
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

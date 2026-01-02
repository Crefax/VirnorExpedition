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

import java.util.UUID;

public class ChestInteractListener implements Listener {

    private final VirnorExpedition plugin;

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

        // Get current state ONCE at the beginning
        ExpeditionState currentState = chest.getState();

        switch (currentState) {
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
        // STEP 1: Check ownership FIRST before anything else
        boolean hasOwnershipBypass = player.hasPermission("expedition.bypass.ownership");
        UUID ownerUUID = chest.getOwnerUUID();
        UUID playerUUID = player.getUniqueId();
        
        // If no bypass permission, must be the owner
        if (!hasOwnershipBypass) {
            // No owner set = no one can claim
            if (ownerUUID == null) {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNotOwner()));
                return;
            }
            // Not the owner
            if (!ownerUUID.equals(playerUUID)) {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNotOwner()));
                return;
            }
        }

        // STEP 2: Check if ownership time expired
        if (chest.isOwnershipExpired()) {
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgOwnershipExpired()));
            chest.setState(ExpeditionState.READY);
            chest.setOwnerUUID(null);
            plugin.getHologramManager().updateHologram(chest, 
                config.getHologramTitle(), 
                config.getHologramStatusReady());
            return;
        }

        // STEP 3: Try to acquire the claim lock
        if (!chest.tryClaimLock()) {
            // Already being claimed or recently claimed
            return;
        }
        
        // STEP 4: Double-check state is still CONQUERED (after getting lock)
        if (chest.getState() != ExpeditionState.CONQUERED) {
            chest.releaseLock(false);
            return;
        }
        
        // STEP 5: Process the claim
        boolean success = false;
        try {
            // Change state to COOLDOWN immediately
            chest.setState(ExpeditionState.COOLDOWN);
            chest.setCooldownExpireTime(System.currentTimeMillis() + (config.getCooldownDuration() * 1000L));
            chest.setOwnerUUID(null);
            
            // Update hologram
            plugin.getHologramManager().updateHologram(chest, 
                config.getHologramTitle(),
                config.getHologramStatusCooldown().replace("%time%", ColorUtils.formatTime(chest.getRemainingCooldown())));
            
            // Give loot
            plugin.getLootManager().giveLootToPlayer(player);
            
            // Send message
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgLootReceived()));
            
            // Save data
            plugin.getDataManager().saveData();
            
            success = true;
        } finally {
            // Release lock after processing
            chest.releaseLock(success);
        }
    }

    private void handleCooldownChest(Player player, ExpeditionChest chest, ConfigManager config) {
        // Only bypass permission holders can claim during cooldown
        if (!player.hasPermission("expedition.bypass.cooldown")) {
            String timeLeft = ColorUtils.formatTime(chest.getRemainingCooldown());
            String message = config.getMsgCooldown().replace("%time%", timeLeft);
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + message));
            return;
        }
        
        // Try to acquire lock
        if (!chest.tryClaimLock()) {
            return;
        }
        
        boolean success = false;
        try {
            // Give loot (state stays COOLDOWN, timer not reset)
            plugin.getLootManager().giveLootToPlayer(player);
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgLootReceived()));
            success = true;
        } finally {
            chest.releaseLock(success);
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

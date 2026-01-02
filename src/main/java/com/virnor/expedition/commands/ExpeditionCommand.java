package com.virnor.expedition.commands;

import com.virnor.expedition.VirnorExpedition;
import com.virnor.expedition.config.ConfigManager;
import com.virnor.expedition.data.ExpeditionChest;
import com.virnor.expedition.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ExpeditionCommand implements CommandExecutor {

    private final VirnorExpedition plugin;

    public ExpeditionCommand(VirnorExpedition plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager config = plugin.getConfigManager();
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize("&cBu komutu sadece oyuncular kullanabilir!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (!player.hasPermission("expedition.admin")) {
                    player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNoPermission()));
                    return true;
                }
                handleSet(player);
            }
            case "admin" -> {
                if (!player.hasPermission("expedition.admin")) {
                    player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNoPermission()));
                    return true;
                }
                handleAdmin(player);
            }
            case "remove" -> {
                if (!player.hasPermission("expedition.admin")) {
                    player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNoPermission()));
                    return true;
                }
                handleRemove(player, args);
            }
            case "list" -> {
                if (!player.hasPermission("expedition.admin")) {
                    player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNoPermission()));
                    return true;
                }
                handleList(player);
            }
            case "reload" -> {
                if (!player.hasPermission("expedition.admin")) {
                    player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgNoPermission()));
                    return true;
                }
                handleReload(player);
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleSet(Player player) {
        ConfigManager config = plugin.getConfigManager();
        
        var targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + "&cBir bloğa bakmalısın!"));
            return;
        }

        var location = targetBlock.getLocation().add(0, 1, 0);
        
        if (plugin.getDataManager().getChestByLocation(location) != null) {
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + "&cBu konumda zaten bir expedition chest var!"));
            return;
        }

        if (plugin.getExpeditionManager().createExpeditionChest(location)) {
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgChestCreated()));
        } else {
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + "&cExpedition chest oluşturulurken bir hata oluştu!"));
        }
    }

    private void handleAdmin(Player player) {
        plugin.getGuiManager().openMainMenu(player);
    }

    private void handleRemove(Player player, String[] args) {
        ConfigManager config = plugin.getConfigManager();
        
        if (args.length < 2) {
            var targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null) {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + "&cBir expedition chest'e bakmalısın veya ID belirtmelisin!"));
                return;
            }

            ExpeditionChest chest = plugin.getDataManager().getChestByLocation(targetBlock.getLocation());
            if (chest == null) {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgChestNotFound()));
                return;
            }

            if (plugin.getExpeditionManager().removeExpeditionChest(chest.getId())) {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgChestRemoved()));
            } else {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + "&cExpedition chest silinirken bir hata oluştu!"));
            }
        } else {
            String id = args[1];
            if (plugin.getExpeditionManager().removeExpeditionChest(id)) {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgChestRemoved()));
            } else {
                player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgChestNotFound()));
            }
        }
    }

    private void handleList(Player player) {
        ConfigManager config = plugin.getConfigManager();
        var chests = plugin.getDataManager().getExpeditionChests();
        
        if (chests.isEmpty()) {
            player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + "&eHiç expedition chest yok."));
            return;
        }

        player.sendMessage(ColorUtils.colorize("&6&l=== Expedition Chests ==="));
        for (ExpeditionChest chest : chests.values()) {
            var loc = chest.getLocation();
            player.sendMessage(ColorUtils.colorize(String.format(
                "&e%s &7- &f%s &7(%d, %d, %d) &7- &f%s",
                chest.getId(),
                loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                chest.getState().name()
            )));
        }
    }

    private void handleReload(Player player) {
        ConfigManager config = plugin.getConfigManager();
        
        plugin.reloadConfig();
        config.loadConfig();
        plugin.getLootManager().loadLoots();
        plugin.getHologramManager().reloadHolograms();
        
        player.sendMessage(ColorUtils.colorize(config.getMsgPrefix() + config.getMsgConfigReloaded()));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtils.colorize("&6&l=== Expedition Komutları ==="));
        player.sendMessage(ColorUtils.colorize("&e/expedition set &7- Baktığın yere expedition chest koy"));
        player.sendMessage(ColorUtils.colorize("&e/expedition admin &7- Admin ayarlar menüsünü aç"));
        player.sendMessage(ColorUtils.colorize("&e/expedition remove [id] &7- Expedition chest sil"));
        player.sendMessage(ColorUtils.colorize("&e/expedition list &7- Tüm expedition chestleri listele"));
        player.sendMessage(ColorUtils.colorize("&e/expedition reload &7- Configleri yeniden yükle"));
        player.sendMessage(ColorUtils.colorize(""));
        player.sendMessage(ColorUtils.colorize("&6&lPermissions:"));
        player.sendMessage(ColorUtils.colorize("&e expedition.admin &7- Admin komutları"));
        player.sendMessage(ColorUtils.colorize("&e expedition.loot &7- Loot alma izni"));
        player.sendMessage(ColorUtils.colorize("&e expedition.bypass.cooldown &7- Cooldown bypass"));
        player.sendMessage(ColorUtils.colorize("&e expedition.bypass.ownership &7- Sahiplik bypass"));
    }
}

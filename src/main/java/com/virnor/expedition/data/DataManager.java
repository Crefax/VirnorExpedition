package com.virnor.expedition.data;

import com.virnor.expedition.VirnorExpedition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final VirnorExpedition plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<String, ExpeditionChest> expeditionChests;

    public DataManager(VirnorExpedition plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.expeditionChests = new HashMap<>();
    }

    public void loadData() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml!");
                e.printStackTrace();
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        expeditionChests.clear();
        
        ConfigurationSection chestsSection = dataConfig.getConfigurationSection("chests");
        if (chestsSection != null) {
            for (String id : chestsSection.getKeys(false)) {
                ConfigurationSection chestSection = chestsSection.getConfigurationSection(id);
                if (chestSection != null) {
                    String worldName = chestSection.getString("world");
                    double x = chestSection.getDouble("x");
                    double y = chestSection.getDouble("y");
                    double z = chestSection.getDouble("z");
                    
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        Location location = new Location(world, x, y, z);
                        ExpeditionChest chest = new ExpeditionChest(id, location);
                        
                        // Load state
                        String stateStr = chestSection.getString("state", "READY");
                        chest.setState(ExpeditionState.valueOf(stateStr));
                        
                        // Load cooldown
                        long cooldownExpire = chestSection.getLong("cooldownExpire", 0);
                        chest.setCooldownExpireTime(cooldownExpire);
                        
                        // Check if cooldown expired
                        if (chest.getState() == ExpeditionState.COOLDOWN && chest.isCooldownExpired()) {
                            chest.setState(ExpeditionState.READY);
                        }
                        
                        expeditionChests.put(id, chest);
                        plugin.getLogger().info("Loaded expedition chest: " + id);
                    }
                }
            }
        }
    }

    public void saveData() {
        dataConfig = new YamlConfiguration();
        
        for (Map.Entry<String, ExpeditionChest> entry : expeditionChests.entrySet()) {
            String id = entry.getKey();
            ExpeditionChest chest = entry.getValue();
            Location loc = chest.getLocation();
            
            String path = "chests." + id;
            dataConfig.set(path + ".world", loc.getWorld().getName());
            dataConfig.set(path + ".x", loc.getX());
            dataConfig.set(path + ".y", loc.getY());
            dataConfig.set(path + ".z", loc.getZ());
            dataConfig.set(path + ".state", chest.getState().name());
            dataConfig.set(path + ".cooldownExpire", chest.getCooldownExpireTime());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
            e.printStackTrace();
        }
    }

    public Map<String, ExpeditionChest> getExpeditionChests() {
        return expeditionChests;
    }

    public ExpeditionChest getExpeditionChest(String id) {
        return expeditionChests.get(id);
    }

    public void addExpeditionChest(ExpeditionChest chest) {
        expeditionChests.put(chest.getId(), chest);
        saveData();
    }

    public void removeExpeditionChest(String id) {
        expeditionChests.remove(id);
        saveData();
    }

    public ExpeditionChest getChestByLocation(Location location) {
        for (ExpeditionChest chest : expeditionChests.values()) {
            Location chestLoc = chest.getLocation();
            if (chestLoc.getWorld().equals(location.getWorld()) &&
                chestLoc.getBlockX() == location.getBlockX() &&
                chestLoc.getBlockY() == location.getBlockY() &&
                chestLoc.getBlockZ() == location.getBlockZ()) {
                return chest;
            }
        }
        return null;
    }

    public String generateChestId() {
        int counter = 1;
        while (expeditionChests.containsKey("chest_" + counter)) {
            counter++;
        }
        return "chest_" + counter;
    }
}

package com.virnor.expedition.loot;

import com.virnor.expedition.VirnorExpedition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LootManager {

    private final VirnorExpedition plugin;
    private final File lootFile;
    private FileConfiguration lootConfig;
    private final Map<String, LootSet> lootSets;
    private final Random random;

    public LootManager(VirnorExpedition plugin) {
        this.plugin = plugin;
        this.lootFile = new File(plugin.getDataFolder(), "loots.yml");
        this.lootSets = new LinkedHashMap<>();
        this.random = new Random();
    }

    public void loadLoots() {
        if (!lootFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                lootFile.createNewFile();
                // Create default loot set
                createDefaultLoot();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create loots.yml!");
                e.printStackTrace();
            }
        }
        
        lootConfig = YamlConfiguration.loadConfiguration(lootFile);
        lootSets.clear();
        
        ConfigurationSection lootsSection = lootConfig.getConfigurationSection("loots");
        if (lootsSection != null) {
            for (String lootId : lootsSection.getKeys(false)) {
                List<?> itemsList = lootsSection.getList(lootId + ".items");
                List<ItemStack> items = new ArrayList<>();
                
                if (itemsList != null) {
                    for (Object obj : itemsList) {
                        if (obj instanceof ItemStack item) {
                            items.add(item);
                        }
                    }
                }
                
                LootSet lootSet = new LootSet(lootId, items);
                lootSets.put(lootId, lootSet);
                plugin.getLogger().info("Loaded loot set: " + lootId + " with " + items.size() + " items");
            }
        }
        
        if (lootSets.isEmpty()) {
            createDefaultLoot();
        }
    }

    public void saveLoots() {
        lootConfig = new YamlConfiguration();
        
        for (Map.Entry<String, LootSet> entry : lootSets.entrySet()) {
            String lootId = entry.getKey();
            LootSet lootSet = entry.getValue();
            
            lootConfig.set("loots." + lootId + ".items", lootSet.getItems());
        }
        
        try {
            lootConfig.save(lootFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save loots.yml!");
            e.printStackTrace();
        }
    }

    private void createDefaultLoot() {
        // Create a default loot set with example items
        LootSet defaultLoot = new LootSet("loot_1");
        defaultLoot.addItem(new ItemStack(org.bukkit.Material.DIAMOND, 5));
        defaultLoot.addItem(new ItemStack(org.bukkit.Material.GOLD_INGOT, 10));
        defaultLoot.addItem(new ItemStack(org.bukkit.Material.IRON_INGOT, 20));
        
        lootSets.put("loot_1", defaultLoot);
        
        LootSet loot2 = new LootSet("loot_2");
        loot2.addItem(new ItemStack(org.bukkit.Material.EMERALD, 8));
        loot2.addItem(new ItemStack(org.bukkit.Material.DIAMOND_SWORD, 1));
        
        lootSets.put("loot_2", loot2);
        
        LootSet loot3 = new LootSet("loot_3");
        loot3.addItem(new ItemStack(org.bukkit.Material.NETHERITE_INGOT, 2));
        loot3.addItem(new ItemStack(org.bukkit.Material.GOLDEN_APPLE, 5));
        
        lootSets.put("loot_3", loot3);
        
        saveLoots();
    }

    public LootSet getRandomLootSet() {
        if (lootSets.isEmpty()) {
            return null;
        }
        
        List<LootSet> sets = new ArrayList<>(lootSets.values());
        return sets.get(random.nextInt(sets.size()));
    }

    public void giveLootToPlayer(Player player) {
        LootSet lootSet = getRandomLootSet();
        if (lootSet == null || lootSet.isEmpty()) {
            plugin.getLogger().warning("No loot sets available!");
            return;
        }
        
        for (ItemStack item : lootSet.getItems()) {
            if (item != null) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
                // Drop items that don't fit
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
        
        plugin.getLogger().info("Gave loot set '" + lootSet.getId() + "' to player " + player.getName());
    }

    public Map<String, LootSet> getLootSets() {
        return lootSets;
    }

    public LootSet getLootSet(String id) {
        return lootSets.get(id);
    }

    public void addLootSet(LootSet lootSet) {
        lootSets.put(lootSet.getId(), lootSet);
        saveLoots();
    }

    public void removeLootSet(String id) {
        lootSets.remove(id);
        saveLoots();
    }

    public String generateLootId() {
        int counter = 1;
        while (lootSets.containsKey("loot_" + counter)) {
            counter++;
        }
        return "loot_" + counter;
    }

    public void updateLootSet(String id, List<ItemStack> items) {
        LootSet lootSet = lootSets.get(id);
        if (lootSet != null) {
            lootSet.setItems(items);
            saveLoots();
        }
    }
}

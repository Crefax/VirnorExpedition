package com.virnor.expedition;

import com.virnor.expedition.commands.ExpeditionCommand;
import com.virnor.expedition.commands.ExpeditionTabCompleter;
import com.virnor.expedition.data.DataManager;
import com.virnor.expedition.gui.GUIManager;
import com.virnor.expedition.hologram.HologramManager;
import com.virnor.expedition.listeners.*;
import com.virnor.expedition.loot.LootManager;
import com.virnor.expedition.managers.DamageTracker;
import com.virnor.expedition.managers.ExpeditionManager;
import com.virnor.expedition.managers.MobManager;
import com.virnor.expedition.config.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VirnorExpedition extends JavaPlugin {

    private static VirnorExpedition instance;
    
    private ConfigManager configManager;
    private DataManager dataManager;
    private ExpeditionManager expeditionManager;
    private MobManager mobManager;
    private LootManager lootManager;
    private GUIManager guiManager;
    private HologramManager hologramManager;
    private DamageTracker damageTracker;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        lootManager = new LootManager(this);
        hologramManager = new HologramManager(this);
        mobManager = new MobManager(this);
        damageTracker = new DamageTracker(this);
        expeditionManager = new ExpeditionManager(this);
        guiManager = new GUIManager(this);
        
        // Load data
        configManager.loadConfig();
        dataManager.loadData();
        lootManager.loadLoots();
        
        // Register commands
        getCommand("expedition").setExecutor(new ExpeditionCommand(this));
        getCommand("expedition").setTabCompleter(new ExpeditionTabCompleter());
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new MobTargetListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        
        // Start tasks
        expeditionManager.startTasks();
        
        getLogger().info("VirnorExpedition has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save data
        if (dataManager != null) {
            dataManager.saveData();
        }
        if (lootManager != null) {
            lootManager.saveLoots();
        }
        
        // Cleanup
        if (mobManager != null) {
            mobManager.removeAllMobs();
        }
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }
        if (damageTracker != null) {
            damageTracker.removeAllHealthBars();
        }
        
        getLogger().info("VirnorExpedition has been disabled!");
    }

    public static VirnorExpedition getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ExpeditionManager getExpeditionManager() {
        return expeditionManager;
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public DamageTracker getDamageTracker() {
        return damageTracker;
    }
}

package com.custommobs;

import com.custommobs.UI.GUIManager;
import com.custommobs.commands.CustomMobCommand;
import com.custommobs.config.ConfigManager;
import com.custommobs.config.CustomMobDropHandler;
import com.custommobs.events.ConfigGUIListener;
import com.custommobs.events.DamageTrackingListener;
import com.custommobs.mobs.tracking.DamageTracker;
import com.custommobs.events.MobDeathListener;
import com.custommobs.events.MobSpawnListener;
import com.custommobs.mobs.CustomMobManager;
import com.custommobs.mobs.abilities.AbilityManager;
import com.custommobs.mobs.equipment.EquipmentManager;
import lombok.Getter;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Main plugin class for CustomMobs
 */
public class CustomMobsPlugin extends JavaPlugin {

    @Getter
    private static CustomMobsPlugin instance;

    @Getter
    private ConfigManager configManager;

    @Getter
    private DamageTracker damageTracker;

    @Getter
    private CustomMobDropHandler dropHandler;

    @Getter
    private CustomMobManager customMobManager;
    
    @Getter
    private AbilityManager abilityManager;
    
    @Getter
    private EquipmentManager equipmentManager;

    @Getter
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();
        this.damageTracker = new DamageTracker(this);
        this.dropHandler = new CustomMobDropHandler(this);

        // Initialize managers
        this.abilityManager = new AbilityManager(this);
        this.equipmentManager = new EquipmentManager(this);
        this.customMobManager = new CustomMobManager(this);
        this.guiManager = new GUIManager(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(new DamageTrackingListener(this), this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ConfigGUIListener(this), this); // Add this line


        // Register commands
        PluginCommand command = Objects.requireNonNull(getCommand("custommob"));
        CustomMobCommand commandExecutor = new CustomMobCommand(this);
        command.setExecutor(commandExecutor);
        command.setTabCompleter(commandExecutor);
        
        // Load abilities and equipment
        this.abilityManager.loadAbilities();
        this.equipmentManager.loadEquipment();
        
        getLogger().info("CustomMobs has been enabled!");
    }

    @Override
    public void onDisable() {
        // Clean up any active custom mobs
        if (this.customMobManager != null) {
            this.customMobManager.cleanup();
        }
        
        getLogger().info("CustomMobs has been disabled!");
    }
    
    /**
     * Logs a debug message if debug mode is enabled
     * 
     * @param message The message to log
     */
    public void debug(String message) {
        if (this.configManager != null && this.configManager.isDebugEnabled()) {
            getLogger().log(Level.INFO, "[DEBUG] " + message);
        }
    }
}
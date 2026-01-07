package com.powermobs;

import com.powermobs.UI.GUIManager;
import com.powermobs.commands.PowerMobCommand;
import com.powermobs.config.PowerManager;
import com.powermobs.config.PowerMobDropHandler;
import com.powermobs.config.SpawnBlockerManager;
import com.powermobs.events.*;
import com.powermobs.mobs.PowerMobManager;
import com.powermobs.mobs.abilities.AbilityManager;
import com.powermobs.mobs.equipment.CustomItemEffectManager;
import com.powermobs.mobs.equipment.EquipmentManager;
import com.powermobs.mobs.equipment.ItemEffectProcessor;
import com.powermobs.mobs.timing.SpawnTimerManager;
import com.powermobs.mobs.tracking.DamageTracker;
import lombok.Getter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Main plugin class for PowerMobs
 */
public class PowerMobsPlugin extends JavaPlugin {

    @Getter
    private static PowerMobsPlugin instance;

    @Getter
    private PowerManager configManager;

    @Getter
    private SpawnTimerManager spawnTimerManager;

    @Getter
    private DamageTracker damageTracker;

    @Getter
    private PowerMobDropHandler dropHandler;

    @Getter
    private PowerMobManager powerMobManager;

    @Getter
    private AbilityManager abilityManager;

    @Getter
    private EquipmentManager equipmentManager;

    @Getter
    private GUIManager guiManager;

    @Getter
    private CustomItemEffectManager itemEffectManager;

    @Getter
    private ItemEffectProcessor itemEffectProcessor;

    @Getter
    private SpawnBlockerManager spawnBlockerManager;


    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new PowerManager(this);
        this.configManager.loadConfig();
        this.spawnTimerManager = new SpawnTimerManager(this);
        this.damageTracker = new DamageTracker(this);
        this.dropHandler = new PowerMobDropHandler(this);

        // Initialize managers
        this.abilityManager = new AbilityManager(this);
        this.spawnBlockerManager = new SpawnBlockerManager(this);
        this.equipmentManager = new EquipmentManager(this);
        this.itemEffectManager = new CustomItemEffectManager(this);
        this.itemEffectProcessor = new ItemEffectProcessor(this);
        this.powerMobManager = new PowerMobManager(this);
        this.guiManager = new GUIManager(this);


        // Register events
        getServer().getPluginManager().registerEvents(new DamageTrackingListener(this), this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ConfigGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemEffectListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnBlockerListener(this), this);


        // Register commands
        PluginCommand command = Objects.requireNonNull(getCommand("powermob"));
        PowerMobCommand commandExecutor = new PowerMobCommand(this);
        command.setExecutor(commandExecutor);
        command.setTabCompleter(commandExecutor);

        // Load abilities and equipment
        this.abilityManager.loadAbilities();
        this.spawnBlockerManager.loadBlockers();
        this.equipmentManager.loadEquipment();

        // Start cleanup task AFTER everything is loaded
        this.spawnBlockerManager.startCleanupTask();
        this.spawnTimerManager.initialize();

        getLogger().info("PowerMobs has been enabled!");
    }

    @Override
    public void onDisable() {
        // Clean up any active power mobs
        if (this.powerMobManager != null) {
            this.powerMobManager.cleanup();
        }

        if (spawnTimerManager != null) {
            spawnTimerManager.shutdown();
        }

        if (this.spawnBlockerManager != null) {
            this.spawnBlockerManager.saveActiveBlockersToDisk();
        }

        getServer().getScheduler().cancelTasks(this);

        getLogger().info("PowerMobs has been disabled!");
    }

    /**
     * Logs a debug message if debug mode is enabled
     *
     * @param message The message to log
     */
    public void debug(String message, String type) {
        if (this.configManager != null && this.configManager.isDebugEnabled()) {
            String prefix = "[DEBUG]";
            switch (type) {
                case "mob_spawning":
                    if (this.configManager.isDebugMobSpawning()) {
                        prefix += "[MOB SPAWNING]";
                    }
                    break;
                case "ui":
                    if (this.configManager.isDebugUI()) {
                        prefix += "[UI]";
                    }
                    break;
                case "mob_combat":
                    if (this.configManager.isDebugMobCombat()) {
                        prefix += "[MOB COMBAT]";
                    }
                    break;
                case "drops":
                    if (this.configManager.isDebugDrops()) {
                        prefix += "[DROPS]";
                    }
                    break;
                case "cleanup":
                    if (this.configManager.isDebugCleanup()) {
                        prefix += "[CLEANUP]";
                    }
                    break;
                case "item_effects":
                    if (this.configManager.isDebugItemEffects()) {
                        prefix += "[ITEM EFFECTS]";
                    }
                    break;
                case "spawn_blockers":
                    if (this.configManager.isDebugSpawnBlockers()) {
                        prefix += "[SPAWN BLOCKERS]";
                    }
                    break;
                case "save_and_load":
                    if (this.configManager.isDebugSaveAndLoad()) {
                        prefix += "[SAVE AND LOAD]";
                    }
                    break;
                default:
                    prefix += "[UNSORTED]";
            }
            getLogger().log(Level.INFO, prefix + " " + message);
        }
    }
}
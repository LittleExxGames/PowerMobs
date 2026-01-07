package com.powermobs.config;

import com.powermobs.PowerMobsPlugin;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the plugin configuration
 */
public class PowerManager {

    private final PowerMobsPlugin plugin;
    @Getter
    private final FileConfigManager mobsConfigManager;
    @Getter
    private final FileConfigManager abilitiesConfigManager;
    @Getter
    private final FileConfigManager itemsConfigManager;
    @Getter
    private final Map<String, PowerMobConfig> powerMobs = new HashMap<>();
    private final Object saveLock = new Object();
    @Getter
    private FileConfiguration config;
    @Getter
    private boolean debugEnabled;
    @Getter
    private boolean debugMobSpawning;
    @Getter
    private boolean debugUI;
    @Getter
    private boolean debugMobCombat;
    @Getter
    private boolean debugDrops;
    @Getter
    private boolean debugCleanup;
    @Getter
    private boolean debugItemEffects;
    @Getter
    private boolean debugSpawnBlockers;
    @Getter
    private boolean debugSaveAndLoad;
    @Getter
    private double spawnChance;
    @Getter
    private boolean spawnAnnouncements;
    @Getter
    private boolean spawnEffect;
    @Getter
    private boolean showNames;
    @Getter
    private boolean spawnTimersEnabled;
    @Getter
    private boolean spawnLocationBased;
    @Getter
    private int locationDistance;
    @Getter
    private boolean spawnerBypass;
    @Getter
    private boolean itemBypass;
    @Getter
    private double playerDamageRequirement;
    @Getter
    private boolean countAllyDamage;
    @Getter
    private RandomMobConfig randomMobConfig;
    private volatile boolean saveInProgress = false;

    /**
     * Creates a new config manager
     *
     * @param plugin The plugin instance
     */
    public PowerManager(PowerMobsPlugin plugin) {
        this.plugin = plugin;
        this.mobsConfigManager = new FileConfigManager(plugin, "mobsconfig.yml");
        this.abilitiesConfigManager = new FileConfigManager(plugin, "abilitiesconfig.yml");
        this.itemsConfigManager = new FileConfigManager(plugin, "itemsconfig.yml");
    }


    /**
     * Loads the configuration from disk
     */
    public void loadConfig() {
        // Save default config if it doesn't exist
        this.plugin.saveDefaultConfig();

        // Reload config from disk
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();

        // Load general settings
        loadGlobalConfig();

        // Load power mobs
        loadPowerMobs();

        // Load random mob config
        loadRandomMobConfig();

        this.plugin.debug("Configuration loaded successfully", "save_and_load");
    }

    private void loadGlobalConfig() {
        ConfigurationSection settings = this.config.getConfigurationSection("settings");
        if (settings != null) {
            this.debugEnabled = settings.getBoolean("debug", false);
            this.debugMobSpawning = settings.getBoolean("debug-mob-spawning", true);
            this.debugUI = settings.getBoolean("debug-ui", true);
            this.debugMobCombat = settings.getBoolean("debug-mob-combat", true);
            this.debugDrops = settings.getBoolean("debug-drops", true);
            this.debugCleanup = settings.getBoolean("debug-cleanup", true);
            this.debugItemEffects = settings.getBoolean("debug-item-effects", true);
            this.debugSpawnBlockers = settings.getBoolean("debug-spawn-blockers", true);
            this.debugSaveAndLoad = settings.getBoolean("debug-save-and-load", true);
            this.spawnChance = settings.getDouble("spawn-chance", 0.1);
            this.spawnAnnouncements = settings.getBoolean("spawn-announcements", true);
            this.spawnEffect = settings.getBoolean("spawn-effect", true);
            this.showNames = settings.getBoolean("show-names", true);

            ConfigurationSection spawnTimerSection = settings.getConfigurationSection("spawn-timers");
            if (spawnTimerSection != null) {
                this.spawnTimersEnabled = spawnTimerSection.getBoolean("enabled", true);
                this.spawnLocationBased = spawnTimerSection.getBoolean("location-based", false);
                this.locationDistance = spawnTimerSection.getInt("distance", 300);
                this.itemBypass = spawnTimerSection.getBoolean("item-bypass", true);
                this.spawnerBypass = spawnTimerSection.getBoolean("spawner-bypass", false);
            } else {
                this.spawnTimersEnabled = true;
                this.spawnLocationBased = false;
                this.locationDistance = 300;
                this.itemBypass = true;
                this.spawnerBypass = false;
            }

            // Load loot drop requirements
            ConfigurationSection lootSection = settings.getConfigurationSection("loot-requirements");
            if (lootSection != null) {
                this.playerDamageRequirement = lootSection.getDouble("player-damage-percentage", 50.0);
                this.countAllyDamage = lootSection.getBoolean("count-ally-damage", true);
            } else {
                // Default values
                this.playerDamageRequirement = 50.0; // 50% by default
                this.countAllyDamage = true; // Include ally damage by default
            }

        }
    }

    /**
     * Loads all power mob configurations
     */
    private void loadPowerMobs() {
        this.powerMobs.clear();

        FileConfiguration mobsConfig = this.mobsConfigManager.getConfig();
        ConfigurationSection mobsSection = mobsConfig.getConfigurationSection("power-mobs");
        if (mobsSection == null) {
            this.plugin.getLogger().warning("No power mobs defined in mobsconfig.yml");
            return;
        }

        for (String key : mobsSection.getKeys(false)) {
            ConfigurationSection mobSection = mobsSection.getConfigurationSection(key);
            if (mobSection != null) {
                try {
                    PowerMobConfig mobConfig = new PowerMobConfig(key, mobSection);
                    this.powerMobs.put(key, mobConfig);
                    this.plugin.debug("Loaded power mob: " + key, "save_and_load");
                } catch (Exception e) {
                    this.plugin.getLogger().severe("Failed to load power mob '" + key + "': " + e.getMessage());
                }
            }
        }

        this.plugin.debug("Loaded " + this.powerMobs.size() + " power mobs", "save_and_load");
    }

    /**
     * Loads the random mob configuration
     */
    private void loadRandomMobConfig() {
        ConfigurationSection randomSection = this.config.getConfigurationSection("random-mobs");
        plugin.debug("=== Random Mob Config Debug ===", "save_and_load");
        plugin.debug("Config file exists: " + plugin.getDataFolder().toPath().resolve("config.yml").toFile().exists(), "save_and_load");
        plugin.debug("Available config sections: " + config.getKeys(false), "save_and_load");
        plugin.debug("Are they null? - Random section: " + (randomSection == null), "save_and_load");
        if (randomSection != null) {
            this.randomMobConfig = new RandomMobConfig(randomSection);
            this.plugin.debug("Loaded random mob configuration", "save_and_load");
        } else {
            this.plugin.getLogger().warning("No random mob configuration found");
            // Create default config
            this.randomMobConfig = new RandomMobConfig();
        }
    }

    /**
     * Gets a power mob configuration by ID
     *
     * @param id The mob ID
     * @return The mob configuration or null if not found
     */
    public PowerMobConfig getPowerMob(String id) {
        return this.powerMobs.get(id);
    }


    /**
     * Reloads the configuration from disk
     */
    public void reloadConfig() {
        this.plugin.reloadConfig();
        this.mobsConfigManager.reloadConfig();
        this.abilitiesConfigManager.reloadConfig();
        this.itemsConfigManager.reloadConfig();
        loadConfig();
    }

    /**
     * Saves a power mob configuration to the mobsconfig.yml file
     *
     * @param id        The mob ID
     * @param mobConfig The mob configuration
     * @return True if saved successfully
     */
    public boolean savePowerMob(String id, Map<String, Object> mobConfig) {
        if (id == null || id.isEmpty() || mobConfig == null) {
            return false;
        }
        if (saveInProgress) {
            this.plugin.debug("Save already in progress, please wait", "save_and_load");
            return false;
        }
        synchronized (saveLock) {
            saveInProgress = true;
        }
        try {
            this.plugin.debug("Saving...", "save_and_load");
            // Save to file
            FileConfiguration mobsConfig = this.mobsConfigManager.getConfig();
            mobsConfig.set("power-mobs." + id, mobConfig);
            this.mobsConfigManager.saveConfig();

            // Try to update memory directly first
            try {
                ConfigurationSection mobSection = mobsConfig.getConfigurationSection("power-mobs." + id);
                if (mobSection != null) {
                    PowerMobConfig newMobConfig = new PowerMobConfig(id, mobSection);
                    this.powerMobs.put(id, newMobConfig);
                    this.plugin.debug("Updated power mob in memory: " + id, "save_and_load");
                    return true;
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to update mob in memory, falling back to full reload: " + e.getMessage());
            }

            // Fallback: reload from disk if direct update fails
            this.mobsConfigManager.reloadConfig();
            loadPowerMobs();

            return true;
        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to save power mob '" + id + "': " + e.getMessage());
            return false;
        } finally {
            saveInProgress = false;
        }
    }

    /**
     * Saves the random mob configuration to the config.yml file
     *
     * @param mobConfig The mob configuration
     * @return True if saved successfully
     */
    public boolean saveRandomMob(Map<String, Object> mobConfig) {
        if (mobConfig == null) {
            return false;
        }
        if (saveInProgress) {
            this.plugin.debug("Save already in progress, please wait", "save_and_load");
            return false;
        }
        synchronized (saveLock) {
            saveInProgress = true;
        }
        try {
            this.config.set("random-mobs", null);
            this.config.createSection("random-mobs", mobConfig);
            this.plugin.saveConfig();
            loadRandomMobConfig();
            return true;
        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to save random mob: " + e.getMessage());
            return false;
        } finally {
            saveInProgress = false;
        }
    }

    /**
     * Removes a power mob configuration from the mobsconfig.yml file
     *
     * @param id The mob ID to remove
     * @return True if removed successfully
     */
    public boolean removePowerMob(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (saveInProgress) {
            this.plugin.debug("Save already in progress, please wait", "save_and_load");
            return false;
        }
        synchronized (saveLock) {
            saveInProgress = true;
        }
        try {
            FileConfiguration mobsConfig = this.mobsConfigManager.getConfig();
            String path = "power-mobs." + id;

            if (!mobsConfig.contains(path)) {
                return false;
            }

            // Remove from file and save
            mobsConfig.set(path, null);
            this.mobsConfigManager.saveConfig();

            // Remove from memory
            this.powerMobs.remove(id);
            return true;
        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to remove power mob '" + id + "': " + e.getMessage());
            return false;
        } finally {
            saveInProgress = false;
        }
    }

    public boolean isSaveInProgress() {
        if (saveInProgress) {
            this.plugin.debug("Save already in progress", "save_and_load");
        }
        return saveInProgress;
    }
}
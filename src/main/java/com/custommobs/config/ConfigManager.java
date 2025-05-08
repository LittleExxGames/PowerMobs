package com.custommobs.config;

import com.custommobs.CustomMobsPlugin;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the plugin configuration
 */
public class ConfigManager {

    private final CustomMobsPlugin plugin;
    
    @Getter
    private FileConfiguration config;
    
    @Getter
    private boolean debugEnabled;
    
    @Getter
    private double spawnChance;
    
    @Getter
    private boolean spawnAnnouncements;
    
    @Getter
    private boolean spawnEffect;
    
    @Getter
    private boolean showNames;

    @Getter
    private double playerDamageRequirement;

    @Getter
    private boolean countAllyDamage;

    @Getter
    private final Map<String, CustomMobConfig> customMobs = new HashMap<>();
    
    @Getter
    private RandomMobConfig randomMobConfig;

    /**
     * Creates a new config manager
     * 
     * @param plugin The plugin instance
     */
    public ConfigManager(CustomMobsPlugin plugin) {
        this.plugin = plugin;
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
        ConfigurationSection settings = this.config.getConfigurationSection("settings");
        if (settings != null) {
            this.debugEnabled = settings.getBoolean("debug", false);
            this.spawnChance = settings.getDouble("spawn-chance", 0.1);
            this.spawnAnnouncements = settings.getBoolean("spawn-announcements", true);
            this.spawnEffect = settings.getBoolean("spawn-effect", true);
            this.showNames = settings.getBoolean("show-names", true);

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
        
        // Load custom mobs
        loadCustomMobs();
        
        // Load random mob config
        loadRandomMobConfig();
        
        this.plugin.debug("Configuration loaded successfully");
    }
    
    /**
     * Loads all custom mob configurations
     */
    private void loadCustomMobs() {
        this.customMobs.clear();
        
        ConfigurationSection mobsSection = this.config.getConfigurationSection("custom-mobs");
        if (mobsSection == null) {
            this.plugin.getLogger().warning("No custom mobs defined in config");
            return;
        }
        
        for (String key : mobsSection.getKeys(false)) {
            ConfigurationSection mobSection = mobsSection.getConfigurationSection(key);
            if (mobSection != null) {
                try {
                    CustomMobConfig mobConfig = new CustomMobConfig(key, mobSection);
                    this.customMobs.put(key, mobConfig);
                    this.plugin.debug("Loaded custom mob: " + key);
                } catch (Exception e) {
                    this.plugin.getLogger().severe("Failed to load custom mob '" + key + "': " + e.getMessage());
                }
            }
        }
        
        this.plugin.debug("Loaded " + this.customMobs.size() + " custom mobs");
    }
    
    /**
     * Loads the random mob configuration
     */
    private void loadRandomMobConfig() {
        ConfigurationSection randomSection = this.config.getConfigurationSection("random-mobs");
        if (randomSection != null) {
            this.randomMobConfig = new RandomMobConfig(randomSection);
            this.plugin.debug("Loaded random mob configuration");
        } else {
            this.plugin.getLogger().warning("No random mob configuration found");
            // Create default config
            this.randomMobConfig = new RandomMobConfig();
        }
    }
    
    /**
     * Gets a custom mob configuration by ID
     * 
     * @param id The mob ID
     * @return The mob configuration or null if not found
     */
    public CustomMobConfig getCustomMob(String id) {
        return this.customMobs.get(id);
    }
    
    /**
     * Reloads the configuration from disk
     */
    public void reloadConfig() {
        this.plugin.reloadConfig();
        loadConfig();
    }
}
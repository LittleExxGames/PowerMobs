package com.powermobs.config;

import com.powermobs.PowerMobsPlugin;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Manages custom configuration files
 */
public class FileConfigManager {

    private final PowerMobsPlugin plugin;
    private final String fileName;
    private final File configFile;
    @Getter
    private FileConfiguration fileConfiguration;

    /**
     * Creates a new file configuration manager
     *
     * @param plugin   The plugin instance
     * @param fileName The file name (including .yml extension)
     */
    public FileConfigManager(PowerMobsPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.configFile = new File(plugin.getDataFolder(), fileName);
        saveDefaultConfig();
        reloadConfig();
    }

    /**
     * Reloads the configuration from disk
     *
     * @return The loaded configuration
     */
    public FileConfiguration reloadConfig() {
        this.fileConfiguration = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            this.fileConfiguration.setDefaults(defaultConfig);
        }

        return this.fileConfiguration;
    }

    /**
     * Gets the configuration
     *
     * @return The file configuration
     */
    public FileConfiguration getConfig() {
        if (this.fileConfiguration == null) {
            reloadConfig();
        }
        return this.fileConfiguration;
    }

    /**
     * Saves the configuration to disk
     */
    public void saveConfig() {
        if (this.fileConfiguration == null || this.configFile == null) {
            return;
        }

        try {
            getConfig().save(this.configFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save config to " + this.configFile + ": " + ex.getMessage());
        }
    }


    /**
     * Saves the default configuration if it doesn't exist
     */
    public void saveDefaultConfig() {
        if (!this.configFile.exists()) {
            plugin.saveResource(fileName, false);
        }
    }
}


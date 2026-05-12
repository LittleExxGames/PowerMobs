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
import java.nio.file.Files;
import java.util.*;

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
        saveConfig(0);
    }

    /**
     * Saves the configuration to disk but spacing between a given depth in a yaml file
     */
    public void saveConfig(int spacingDepth) {
        if (this.fileConfiguration == null || this.configFile == null) {
            return;
        }

        try {
            if (spacingDepth <= 0) {
                getConfig().save(this.configFile);
            } else {
                saveWithSpacing(this.configFile, (YamlConfiguration) getConfig(), spacingDepth);
            }
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

    public static void saveWithSpacing(File file, YamlConfiguration cfg, int spacingDepth) throws IOException {
        String yaml = cfg.saveToString();
        String[] lines = yaml.split("\\R", -1);

        List<String> output = new ArrayList<>();
        Map<Integer, Boolean> seenAtDepth = new HashMap<>();

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                output.add(line);
                continue;
            }

            int leadingSpaces = 0;
            while (leadingSpaces < line.length() && line.charAt(leadingSpaces) == ' ') {
                leadingSpaces++;
            }

            int depth = leadingSpaces / 2;
            boolean isKeyLine = trimmed.endsWith(":") && !trimmed.startsWith("-");

            if (isKeyLine && depth > 0 && depth <= spacingDepth) {
                if (Boolean.TRUE.equals(seenAtDepth.get(depth))) {
                    output.add("");
                }
                seenAtDepth.put(depth, true);
            }

            output.add(line);
        }

        while (!output.isEmpty() && output.get(0).isBlank()) {
            output.remove(0);
        }

        Files.writeString(file.toPath(), String.join(System.lineSeparator(), output), StandardCharsets.UTF_8);
    }
}


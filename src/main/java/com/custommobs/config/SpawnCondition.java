package com.custommobs.config;

import com.custommobs.CustomMobsPlugin;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Conditions for spawning a custom mob
 */
@Getter
public class SpawnCondition {

    private final Set<World.Environment> dimensions;
    private final int minDistance;
    private final int maxDistance;
    private final int minY;
    private final int maxY;
    private final Set<Biome> biomes;
    private final Set<TimeCondition> timeConditions;

    /**
     * Creates a default spawn condition
     */
    public SpawnCondition() {
        this.dimensions = EnumSet.allOf(World.Environment.class);
        this.minDistance = 0;
        this.maxDistance = Integer.MAX_VALUE;
        this.minY = Integer.MIN_VALUE;
        this.maxY = Integer.MAX_VALUE;
        this.biomes = new HashSet<>();
        this.timeConditions = EnumSet.allOf(TimeCondition.class);
    }

    /**
     * Creates a spawn condition from a configuration section
     * 
     * @param section The configuration section
     */
    public SpawnCondition(ConfigurationSection section) {
        // Dimensions
        this.dimensions = EnumSet.noneOf(World.Environment.class);
        List<String> dimStrings = section.getStringList("dimensions");
        if (dimStrings.isEmpty()) {
            // Default to all dimensions if not specified
            this.dimensions.addAll(EnumSet.allOf(World.Environment.class));
        } else {
            for (String dim : dimStrings) {
                try {
                    // Handle the "OVERWORLD" -> "NORMAL" mapping
                    if (dim.equalsIgnoreCase("OVERWORLD")) {
                        this.dimensions.add(World.Environment.NORMAL);
                    } else {
                        World.Environment env = World.Environment.valueOf(dim.toUpperCase());
                        this.dimensions.add(env);
                    }

                } catch (IllegalArgumentException e) {
                    // Skip invalid dimension
                }
            }
        }
        
        // Distance from spawn
        this.minDistance = section.getInt("min-distance", 0);
        this.maxDistance = section.getInt("max-distance", Integer.MAX_VALUE);
        
        // Y level range
        this.minY = section.getInt("min-y", Integer.MIN_VALUE);
        this.maxY = section.getInt("max-y", Integer.MAX_VALUE);
        
        // Biomes
        this.biomes = new HashSet<>();
        List<String> biomeStrings = section.getStringList("biomes");
        for (String biomeStr : biomeStrings) {
            try {
                Biome biome = Biome.valueOf(biomeStr.toUpperCase());
                this.biomes.add(biome);
            } catch (IllegalArgumentException e) {
                // Skip invalid biome
            }
        }
        
        // Time conditions
        this.timeConditions = EnumSet.noneOf(TimeCondition.class);
        List<String> timeStrings = section.getStringList("time");
        if (timeStrings.isEmpty()) {
            // Default to all times if not specified
            this.timeConditions.addAll(EnumSet.allOf(TimeCondition.class));
        } else {
            for (String timeStr : timeStrings) {
                try {
                    TimeCondition time = TimeCondition.valueOf(timeStr.toUpperCase());
                    this.timeConditions.add(time);
                } catch (IllegalArgumentException e) {
                    // Skip invalid time
                }
            }
        }
    }
    
    /**
     * Checks if a location meets the spawn conditions
     * 
     * @param location The location to check
     * @param plugin The plugin instance for debug logging
     * @return True if the location is valid for spawning
     */
    public boolean isValidSpawn(Location location, CustomMobsPlugin plugin) {
        World world = location.getWorld();
        if (world == null) {
            plugin.debug("FAILED spawn condition: World is null");
            return false;
        }
        
        // Check dimension
        if (!this.dimensions.contains(world.getEnvironment())) {
            plugin.debug("FAILED spawn condition: Dimension check failed. Current: " + world.getEnvironment() +
                    ", Allowed: " + this.dimensions);
            return false;
        }
        
        // Check Y level
        if (location.getY() < this.minY || location.getY() > this.maxY) {
            plugin.debug("FAILED spawn condition: Y-level check failed. Current: " + location.getY() +
                    ", Required range: " + this.minY + " to " + this.maxY);
            return false;
        }
        
        // Check distance from spawn
        Location spawnLocation = world.getSpawnLocation();
        double distance = location.distance(spawnLocation);
        if (distance < this.minDistance || distance > this.maxDistance) {
            plugin.debug("FAILED spawn condition: Distance check failed. Current: " + distance +
                    ", Required range: " + this.minDistance + " to " + this.maxDistance);
            return false;
        }
        
        // Check biome
        if (!this.biomes.isEmpty() && !this.biomes.contains(location.getBlock().getBiome())) {
            plugin.debug("FAILED spawn condition: Biome check failed. Current: " + location.getBlock().getBiome() +
                    ", Allowed: " + this.biomes);
            return false;
        }
        
        // Check time
        long time = world.getTime();
        boolean isDay = time >= 0 && time < 13000;
        TimeCondition currentTime = isDay ? TimeCondition.DAY : TimeCondition.NIGHT;
        if (!this.timeConditions.contains(currentTime)) {
            plugin.debug("FAILED spawn condition: Time check failed. Current: " + currentTime +
                    " (time: " + time + "), Allowed: " + this.timeConditions);
            return false;
        }
        plugin.debug("PASSED all spawn conditions.");
        return true;

    }
    
    /**
     * Time condition for spawning
     */
    public enum TimeCondition {
        DAY,
        NIGHT
    }
}
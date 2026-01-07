package com.powermobs.config;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.utils.WeightedRandom;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Conditions for spawning a power mob
 */
@Getter
@Setter
public class SpawnCondition {

    private double spawnChance;
    private int minSpawnDelay;
    private int maxSpawnDelay;
    private int spawnDelayWeight;
    private boolean replaceTypeOnly;
    private int minDespawnTime;
    private int maxDespawnTime;
    private Set<World.Environment> dimensions;
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    private int minY;
    private int maxY;
    private Set<TimeCondition> timeConditions;
    private BiomeGroupManager biomeGroupManager;


    /**
     * Creates a default spawn condition
     */
    public SpawnCondition() {
        this.spawnChance = 0.00;
        this.minSpawnDelay = 500;
        this.maxSpawnDelay = 1200;
        this.spawnDelayWeight = 100;
        this.minDespawnTime = 1800; // 30min
        this.maxDespawnTime = 1800;
        this.replaceTypeOnly = true;
        this.dimensions = EnumSet.allOf(World.Environment.class);
        this.minX = Integer.MIN_VALUE;
        this.maxX = Integer.MAX_VALUE;
        this.minZ = Integer.MIN_VALUE;
        this.maxZ = Integer.MAX_VALUE;
        this.minY = Integer.MIN_VALUE;
        this.maxY = Integer.MAX_VALUE;
        this.timeConditions = EnumSet.allOf(TimeCondition.class);
        this.biomeGroupManager = new BiomeGroupManager();
    }


    public SpawnCondition(SpawnCondition copy) {
        this.spawnChance = copy.getSpawnChance();
        this.minSpawnDelay = copy.getMinSpawnDelay();
        this.maxSpawnDelay = copy.getMaxSpawnDelay();
        this.spawnDelayWeight = copy.getSpawnDelayWeight();
        this.replaceTypeOnly = copy.isReplaceTypeOnly();
        this.minDespawnTime = copy.getMinDespawnTime();
        this.maxDespawnTime = copy.getMaxDespawnTime();
        this.dimensions = new LinkedHashSet<>(copy.getDimensions());
        this.minX = copy.getMinX();
        this.maxX = copy.getMaxX();
        this.minY = copy.getMinY();
        this.maxY = copy.getMaxY();
        this.minZ = copy.getMinZ();
        this.maxZ = copy.getMaxZ();
        this.timeConditions = new LinkedHashSet<>(copy.getTimeConditions());
        this.biomeGroupManager = new BiomeGroupManager(copy.getBiomeGroupManager());

    }

    /**
     * Creates a spawn condition from a configuration section
     *
     * @param section The configuration section
     */
    public SpawnCondition(ConfigurationSection section) {
        this.biomeGroupManager = new BiomeGroupManager();

        // Spawn chance(after global)
        this.spawnChance = section.getDouble("spawn-chance", 0.05);

        // Spawn delay range
        String delayObj = section.getString("spawn-delay");
        if (delayObj != null) {
            String[] parts = (delayObj).split("-");
            this.minSpawnDelay = Integer.parseInt(parts[0]);
            this.maxSpawnDelay = parts.length > 1 ? Integer.parseInt(parts[1]) : this.minSpawnDelay;
        } else {
            this.minSpawnDelay = 500;
            this.maxSpawnDelay = 1200;
        }

        //Spawn delay weight
        this.spawnDelayWeight = section.getInt("spawn-delay-weight", 100);

        //Replace type toggle
        this.replaceTypeOnly = section.getBoolean("replace-type-only", true);

        //Despawn timer
        String despawnObj = section.getString("despawn-time");
        if (despawnObj != null) {
            String[] parts = (despawnObj).split("-");
            this.minDespawnTime = Integer.parseInt(parts[0]);
            this.maxDespawnTime = parts.length > 1 ? Integer.parseInt(parts[1]) : this.minDespawnTime;
        } else {
            this.minDespawnTime = 1800;
            this.maxDespawnTime = 1800;
        }

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

        // X distance
        this.minX = section.getInt("min-x", Integer.MIN_VALUE);
        this.maxX = section.getInt("max-x", Integer.MAX_VALUE);
        // Z distance
        this.minZ = section.getInt("min-z", Integer.MIN_VALUE);
        this.maxZ = section.getInt("max-z", Integer.MAX_VALUE);
        // Y level range
        this.minY = section.getInt("min-y", Integer.MIN_VALUE);
        this.maxY = section.getInt("max-y", Integer.MAX_VALUE);


        // Load custom biome group configurations if present
        ConfigurationSection biomeGroupsSection = section.getConfigurationSection("biome-groups");
        if (biomeGroupsSection != null) {
            Map<String, Object> biomeGroupConfig = new HashMap<>();
            for (String key : biomeGroupsSection.getKeys(false)) {
                biomeGroupConfig.put(key, biomeGroupsSection.getStringList(key));
            }
            this.biomeGroupManager.fromConfigMap(biomeGroupConfig);
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

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        // Spawn chance(after global)
        map.put("spawn-chance", this.spawnChance);

        // Spawn delay
        if (this.minSpawnDelay == this.maxSpawnDelay) {
            map.put("spawn-delay", this.minSpawnDelay + "");
        } else {
            map.put("spawn-delay", this.minSpawnDelay + "-" + this.maxSpawnDelay);
        }

        // Spawn delay weight
        map.put("spawn-delay-weight", this.spawnDelayWeight);

        // Spawn type toggle
        map.put("replace-type-only", this.replaceTypeOnly);

        // Despawn timer
        if (this.minDespawnTime == this.maxDespawnTime) {
            map.put("despawn-time", this.minDespawnTime + "");
        } else {
            map.put("despawn-time", this.minDespawnTime + "-" + this.maxDespawnTime);
        }

        // Convert dimensions to string list (handle NORMAL -> OVERWORLD mapping)
        List<String> dimensionStrings = new ArrayList<>();
        for (World.Environment env : this.dimensions) {
            if (env == World.Environment.NORMAL) {
                dimensionStrings.add("OVERWORLD");
            } else {
                dimensionStrings.add(env.name());
            }
        }
        map.put("dimensions", dimensionStrings);


        // Add coordinate boundaries only if they're not infinite
        if (this.minX != Integer.MIN_VALUE) {
            map.put("min-x", this.minX);
        } else {
            map.put("min-x", "infinite");
        }
        if (this.maxX != Integer.MAX_VALUE) {
            map.put("max-x", this.maxX);
        } else {
            map.put("max-x", "infinite");
        }
        if (this.minZ != Integer.MIN_VALUE) {
            map.put("min-z", this.minZ);
        } else {
            map.put("min-z", "infinite");
        }
        if (this.maxZ != Integer.MAX_VALUE) {
            map.put("max-z", this.maxZ);
        } else {
            map.put("max-z", "infinite");
        }
        if (this.minY != Integer.MIN_VALUE) {
            map.put("min-y", this.minY);
        } else {
            map.put("min-y", "infinite");
        }
        if (this.maxY != Integer.MAX_VALUE) {
            map.put("max-y", this.maxY);
        } else {
            map.put("max-y", "infinite");
        }

        // Save custom biome group configurations
        Map<String, Object> biomeGroupConfig = this.biomeGroupManager.toConfigMap();
        if (!biomeGroupConfig.isEmpty()) {
            map.put("biome-groups", biomeGroupConfig);
        }

        // Convert time conditions to string list (only if not all times allowed)
        if (!this.timeConditions.containsAll(EnumSet.allOf(TimeCondition.class))) {
            List<String> timeStrings = new ArrayList<>();
            for (TimeCondition time : this.timeConditions) {
                timeStrings.add(time.name());
            }
            map.put("time", timeStrings);
        }

        return map;
    }


    /**
     * Checks if a location meets the spawn conditions
     *
     * @param location The location to check
     * @param plugin   The plugin instance for debug logging
     * @return True if the location is valid for spawning
     */
    public boolean isValidSpawn(Location location, PowerMobsPlugin plugin) {
        World world = location.getWorld();
        if (world == null) {
            plugin.debug("FAILED spawn condition: World is null", "mob_spawning");
            return false;
        }

        // Check dimension
        if (!this.dimensions.contains(world.getEnvironment())) {
            plugin.debug("FAILED spawn condition: Dimension check failed. Current: " + world.getEnvironment() +
                    ", Allowed: " + this.dimensions, "mob_spawning");
            return false;
        }

        // Check coordinates
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int y = location.getBlockY();

        if ((x < this.minX || x > this.maxX) ||
                (z < this.minZ || z > this.maxZ) ||
                (y < this.minY || y > this.maxY)) {
            plugin.debug(String.format("FAILED spawn condition: Spawn range check failed. Current: %d x, %d y, %d z \n " +
                            "Needed: %d - %d x, %d - %d y, %d - %d z",
                    x, y, z, this.minX, this.maxX, this.minZ, this.maxZ, this.minY, this.maxY), "mob_spawning");
            return false;
        }

        // Check biome conditions
        Biome currentBiome = location.getBlock().getBiome();

        // If no biome restrictions are set, allow all biomes

        boolean hasBiomeRestrictions = !biomeGroupManager.getAllActiveBiomes().isEmpty();
        if (!hasBiomeRestrictions) {
            plugin.debug("No biome restrictions, allowing spawn", "mob_spawning");
        } else {
            // Check biome groups
            Set<Biome> allowedBiomes = biomeGroupManager.getAllActiveBiomes();
            boolean biomeMatches = allowedBiomes.contains(currentBiome);

            if (!biomeMatches) {
                plugin.debug("Spawn rejected: biome " + currentBiome + " not allowed", "mob_spawning");
                return false;
            }
        }


        // Check time
        long time = world.getTime();
        boolean isDay = time >= 0 && time < 13000;
        TimeCondition currentTime = isDay ? TimeCondition.DAY : TimeCondition.NIGHT;
        if (!this.timeConditions.contains(currentTime)) {
            plugin.debug("FAILED spawn condition: Time check failed. Current: " + currentTime +
                    " (time: " + time + "), Allowed: " + this.timeConditions, "mob_spawning");
            return false;
        }
        return true;

    }

    public int getActualSpawnDelay() {
        return WeightedRandom.getWeightedRandom(this.spawnDelayWeight, this.minSpawnDelay, this.spawnDelayWeight);
    }

    public int getActualDespawnTime() {
        Random random = new Random();
        return random.nextInt(minDespawnTime, maxDespawnTime + 1);
    }

    /**
     * Time condition for spawning
     */
    public enum TimeCondition {
        DAY,
        NIGHT
    }
}
package com.custommobs.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * Configuration for randomly generated mobs
 */
@Getter
public class RandomMobConfig {

    private final boolean enabled;
    private final double chance;
    private final Set<EntityType> allowedTypes;
    private final int minAbilities;
    private final int maxAbilities;
    private final List<String> possibleAbilities;
    private final double minHealthMultiplier;
    private final double maxHealthMultiplier;
    private final double minDamageMultiplier;
    private final double maxDamageMultiplier;
    private final double minSpeedMultiplier;
    private final double maxSpeedMultiplier;
    private final double weaponChance;
    private final double helmetChance;
    private final double chestplateChance;
    private final double leggingsChance;
    private final double bootsChance;
    private final List<String> possibleWeapons;
    private final List<String> possibleHelmets;
    private final List<String> possibleChestplates;
    private final List<String> possibleLeggings;
    private final List<String> possibleBoots;
    private final List<String> namePrefixes;
    private final List<String> nameSuffixes;
    private final SpawnCondition spawnCondition;
    private final int minDrops;
    private final int maxDrops;
    private final int dropWeight;
    private final List<CustomMobConfig.CustomDropConfig> drops;


    /**
     * Creates a default random mob configuration
     */
    public RandomMobConfig() {
        this.enabled = false;
        this.chance = 0.05;
        this.allowedTypes = EnumSet.noneOf(EntityType.class);
        this.minAbilities = 1;
        this.maxAbilities = 1;
        this.possibleAbilities = new ArrayList<>();
        this.minHealthMultiplier = 1.5;
        this.maxHealthMultiplier = 2.0;
        this.minDamageMultiplier = 1.2;
        this.maxDamageMultiplier = 1.5;
        this.minSpeedMultiplier = 1.0;
        this.maxSpeedMultiplier = 1.2;
        this.weaponChance = 0.5;
        this.helmetChance = 0.5;
        this.chestplateChance = 0.5;
        this.leggingsChance = 0.5;
        this.bootsChance = 0.5;
        this.possibleWeapons = new ArrayList<>();
        this.possibleHelmets = new ArrayList<>();
        this.possibleChestplates = new ArrayList<>();
        this.possibleLeggings = new ArrayList<>();
        this.possibleBoots = new ArrayList<>();
        this.namePrefixes = new ArrayList<>();
        this.nameSuffixes = new ArrayList<>();
        this.spawnCondition = new SpawnCondition();
        this.minDrops = 0;
        this.maxDrops = 2;
        this.dropWeight = 100;
        this.drops = new ArrayList<>();

    }

    /**
     * Creates a random mob configuration from a configuration section
     * 
     * @param section The configuration section
     */
    public RandomMobConfig(ConfigurationSection section) {
        this.enabled = section.getBoolean("enabled", false);
        this.chance = section.getDouble("chance", 0.05);
        
        // Load allowed entity types
        this.allowedTypes = EnumSet.noneOf(EntityType.class);
        List<String> typeStrings = section.getStringList("allowed-types");
        for (String typeStr : typeStrings) {
            try {
                EntityType type = EntityType.valueOf(typeStr.toUpperCase());
                if (type.isAlive() && type.isSpawnable()) {
                    this.allowedTypes.add(type);
                }
            } catch (IllegalArgumentException e) {
                // Skip invalid entity type
            }
        }
        
        // Load ability configuration
        ConfigurationSection abilitiesSection = section.getConfigurationSection("abilities");
        if (abilitiesSection != null) {
            this.minAbilities = abilitiesSection.getInt("min", 1);
            this.maxAbilities = abilitiesSection.getInt("max", 1);
            this.possibleAbilities = abilitiesSection.getStringList("possible");
        } else {
            this.minAbilities = 1;
            this.maxAbilities = 1;
            this.possibleAbilities = new ArrayList<>();
        }
        
        // Load stats configuration
        ConfigurationSection statsSection = section.getConfigurationSection("stats");
        if (statsSection != null) {
            // Parse multiplier ranges
            String healthMultiplier = statsSection.getString("health-multiplier", "1.5-3.0");
            String[] healthParts = healthMultiplier.split("-");
            this.minHealthMultiplier = Double.parseDouble(healthParts[0]);
            this.maxHealthMultiplier = healthParts.length > 1 ? Double.parseDouble(healthParts[1]) : this.minHealthMultiplier;
            
            String damageMultiplier = statsSection.getString("damage-multiplier", "1.2-2.0");
            String[] damageParts = damageMultiplier.split("-");
            this.minDamageMultiplier = Double.parseDouble(damageParts[0]);
            this.maxDamageMultiplier = damageParts.length > 1 ? Double.parseDouble(damageParts[1]) : this.minDamageMultiplier;
            
            String speedMultiplier = statsSection.getString("speed-multiplier", "1.0-1.5");
            String[] speedParts = speedMultiplier.split("-");
            this.minSpeedMultiplier = Double.parseDouble(speedParts[0]);
            this.maxSpeedMultiplier = speedParts.length > 1 ? Double.parseDouble(speedParts[1]) : this.minSpeedMultiplier;
        } else {
            this.minHealthMultiplier = 1.5;
            this.maxHealthMultiplier = 3.0;
            this.minDamageMultiplier = 1.2;
            this.maxDamageMultiplier = 2.0;
            this.minSpeedMultiplier = 1.0;
            this.maxSpeedMultiplier = 1.5;
        }
        
        // Load equipment configuration
        ConfigurationSection equipmentSection = section.getConfigurationSection("equipment");
        if (equipmentSection != null) {
            this.weaponChance = equipmentSection.getDouble("weapon-chance", 0.7);
            this.helmetChance = equipmentSection.getDouble("helmet-chance", 0.5);
            this.chestplateChance = equipmentSection.getDouble("chestplate-chance", 0.4);
            this.leggingsChance = equipmentSection.getDouble("leggings-chance", 0.4);
            this.bootsChance = equipmentSection.getDouble("boots-chance", 0.5);
            
            this.possibleWeapons = equipmentSection.getStringList("possible-weapons");
            this.possibleHelmets = equipmentSection.getStringList("possible-helmets");
            this.possibleChestplates = equipmentSection.getStringList("possible-chestplates");
            this.possibleLeggings = equipmentSection.getStringList("possible-leggings");
            this.possibleBoots = equipmentSection.getStringList("possible-boots");
        } else {
            this.weaponChance = 0.7;
            this.helmetChance = 0.5;
            this.chestplateChance = 0.4;
            this.leggingsChance = 0.4;
            this.bootsChance = 0.5;
            this.possibleWeapons = new ArrayList<>();
            this.possibleHelmets = new ArrayList<>();
            this.possibleChestplates = new ArrayList<>();
            this.possibleLeggings = new ArrayList<>();
            this.possibleBoots = new ArrayList<>();
        }

        // Drop count range (can be a range like "0-2" or a single number)
        String amountObj = section.getString("drop-count");
        if (amountObj != null) {
            String[] parts = ((String) amountObj).split("-");
            this.minDrops = Integer.parseInt(parts[0]);
            this.maxDrops = parts.length > 1 ? Integer.parseInt(parts[1]) : this.minDrops;
        } else {
            this.minDrops = 0;
            this.maxDrops = 2;
        }

        // Drop weight
        this.dropWeight = section.getInt("drop-weight", 100);


        // Drops
        this.drops = new ArrayList<>();
        List<Map<?, ?>> dropsList = section.getMapList("drops");
        for (Map<?, ?> dropMap : dropsList) {
            @SuppressWarnings("unchecked")
            Map<String, Object> castedDropMap = (Map<String, Object>) dropMap;
            CustomMobConfig.CustomDropConfig drop = new CustomMobConfig.CustomDropConfig(castedDropMap);
            this.drops.add(drop);
        }

        // Load name generation
        ConfigurationSection namesSection = section.getConfigurationSection("names");
        if (namesSection != null) {
            this.namePrefixes = namesSection.getStringList("prefixes");
            this.nameSuffixes = namesSection.getStringList("suffixes");
        } else {
            this.namePrefixes = new ArrayList<>();
            this.nameSuffixes = new ArrayList<>();
        }

        // Load spawn conditions
        ConfigurationSection conditionSection = section.getConfigurationSection("spawn-conditions");
        if (conditionSection != null) {
            this.spawnCondition = new SpawnCondition(conditionSection);
        } else {
            this.spawnCondition = new SpawnCondition();
        }

    }

    public int getActualDropCount() {
        return com.custommobs.util.WeightedRandom.getWeightedRandom(minDrops, maxDrops, dropWeight);
    }

    /**
     * Checks if a mob type is allowed for random enhancements
     * 
     * @param type The entity type
     * @return True if the type is allowed
     */
    public boolean isAllowedType(EntityType type) {
        return this.allowedTypes.contains(type);
    }
}
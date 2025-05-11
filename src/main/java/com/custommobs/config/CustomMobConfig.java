package com.custommobs.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import com.custommobs.util.WeightedRandom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a custom mob
 */
@Getter
public class CustomMobConfig implements ICustomMobConfig {

    private final String id;
    private final EntityType entityType;
    private final String name;
    private final double health;
    private final double damageMultiplier;
    private final double speedMultiplier;
    private final Map<String, Double> attributes;
    private final List<String> abilities;
    private final Map<String, String> equipment;
    private final int minDrops;
    private final int maxDrops;
    private final int dropWeight;
    private final List<CustomDropConfig> drops;
    private final SpawnCondition spawnCondition;

    /**
     * Creates a new custom mob configuration
     * 
     * @param id The mob ID
     * @param section The configuration section
     * @throws IllegalArgumentException If the configuration is invalid
     */
    public CustomMobConfig(String id, ConfigurationSection section) {
        this.id = id;
        
        // Basic properties
        String typeString = section.getString("type");
        if (typeString == null) {
            throw new IllegalArgumentException("Mob type is required");
        }
        
        try {
            this.entityType = EntityType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid entity type: " + typeString);
        }
        
        this.name = section.getString("name", "Custom " + entityType.toString());
        this.health = section.getDouble("health", -1); // -1 means use default
        this.damageMultiplier = section.getDouble("damage-multiplier", 1.0);
        this.speedMultiplier = section.getDouble("speed-multiplier", 1.0);
        
        // Attributes
        this.attributes = new HashMap<>();
        ConfigurationSection attrSection = section.getConfigurationSection("attributes");
        if (attrSection != null) {
            for (String key : attrSection.getKeys(false)) {
                this.attributes.put(key, attrSection.getDouble(key));
            }
        }
        
        // Abilities
        this.abilities = section.getStringList("abilities");
        
        // Equipment
        this.equipment = new HashMap<>();
        ConfigurationSection equipSection = section.getConfigurationSection("equipment");
        if (equipSection != null) {
            for (String slot : equipSection.getKeys(false)) {
                this.equipment.put(slot, equipSection.getString(slot));
            }
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
            CustomDropConfig drop = new CustomDropConfig(castedDropMap);
            this.drops.add(drop);
        }
        
        // Spawn conditions
        ConfigurationSection conditionSection = section.getConfigurationSection("spawn-conditions");
        if (conditionSection != null) {
            this.spawnCondition = new SpawnCondition(conditionSection);
        } else {
            this.spawnCondition = new SpawnCondition(); // Default conditions
        }
    }

    /**
     * Gets the actual drop count using the weighted distribution
     *
     * @return The selected amount
     */
    public int getActualDropCount() {
        return WeightedRandom.getWeightedRandom(minDrops, maxDrops, dropWeight);
    }
    
    /**
     * Configuration for a custom drop
     */
    @Getter
    public static class CustomDropConfig {
        private final String item;
        private final double chance;
        private final int minAmount;
        private final int maxAmount;
        private final int amountWeight;
        private final List<EnchantmentConfig> enchantments;
        
        /**
         * Creates a new custom drop configuration
         * 
         * @param map The configuration map
         */

        public CustomDropConfig(Map<String, Object> map) {
            this.item = (String) map.get("item");
            this.chance = ((Number) map.getOrDefault("chance", 1.0)).doubleValue();

            // Parse amount (can be a range like "1-3" or a single number)
            Object amountObj = map.getOrDefault("amount", 1);
            if (amountObj instanceof String) {
                String[] parts = ((String) amountObj).split("-");
                this.minAmount = Integer.parseInt(parts[0]);
                this.maxAmount = parts.length > 1 ? Integer.parseInt(parts[1]) : this.minAmount;
            } else {
                this.minAmount = ((Number) amountObj).intValue();
                this.maxAmount = this.minAmount;
            }

            // Parse amount weight
            this.amountWeight = map.containsKey("amount-weight") ?
                    ((Number) map.get("amount-weight")).intValue() : 100;

            // Parse enchantments
            this.enchantments = new ArrayList<>();
            List<Map<String, Object>> enchList = (List<Map<String, Object>>) map.getOrDefault("enchantments", new ArrayList<>());
            for (Map<String, Object> enchMap : enchList) {
                String type = (String) enchMap.get("type");

                // Parse level (can be a range like "1-3" or a single number)
                Object levelObj = enchMap.getOrDefault("level", 1);
                int minLevel, maxLevel;
                if (levelObj instanceof String) {
                    String[] parts = ((String) levelObj).split("-");
                    minLevel = Integer.parseInt(parts[0]);
                    maxLevel = parts.length > 1 ? Integer.parseInt(parts[1]) : minLevel;
                } else {
                    minLevel = ((Number) levelObj).intValue();
                    maxLevel = minLevel;
                }

                // Parse weight
                int weight = enchMap.containsKey("weight") ?
                        ((Number) enchMap.get("weight")).intValue() : 100;

                this.enchantments.add(new EnchantmentConfig(type, minLevel, maxLevel, weight));

            }
        }

        /**
         * Gets the actual amount for this drop using the weighted distribution
         *
         * @return The selected amount
         */
        public int getActualAmount() {
            return WeightedRandom.getWeightedRandom(minAmount, maxAmount, amountWeight);
        }

    }

    /**
     * Configuration for an enchantment
     */
    @Getter
    public static class EnchantmentConfig {
        private final String type;
        private final int minLevel;
        private final int maxLevel;
        private final int weight;

        /**
         * Creates a new enchantment configuration
         *
         * @param type The enchantment type
         * @param level The enchantment level or min level if range is used
         */
        public EnchantmentConfig(String type, int level) {
            this.type = type;
            this.minLevel = level;
            this.maxLevel = level;
            this.weight = 100; // Default weight (equal distribution)
        }

        /**
         * Creates a new enchantment configuration with level range and weight
         *
         * @param type The enchantment type
         * @param minLevel The minimum enchantment level
         * @param maxLevel The maximum enchantment level
         * @param weight The weight value (1-200)
         */
        public EnchantmentConfig(String type, int minLevel, int maxLevel, int weight) {
            this.type = type;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.weight = weight;
        }

        /**
         * Gets the actual level for this enchantment using the weighted distribution
         *
         * @return The selected enchantment level
         */
        public int getActualLevel() {
            return WeightedRandom.getWeightedRandom(minLevel, maxLevel, weight);
        }
    }

}
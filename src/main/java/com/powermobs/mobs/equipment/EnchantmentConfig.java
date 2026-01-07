package com.powermobs.mobs.equipment;

import com.powermobs.utils.WeightedRandom;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for an enchantment
 */
@Getter
public class EnchantmentConfig {
    private final String type;
    private final int minLevel;
    private final int maxLevel;
    private final int weight;

    /**
     * Creates a new enchantment configuration
     *
     * @param type  The enchantment type
     * @param level The enchantment level or min level if range is used
     */
    public EnchantmentConfig(String type, int level) {
        this.type = type.toUpperCase();
        this.minLevel = level;
        this.maxLevel = level;
        this.weight = 100; // Default weight (equal distribution)
    }

    /**
     * Creates a new enchantment configuration with level range and weight
     *
     * @param type     The enchantment type
     * @param minLevel The minimum enchantment level
     * @param maxLevel The maximum enchantment level
     * @param weight   The weight value (1-200)
     */
    public EnchantmentConfig(String type, int minLevel, int maxLevel, int weight) {
        this.type = type.toUpperCase();
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.weight = weight;
    }

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("type", this.type);
        if (this.minLevel == this.maxLevel) {
            map.put("level", this.minLevel);
        } else {
            map.put("level", this.minLevel + "-" + this.maxLevel);
        }
        map.put("weight", this.weight);
        return map;
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

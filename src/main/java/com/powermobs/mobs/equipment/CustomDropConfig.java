package com.powermobs.mobs.equipment;

import com.powermobs.utils.WeightedRandom;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a custom drop
 */
@Getter
@Setter
public class CustomDropConfig {
    private String item;
    private double chance;
    private int minAmount;
    private int maxAmount;
    private int amountWeight;
    private List<EnchantmentConfig> enchantments;

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

    public CustomDropConfig(String item, double chance, int minAmount, int maxAmount, int amountWeight, List<EnchantmentConfig> enchantments) {
        this.item = item;
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.amountWeight = amountWeight;
        this.enchantments = new ArrayList<>(enchantments);
    }

    public CustomDropConfig(CustomDropConfig newConfig) {
        this.item = newConfig.getItem();
        this.chance = newConfig.getChance();
        this.minAmount = newConfig.getMinAmount();
        this.maxAmount = newConfig.getMaxAmount();
        this.amountWeight = newConfig.getAmountWeight();
        this.enchantments = new ArrayList<>(newConfig.getEnchantments());
    }


    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("item", this.item);

        map.put("chance", this.chance);

        if (this.minAmount == this.maxAmount) {
            map.put("amount", this.minAmount);
        } else {
            map.put("amount", this.minAmount + "-" + this.maxAmount);
        }

        map.put("amount-weight", this.amountWeight);

        if (!this.enchantments.isEmpty()) {
            List<Map<String, Object>> enchantmentList = new ArrayList<>();
            for (EnchantmentConfig enchantment : this.enchantments) {
                enchantmentList.add(enchantment.toConfigMap());
            }
            map.put("enchantments", enchantmentList);
        }
        return map;
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

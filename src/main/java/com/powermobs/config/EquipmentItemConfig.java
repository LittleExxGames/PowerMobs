package com.powermobs.config;

import com.powermobs.mobs.equipment.EnchantmentConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class EquipmentItemConfig {
    private String item;
    private List<EnchantmentConfig> enchantments;

    /**
     * Creates a new custom drop configuration
     *
     * @param map The configuration map
     */

    public EquipmentItemConfig(Map<String, Object> map) {
        this.item = (String) map.get("item");
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

    public EquipmentItemConfig(EquipmentItemConfig item) {
        this.item = item.item;
        this.enchantments = new ArrayList<>(item.enchantments);
    }

    public EquipmentItemConfig(String item, List<EnchantmentConfig> enchantments) {
        this.item = item;
        this.enchantments = new ArrayList<>(enchantments);
    }

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("item", this.item);

        if (!this.enchantments.isEmpty()) {
            List<Map<String, Object>> enchantmentList = new ArrayList<>();
            for (EnchantmentConfig enchantment : this.enchantments) {
                enchantmentList.add(enchantment.toConfigMap());
            }
            map.put("enchantments", enchantmentList);
        }
        return map;
    }

}

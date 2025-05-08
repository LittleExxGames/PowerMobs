package com.custommobs.mobs.equipment;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import de.tr7zw.nbtapi.NBTItem;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages custom equipment for mobs
 */
@RequiredArgsConstructor
public class EquipmentManager {

    private final CustomMobsPlugin plugin;
    private final Map<String, ItemStack> weapons = new HashMap<>();
    private final Map<String, ItemStack> armor = new HashMap<>();
    
    /**
     * Loads all equipment from the configuration
     */
    public void loadEquipment() {
        // Load weapons
        ConfigurationSection weaponsSection = this.plugin.getConfigManager().getConfig()
            .getConfigurationSection("equipment.weapons");
            
        if (weaponsSection != null) {
            for (String id : weaponsSection.getKeys(false)) {
                ConfigurationSection section = weaponsSection.getConfigurationSection(id);
                if (section != null) {
                    try {
                        ItemStack item = loadEquipmentItem(section);
                        this.weapons.put(id, item);
                        this.plugin.debug("Loaded weapon: " + id);
                    } catch (Exception e) {
                        this.plugin.getLogger().severe("Failed to load weapon '" + id + "': " + e.getMessage());
                    }
                }
            }
        }
        
        // Load armor
        ConfigurationSection armorSection = this.plugin.getConfigManager().getConfig()
            .getConfigurationSection("equipment.armor");
            
        if (armorSection != null) {
            for (String id : armorSection.getKeys(false)) {
                ConfigurationSection section = armorSection.getConfigurationSection(id);
                if (section != null) {
                    try {
                        ItemStack item = loadEquipmentItem(section);
                        this.armor.put(id, item);
                        this.plugin.debug("Loaded armor: " + id);
                    } catch (Exception e) {
                        this.plugin.getLogger().severe("Failed to load armor '" + id + "': " + e.getMessage());
                    }
                }
            }
        }
        
        this.plugin.debug("Loaded " + this.weapons.size() + " weapons and " + this.armor.size() + " armor pieces");
    }
    
    /**
     * Loads an equipment item from a configuration section
     * 
     * @param section The configuration section
     * @return The loaded item
     */
    private ItemStack loadEquipmentItem(ConfigurationSection section) {
        String materialName = section.getString("material");
        if (materialName == null) {
            throw new IllegalArgumentException("Material is required");
        }
        
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid material: " + materialName);
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set name
            String name = section.getString("name");
            if (name != null) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
            }
            
            // Set lore
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            
            // Set custom model data
            if (section.contains("custom-model-data")) {
                meta.setCustomModelData(section.getInt("custom-model-data"));
            }
            
            // Set unbreakable
            if (section.getBoolean("unbreakable", false)) {
                meta.setUnbreakable(true);
            }
            
            // Add glow effect
            if (section.getBoolean("glow", false)) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            // Set attributes
            List<Map<?, ?>> attributesList = section.getMapList("attributes");
            for (Map<?, ?> attrMap : attributesList) {
                Map<String, Object> castedAttrMap = (Map<String, Object>) attrMap;
                String typeName = (String) castedAttrMap.get("type");
                double amount = ((Number) castedAttrMap.get("amount")).doubleValue();
                String operationName = (String) castedAttrMap.getOrDefault("operation", "ADD_NUMBER");


                AttributeModifier.Operation operation;
                try {
                    operation = AttributeModifier.Operation.valueOf(operationName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    operation = AttributeModifier.Operation.ADD_NUMBER;
                }

                try {
                    // Replace the deprecated Attribute.valueOf() with Registry
                    Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(typeName.toLowerCase()));
                    if (attribute != null) {
                        meta.addAttributeModifier(
                                attribute,
                                new AttributeModifier(
                                        new NamespacedKey(plugin, typeName.toLowerCase()),
                                        amount,
                                        operation
                                )
                        );
                    } else {
                        this.plugin.getLogger().warning("Invalid attribute: " + typeName);
                    }
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Invalid attribute: " + typeName);
                }
            }
            
            item.setItemMeta(meta);
        }
        
        // Add enchantments
        List<Map<?, ?>> enchantmentsList = section.getMapList("enchantments");
        for (Map<?, ?> enchMap : enchantmentsList) {
            @SuppressWarnings("unchecked")
            Map<String, Object> castedEnchMap = (Map<String, Object>) enchMap;
            String typeName = (String) castedEnchMap.get("type");
            int level = ((Number) castedEnchMap.getOrDefault("level", 1)).intValue();

            
            try {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(typeName.toLowerCase()));
                if (enchantment != null) {
                    item.addUnsafeEnchantment(enchantment, level);
                } else {
                    this.plugin.getLogger().warning("Invalid enchantment: " + typeName);
                }
            } catch (IllegalArgumentException e) {
                this.plugin.getLogger().warning("Invalid enchantment: " + typeName);
            }
        }
        
        // Add NBT data
        ConfigurationSection nbtSection = section.getConfigurationSection("nbt-data");
        if (nbtSection != null) {
            NBTItem nbtItem = new NBTItem(item);
            
            for (String key : nbtSection.getKeys(false)) {
                if (nbtSection.isBoolean(key)) {
                    nbtItem.setBoolean(key, nbtSection.getBoolean(key));
                } else if (nbtSection.isInt(key)) {
                    nbtItem.setInteger(key, nbtSection.getInt(key));
                } else if (nbtSection.isDouble(key)) {
                    nbtItem.setDouble(key, nbtSection.getDouble(key));
                } else if (nbtSection.isString(key)) {
                    nbtItem.setString(key, nbtSection.getString(key));
                }
            }
            
            item = nbtItem.getItem();
        }
        
        return item;
    }
    
    /**
     * Applies equipment to a custom mob
     * 
     * @param customMob The custom mob
     * @param slot The equipment slot
     * @param equipmentId The equipment ID
     */
    public void applyEquipment(CustomMob customMob, String slot, String equipmentId) {
        LivingEntity entity = customMob.getEntity();
        
        // Get the equipment
        ItemStack item = getEquipment(slot, equipmentId);
        if (item == null) {
            this.plugin.getLogger().warning("Invalid equipment: " + equipmentId + " for slot " + slot);
            return;
        }
        
        // Apply to the entity
        switch (slot.toLowerCase()) {
            case "helmet":
                entity.getEquipment().setHelmet(item);
                entity.getEquipment().setHelmetDropChance(0.0f);
                break;
            case "chestplate":
                entity.getEquipment().setChestplate(item);
                entity.getEquipment().setChestplateDropChance(0.0f);
                break;
            case "leggings":
                entity.getEquipment().setLeggings(item);
                entity.getEquipment().setLeggingsDropChance(0.0f);
                break;
            case "boots":
                entity.getEquipment().setBoots(item);
                entity.getEquipment().setBootsDropChance(0.0f);
                break;
            case "mainhand":
                entity.getEquipment().setItemInMainHand(item);
                entity.getEquipment().setItemInMainHandDropChance(0.0f);
                break;
            case "offhand":
                entity.getEquipment().setItemInOffHand(item);
                entity.getEquipment().setItemInOffHandDropChance(0.0f);
                break;
            default:
                this.plugin.getLogger().warning("Invalid equipment slot: " + slot);
                break;
        }
    }
    
    /**
     * Gets equipment by ID
     * 
     * @param slot The equipment slot
     * @param id The equipment ID
     * @return The equipment item, or null if not found
     */
    private ItemStack getEquipment(String slot, String id) {
        switch (slot.toLowerCase()) {
            case "helmet":
            case "chestplate":
            case "leggings":
            case "boots":
                return this.armor.get(id);
            case "mainhand":
            case "offhand":
                return this.weapons.get(id);
            default:
                return null;
        }
    }
}
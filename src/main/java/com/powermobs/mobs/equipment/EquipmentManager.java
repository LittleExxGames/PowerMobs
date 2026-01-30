package com.powermobs.mobs.equipment;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.config.EquipmentItemConfig;
import com.powermobs.config.FileConfigManager;
import com.powermobs.config.SpawnBlockerManager;
import com.powermobs.mobs.PowerMob;
import com.powermobs.utils.WeightedRandom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages custom equipment for mobs
 */
@RequiredArgsConstructor
public class EquipmentManager {

    private final PowerMobsPlugin plugin;
    @Getter
    private final Map<String, ItemStack> weapons = new HashMap<>();
    @Getter
    private final Map<String, ItemStack> armor = new HashMap<>();
    @Getter
    private final Map<String, ItemStack> uniques = new HashMap<>();
    private final FileConfigManager equipmentConfigManager;

    public EquipmentManager(PowerMobsPlugin plugin) {
        this.plugin = plugin;
        this.equipmentConfigManager = new FileConfigManager(plugin, "itemsconfig.yml");
    }

    /**
     * Loads all equipment from the configuration
     */
    public void loadEquipment() {
        FileConfiguration config = equipmentConfigManager.getConfig();
        ConfigurationSection weaponsSection = config.getConfigurationSection("equipment.weapons");

        // Load weapons
        if (weaponsSection != null) {
            for (String id : weaponsSection.getKeys(false)) {
                ConfigurationSection section = weaponsSection.getConfigurationSection(id);
                if (section != null) {
                    try {
                        ItemStack item = loadEquipmentItem(section);
                        item = setCustomItemId(id, item);
                        this.weapons.put(id, item);
                        this.plugin.debug("Loaded weapon: " + id, "save_and_load");
                    } catch (Exception e) {
                        this.plugin.getLogger().severe("Failed to load weapon '" + id + "': " + e.getMessage());
                    }
                }
            }
        }

        // Load armor
        ConfigurationSection armorSection = config.getConfigurationSection("equipment.armor");

        if (armorSection != null) {
            for (String id : armorSection.getKeys(false)) {
                ConfigurationSection section = armorSection.getConfigurationSection(id);
                if (section != null) {
                    try {
                        ItemStack item = loadEquipmentItem(section);
                        item = setCustomItemId(id, item);
                        this.armor.put(id, item);
                        this.plugin.debug("Loaded armor: " + id, "save_and_load");
                    } catch (Exception e) {
                        this.plugin.getLogger().severe("Failed to load armor '" + id + "': " + e.getMessage());
                    }
                }
            }
        }

        // Load unique items
        ConfigurationSection itemSection = config.getConfigurationSection("equipment.uniques");
        if (itemSection != null) {
            for (String id : itemSection.getKeys(false)) {
                ConfigurationSection section = itemSection.getConfigurationSection(id);
                if (section != null) {
                    try {
                        ItemStack item = loadEquipmentItem(section);
                        item = setCustomItemId(id, item);
                        this.uniques.put(id, item);
                        this.plugin.debug("Loaded unique item: " + id, "save_and_load");
                    } catch (Exception e) {
                        this.plugin.getLogger().severe("Failed to load unique item '" + id + "': " + e.getMessage());
                    }
                }
            }
        }

        // Load spawn blockers as items
        loadSpawnBlockerItems();

        this.plugin.debug("Loaded " + this.weapons.size() + " weapons, " + this.armor.size() +
                " armor pieces, " + this.uniques.size() + " unique items, and " +
                getSpawnBlockerItems().size() + " spawn blocker items.", "save_and_load");
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
                        meta.addEnchant(enchantment, level, true);
                    } else {
                        this.plugin.getLogger().warning("Invalid enchantment: " + typeName);
                    }
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Invalid enchantment: " + typeName);
                }
            }

            // Set attributes
            List<Map<?, ?>> attributesList = section.getMapList("attributes");
            for (Map<?, ?> attrMap : attributesList) {
                @SuppressWarnings("unchecked")
                Map<String, Object> castedAttrMap = (Map<String, Object>) attrMap;

                String typeName = (String) castedAttrMap.get("type");
                if (typeName == null) {
                    plugin.getLogger().warning("Attribute entry missing 'type' in " + section.getCurrentPath());
                    continue;
                }

                Object amountObj = castedAttrMap.get("amount");
                if (!(amountObj instanceof Number)) {
                    plugin.getLogger().warning("Attribute '" + typeName + "' missing or invalid 'amount' in " + section.getCurrentPath());
                    continue;
                }
                double amount = ((Number) amountObj).doubleValue();

                String operationName = (String) castedAttrMap.getOrDefault("operation", "ADD_NUMBER");
                AttributeModifier.Operation operation;
                try {
                    operation = AttributeModifier.Operation.valueOf(operationName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    operation = AttributeModifier.Operation.ADD_NUMBER;
                }

                Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(typeName.toLowerCase()));
                if (attribute == null) {
                    this.plugin.getLogger().warning("Invalid attribute: " + typeName);
                    continue;
                }

                // Read slots: optional list of strings, e.g. ["mainhand", "off_hand"]
                List<String> slotNames = new ArrayList<>();
                Object slotsObj = castedAttrMap.get("slots");
                if (slotsObj instanceof List<?> rawList) {
                    for (Object o : rawList) {
                        if (o != null) {
                            slotNames.add(o.toString());
                        }
                    }
                }

                // If no slots specified, default to ANY (matches your old behavior)
                if (slotNames.isEmpty()) {
                    slotNames.add("any");
                }

                for (String rawSlotName : slotNames) {
                    String slotName = rawSlotName.trim();
                    if (slotName.isEmpty()) continue;

                    EquipmentSlotGroup slotGroup = EquipmentSlotGroup.getByName(slotName);
                    if (slotGroup == null) {
                        plugin.getLogger().warning("Invalid attribute slot '" + rawSlotName +
                                "' for attribute '" + typeName + "' in " + section.getCurrentPath() +
                                ", skipping this slot");
                        continue;
                    }

                    try {
                        AttributeModifier modifier = new AttributeModifier(
                                new NamespacedKey(
                                        plugin,
                                        typeName.toLowerCase() + "-" + slotName.toLowerCase()
                                ),
                                amount,
                                operation,
                                slotGroup
                        );
                        meta.addAttributeModifier(attribute, modifier);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Failed to add attribute '" + typeName +
                                "' for slot '" + rawSlotName + "': " + e.getMessage());
                    }
                }
            }


            // Set unbreakable
            if (section.getBoolean("unbreakable", false)) {
                meta.setUnbreakable(true);
            }

            // Set custom model data
            if (section.contains("custom-model-data")) {
                meta.setCustomModelData(section.getInt("custom-model-data"));
            }

            // Add glow effect
            if (section.getBoolean("glow", false)) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Loads spawn blocker items and adds them to the uniques collection
     */
    private void loadSpawnBlockerItems() {
        if (plugin.getSpawnBlockerManager() == null) {
            plugin.debug("SpawnBlockerManager not initialized yet, skipping spawn blocker items", "save_and_load");
            return;
        }

        for (String blockerId : plugin.getSpawnBlockerManager().getSpawnBlockerIds()) {
            SpawnBlockerManager.SpawnBlockerConfig config = plugin.getSpawnBlockerManager().getSpawnBlockerConfig(blockerId);

            // Only add CUSTOM type spawn blockers as droppable items
            if (config != null && config.enabled() && config.type().equals("CUSTOM")) {
                ItemStack item = plugin.getSpawnBlockerManager().createSpawnBlockerItem(blockerId);
                if (item != null) {
                    // Add to uniques with a prefix to distinguish them
                    String itemId = "spawn-blocker-" + blockerId;
                    item = setCustomItemId(itemId, item);
                    this.uniques.put(itemId, item);
                    this.plugin.debug("Loaded spawn blocker item: " + blockerId, "save_and_load");
                }
            }
        }
    }

    /**
     * Gets all spawn blocker items
     *
     * @return Map of spawn blocker item IDs to ItemStacks
     */
    public Map<String, ItemStack> getSpawnBlockerItems() {
        Map<String, ItemStack> spawnBlockerItems = new HashMap<>();
        for (Map.Entry<String, ItemStack> entry : uniques.entrySet()) {
            if (entry.getKey().startsWith("spawn-blocker-")) {
                spawnBlockerItems.put(entry.getKey(), entry.getValue());
            }
        }
        return spawnBlockerItems;
    }


    /**
     * Checks if an item ID is a spawn blocker item
     *
     * @param itemId The item ID to check
     * @return True if it's a spawn blocker item
     */
    public boolean isSpawnBlockerItem(String itemId) {
        return itemId != null && itemId.startsWith("spawn-blocker-");
    }

    /**
     * Gets the spawn blocker ID from an equipment item ID
     *
     * @param itemId The equipment item ID (e.g., "spawn-blocker-soul-torch")
     * @return The spawn blocker ID (e.g., "soul-torch") or null if not a spawn blocker
     */
    public String getSpawnBlockerIdFromItemId(String itemId) {
        if (isSpawnBlockerItem(itemId)) {
            return itemId.substring("spawn-blocker-".length());
        }
        return null;
    }

    /**
     * Reloads spawn blocker items (call this when spawn blockers are reloaded)
     */
    public void reloadSpawnBlockerItems() {
        // Remove existing spawn blocker items
        uniques.entrySet().removeIf(entry -> entry.getKey().startsWith("spawn-blocker-"));

        // Reload spawn blocker items
        loadSpawnBlockerItems();
    }

    /**
     * Applies equipment to a power mob
     *
     * @param powerMob        The power mob
     * @param slot            The equipment slot
     * @param equipmentConfig The equipment config
     */
    public void applyEquipment(PowerMob powerMob, String slot, EquipmentItemConfig equipmentConfig) {
        LivingEntity entity = powerMob.getEntity();

        // Get the equipment
        ItemStack item = getEquipment(equipmentConfig.getItem());
        if (item == null) {
            String id = equipmentConfig.getItem();
            item = createVanillaItem(id);
            if (item == null) {
                this.plugin.getLogger().warning("Invalid equipment: " + equipmentConfig.getItem() + " for slot " + slot);
                return;
            }
            ItemMeta meta = item.getItemMeta();
            for (EnchantmentConfig enchantment : equipmentConfig.getEnchantments()) {
                int actualLevel;
                if (enchantment.getMinLevel() == enchantment.getMaxLevel()) {
                    actualLevel = enchantment.getMinLevel();
                } else {
                    actualLevel = WeightedRandom.getWeightedRandom(enchantment.getMinLevel(), enchantment.getMaxLevel(), enchantment.getWeight());
                }
                try {
                    Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantment.getType().toLowerCase()));
                    if (enchant != null) {
                        meta.addEnchant(enchant, actualLevel, true);
                    } else {
                        this.plugin.getLogger().warning("Invalid enchantment: " + enchantment.getType());
                    }
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Invalid enchantment: " + enchantment.getType());
                }
            }
            item.setItemMeta(meta);
        }

        // Apply to the entity
        switch (slot.toLowerCase()) {
            case "possible-helmets":
                entity.getEquipment().setHelmet(item);
                entity.getEquipment().setHelmetDropChance(0.0f);
                break;
            case "possible-chestplates":
                entity.getEquipment().setChestplate(item);
                entity.getEquipment().setChestplateDropChance(0.0f);
                break;
            case "possible-leggings":
                entity.getEquipment().setLeggings(item);
                entity.getEquipment().setLeggingsDropChance(0.0f);
                break;
            case "possible-boots":
                entity.getEquipment().setBoots(item);
                entity.getEquipment().setBootsDropChance(0.0f);
                break;
            case "possible-weapons":
                entity.getEquipment().setItemInMainHand(item);
                entity.getEquipment().setItemInMainHandDropChance(0.0f);
                break;
            case "possible-offhands":
                entity.getEquipment().setItemInOffHand(item);
                entity.getEquipment().setItemInOffHandDropChance(0.0f);
                break;
            default:
                this.plugin.getLogger().warning("Invalid equipment slot: " + slot);
                break;
        }
    }

    private ItemStack createVanillaItem(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String baseId = id;
        int dashIndex = id.lastIndexOf('-');
        if (dashIndex > 0 && dashIndex < id.length() - 1) {
            String suffix = id.substring(dashIndex + 1);
            boolean numeric = true;
            for (int i = 0; i < suffix.length(); i++) {
                if (!Character.isDigit(suffix.charAt(i))) {
                    numeric = false;
                    break;
                }
            }
            if (numeric) {
                baseId = id.substring(0, dashIndex);
            }
        }

        try {
            Material material = Material.valueOf(baseId.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            // Not a valid Material
            return null;
        }
    }

    /**
     * Gets equipment by ID
     *
     * @param id The equipment ID
     * @return The equipment item, or null if not found
     */
    public ItemStack getEquipment(String id) {
        return getAllEquipment().get(id);
    }

    public Map<String, ItemStack> getAllEquipment() {
        Map<String, ItemStack> equipment = new HashMap<>();
        equipment.putAll(this.weapons);
        equipment.putAll(this.armor);
        equipment.putAll(this.uniques);
        return equipment;
    }

    private ItemStack setCustomItemId(String customId, ItemStack item) {
        if (item == null || customId == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(PowerMobsPlugin.getInstance(), "custom-id");
        dataContainer.set(key, PersistentDataType.STRING, customId);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Removes a custom item from the configuration
     *
     * @param type The type of item (weapon or armor)
     * @param id   The item ID to remove
     * @return True if removed successfully
     */
    public boolean removeCustomItem(String type, String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        FileConfiguration config = equipmentConfigManager.getConfig();
        String path = type.equalsIgnoreCase("weapon") ? "weapons." + id : "armor." + id;

        if (!config.contains(path)) {
            return false;
        }

        // Remove from config
        config.set(path, null);
        equipmentConfigManager.saveConfig();

        // Remove from cache
        if (type.equalsIgnoreCase("weapon")) {
            weapons.remove(id);
        } else if (type.equalsIgnoreCase("armor")) {
            armor.remove(id);
        }

        return true;
    }

}
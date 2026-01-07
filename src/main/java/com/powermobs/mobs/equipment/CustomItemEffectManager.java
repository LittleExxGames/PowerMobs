package com.powermobs.mobs.equipment;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.equipment.items.TriggerType;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages custom item effects - integrates with existing EquipmentManager
 */
public class CustomItemEffectManager {

    private final PowerMobsPlugin plugin;
    private final Map<String, List<ItemEffect>> itemEffects = new HashMap<>();
    private final NamespacedKey customIdKey;

    public CustomItemEffectManager(PowerMobsPlugin plugin) {
        this.plugin = plugin;
        this.customIdKey = new NamespacedKey(plugin, "custom-id");
        loadItemEffects();
    }

    /**
     * Loads item effects from the same itemsconfig.yml your EquipmentManager uses
     */
    public void loadItemEffects() {
        itemEffects.clear();

        // Use the same config manager as EquipmentManager
        FileConfiguration config = plugin.getConfigManager().getItemsConfigManager().getConfig();

        loadEffectsFromSection(config, "equipment.weapons");
        loadEffectsFromSection(config, "equipment.armor");
        loadEffectsFromSection(config, "equipment.uniques");

        plugin.getLogger().info("Loaded effects for " + itemEffects.size() + " custom items");
    }

    /**
     * Loads effects from a configuration section
     */
    private void loadEffectsFromSection(FileConfiguration config, String sectionPath) {
        ConfigurationSection section = config.getConfigurationSection(sectionPath);
        if (section == null) return;

        for (String itemId : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(itemId);
            if (itemSection == null) continue;

            ConfigurationSection effectsSection = itemSection.getConfigurationSection("effects");
            if (effectsSection == null) continue;

            List<ItemEffect> effects = new ArrayList<>();
            for (String effectId : effectsSection.getKeys(false)) {
                ConfigurationSection effectSection = effectsSection.getConfigurationSection(effectId);
                if (effectSection != null) {
                    ItemEffect effect = new ItemEffect(effectSection);
                    if (effect.isValid()) {
                        effects.add(effect);
                    } else {
                        plugin.getLogger().warning("Invalid effect configuration for " + itemId + "." + effectId);
                    }
                }
            }

            if (!effects.isEmpty()) {
                itemEffects.put(itemId, effects);
                plugin.debug("Loaded " + effects.size() + " effects for item: " + itemId, "save_and_load");
            }
        }
    }

    /**
     * Gets the custom item ID from an ItemStack (uses same key as EquipmentManager)
     */
    public String getCustomItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(customIdKey, PersistentDataType.STRING);
    }

    /**
     * Gets all effects for a custom item
     */
    public List<ItemEffect> getItemEffects(String itemId) {
        return itemEffects.getOrDefault(itemId, new ArrayList<>());
    }

    /**
     * Gets all effects for an ItemStack
     */
    public List<ItemEffect> getItemEffects(ItemStack item) {
        String itemId = getCustomItemId(item);
        if (itemId == null) {
            return new ArrayList<>();
        }
        return getItemEffects(itemId);
    }

    /**
     * Gets effects for an item with a specific trigger type
     */
    public List<ItemEffect> getItemEffects(ItemStack item, TriggerType trigger) {
        return getItemEffects(item).stream()
                .filter(effect -> effect.getTrigger() == trigger)
                .toList();
    }

    /**
     * Reloads effects when configuration changes
     */
    public void reloadEffects() {
        loadItemEffects();
    }
}


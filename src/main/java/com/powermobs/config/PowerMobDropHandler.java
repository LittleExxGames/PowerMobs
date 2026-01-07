package com.powermobs.config;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.equipment.CustomDropConfig;
import com.powermobs.mobs.equipment.EnchantmentConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Handles power mob drops
 */
public class PowerMobDropHandler {

    private final PowerMobsPlugin plugin;
    private final Random random = new Random();

    /**
     * Creates a new drop handler
     *
     * @param plugin The plugin instance
     */
    public PowerMobDropHandler(PowerMobsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Processes drops from a power mob
     *
     * @param powerMob The power mob
     * @param killer   The player who killed the mob (may be null)
     * @param location The death location
     */
    public void processDrops(PowerMob powerMob, Player killer, Location location) {
        // If there's no killer or the killer hasn't done enough damage, don't drop special loot
        if (killer == null || !plugin.getDamageTracker().hasPlayerDoneEnoughDamage(powerMob, killer)) {
            plugin.debug("No special drops for mob " + powerMob.getEntityUuid() +
                    " - killer: " + (killer != null ? killer.getName() : "none") +
                    ", enough damage: " + (killer != null && plugin.getDamageTracker().hasPlayerDoneEnoughDamage(powerMob, killer)), "drops");
            return;
        }

        IPowerMobConfig config;
        String mobType;

        if (powerMob.isRandom()) {
            config = plugin.getConfigManager().getRandomMobConfig();
            mobType = "random mob";
        } else {
            config = plugin.getConfigManager().getPowerMob(powerMob.getId());
            mobType = "power mob";
            if (config == null) {
                plugin.debug("No config for " + mobType + " " + powerMob.getEntityUuid() + " - skipping drops", "drops");
                return;
            }
        }

        handleDrops(location, config, killer, mobType);
    }

    /**
     * Handles drops for any mob configuration
     *
     * @param location The death location
     * @param config   The mob configuration
     * @param killer   The player who killed the mob
     * @param mobType  Type description for logging (power mob/random mob)
     */
    private void handleDrops(Location location, IPowerMobConfig config, Player killer, String mobType) {
        if (location.getWorld() == null) {
            return;
        }

        int dropCount = config.getActualDropCount();
        int dropsAt = 0;
        this.plugin.debug("Handling " + dropCount + " drops for " + mobType, "drops");

        List<CustomDropConfig> drops = new ArrayList<>(config.getDrops());
        Collections.shuffle(drops);

        for (CustomDropConfig drop : drops) {
            if (random.nextDouble() >= drop.getChance()) {
                continue;
            }

            if (dropsAt >= dropCount) {
                break;
            }

            int amount = drop.getActualAmount();
            ItemStack item = createDropItem(drop, amount);

            if (item != null) {
                location.getWorld().dropItemNaturally(location, item);
                plugin.debug("Dropped " + amount + " " + drop.getItem() + " for player " + killer.getName(), "drops");
                dropsAt++;
            } else {
                plugin.getLogger().warning("Failed to create drop item: " + drop.getItem() + " for " + mobType);
            }
        }

        // Give Experience
        if (random.nextDouble() <= config.getExperienceChance()) {
            int amount = config.getActualExperienceAmount();
            killer.giveExp(amount);
            plugin.debug("Dropped " + amount + " experience for player " + killer.getName(), "drops");
        }
    }

    /**
     * Creates an ItemStack from a drop configuration
     *
     * @param drop   The drop configuration
     * @param amount The amount to drop
     * @return The created ItemStack, or null if creation failed
     */
    private ItemStack createDropItem(CustomDropConfig drop, int amount) {
        String itemId = drop.getItem();
        ItemStack item;

        // Try to get custom item from equipment manager first
        ItemStack customItem = plugin.getEquipmentManager().getEquipment(itemId);
        if (customItem != null) {
            // Clone the custom item and set the amount
            item = customItem.clone();
            item.setAmount(amount);
            plugin.debug("Using custom item: " + itemId, "drops");
        } else {
            // Parse as vanilla item
            item = createVanillaItem(itemId, amount);
        }

        if (item != null) {
            // Add enchantments from drop configuration
            addEnchantments(item, drop.getEnchantments());
        }

        return item;
    }

    /**
     * Creates a vanilla ItemStack from an item ID
     *
     * @param itemId The item ID (possibly with -1, -2, etc. suffix)
     * @param amount The amount to create
     * @return The created ItemStack, or null if creation failed
     */
    private ItemStack createVanillaItem(String itemId, int amount) {
        try {
            // Parse vanilla item ID - remove numeric suffixes like -1, -2, etc.
            String materialName = itemId;
            if (itemId.matches(".*-\\d+$")) {
                materialName = itemId.substring(0, itemId.lastIndexOf("-"));
                plugin.debug("Parsed vanilla item ID: " + itemId + " -> " + materialName, "drops");
            }

            Material material = Material.valueOf(materialName.toUpperCase());
            return new ItemStack(material, amount);

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material in drop: " + itemId);
            return null;
        }
    }

    /**
     * Adds enchantments to an ItemStack
     *
     * @param item         The ItemStack to enchant
     * @param enchantments The enchantments to add
     */
    private void addEnchantments(ItemStack item, List<EnchantmentConfig> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return;
        }

        Material material = item.getType();

        if (material == Material.ENCHANTED_BOOK) {
            // Handle enchanted books
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            if (meta != null) {
                for (EnchantmentConfig enchant : enchantments) {
                    Enchantment enchantment = Enchantment.getByName(enchant.getType().toUpperCase());
                    if (enchantment != null) {
                        int level = enchant.getActualLevel();
                        meta.addStoredEnchant(enchantment, level, true);
                    } else {
                        plugin.getLogger().warning("Invalid enchantment: " + enchant.getType());
                    }
                }
                item.setItemMeta(meta);
            }
        } else {
            // Handle regular items
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                for (EnchantmentConfig enchant : enchantments) {
                    Enchantment enchantment = Enchantment.getByName(enchant.getType().toUpperCase());
                    if (enchantment != null) {
                        int level = enchant.getActualLevel();
                        meta.addEnchant(enchantment, level, true);
                    } else {
                        plugin.getLogger().warning("Invalid enchantment: " + enchant.getType());
                    }
                }
                item.setItemMeta(meta);
            }
        }
    }
}


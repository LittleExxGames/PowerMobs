package com.custommobs.config;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Handles custom mob drops
 */
public class CustomMobDropHandler {

    private final CustomMobsPlugin plugin;
    private final Random random = new Random();

    /**
     * Creates a new drop handler
     *
     * @param plugin The plugin instance
     */
    public CustomMobDropHandler(CustomMobsPlugin plugin) {
        this.plugin = plugin;
    }

    private static List<CustomMobConfig.CustomDropConfig> getDrops(List<CustomMobConfig.CustomDropConfig> drops, int dropLimit) {
        List<CustomMobConfig.CustomDropConfig> reservoir = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < drops.size(); i++) {
            CustomMobConfig.CustomDropConfig drop = drops.get(i);
            if (random.nextDouble() < drop.getChance()) {
                if (reservoir.size() < dropLimit) {
                    reservoir.add(drop);
                } else {
                    int j = random.nextInt(i + 1);
                    if (j < dropLimit) {
                        reservoir.set(j, drop);
                    }
                }
            }
        }
        return reservoir;
    }

    /**
     * Processes drops from a custom mob
     *
     * @param customMob The custom mob
     * @param killer    The player who killed the mob (may be null)
     * @param location  The death location
     */
    public void processDrops(CustomMob customMob, Player killer, Location location) {

        // If there's no killer or the killer hasn't done enough damage, don't drop special loot
        if (killer == null || !plugin.getDamageTracker().hasPlayerDoneEnoughDamage(customMob, killer)) {
            plugin.debug("No special drops for mob " + customMob.getEntityUuid() +
                    " - killer: " + (killer != null ? killer.getName() : "none") +
                    ", enough damage: " + (killer != null && plugin.getDamageTracker().hasPlayerDoneEnoughDamage(customMob, killer)));
            return;
        }

        if (customMob.isRandom()) {
            RandomMobConfig randomConfig = plugin.getConfigManager().getRandomMobConfig();
            handleRandomDrops(location, randomConfig, killer);
            return;
        }

        CustomMobConfig config = plugin.getConfigManager().getCustomMob(customMob.getId());
        if (config == null) {
            plugin.debug("No config for mob " + customMob.getEntityUuid() + " - skipping drops");
            return;
        }

        handleDrops(location, config, killer);
    }

    /**
     * Handles custom drops for a predefined mob
     *
     * @param location The death location
     * @param config   The mob configuration
     */
    private void handleDrops(Location location, CustomMobConfig config, Player killer) {
        if (location.getWorld() == null) {
            return;
        }

        int dropCount = config.getActualDropCount();
        int dropsAt = 0;
        this.plugin.debug("Handling " + dropCount + " drops for mob " + config.getId());
        List<CustomMobConfig.CustomDropConfig> drops = new ArrayList<>(config.getDrops());
        ;
        Collections.shuffle(drops);

        // Process drops
        //for (CustomMobConfig.CustomDropConfig drop : config.getDrops()) {
        for (CustomMobConfig.CustomDropConfig drop : drops) {
            // Check chance
            if (random.nextDouble() >= drop.getChance()) {
                continue;
            }

            // Determine amount using weighted random
            int amount = drop.getActualAmount();

            // Special case for experience
            if (drop.getItem().equalsIgnoreCase("EXPERIENCE")) {
                killer.giveExp(amount);
                continue;
            }

            if (dropsAt >= dropCount) {
                continue;
            }
            dropsAt++;

            try {
                // Create item
                Material material = Material.valueOf(drop.getItem().toUpperCase());
                ItemStack item = new ItemStack(material, amount);

                // Add enchantments
                if (!drop.getEnchantments().isEmpty()) {
                    if (material == Material.ENCHANTED_BOOK) {
                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                        if (meta != null) {
                            for (CustomMobConfig.EnchantmentConfig enchant : drop.getEnchantments()) {
                                Enchantment enchantment = Enchantment.getByName(enchant.getType().toUpperCase());
                                if (enchantment != null) {
                                    // Use the actual level from the weighted calculation
                                    int level = enchant.getActualLevel();
                                    meta.addStoredEnchant(enchantment, level, true);
                                }
                            }
                            item.setItemMeta(meta);
                        }
                    } else {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            for (CustomMobConfig.EnchantmentConfig enchant : drop.getEnchantments()) {
                                Enchantment enchantment = Enchantment.getByName(enchant.getType().toUpperCase());
                                if (enchantment != null) {
                                    // Use the actual level from the weighted calculation
                                    int level = enchant.getActualLevel();
                                    meta.addEnchant(enchantment, level, true);
                                }
                            }
                            item.setItemMeta(meta);
                        }
                    }
                }


                // Drop the item
                location.getWorld().dropItemNaturally(location, item);
                plugin.debug("Dropped " + amount + " " + material + " for player " + killer.getName());

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in drop: " + drop.getItem());
            }
        }
    }

    /**
     * Handles drops for a random mob
     *
     * @param location The death location
     */
    private void handleRandomDrops(Location location, RandomMobConfig config, Player killer) {
        if (location.getWorld() == null) {
            return;
        }

        //List<ItemStack> drops = new ArrayList<>();
        int dropCount = config.getActualDropCount();
        int dropsAt = 0;
        this.plugin.debug("Handling " + dropCount + " drops for random mob.");
        // Shuffle the possible drops list to randomize selection
        List<CustomMobConfig.CustomDropConfig> drops = new ArrayList<>(config.getDrops());
        Collections.shuffle(drops);

        for (CustomMobConfig.CustomDropConfig drop : drops) {
            // Check chance
            if (random.nextDouble() >= drop.getChance()) {
                continue;
            }

            // Determine amount using weighted random
            int amount = drop.getActualAmount();

            // Special case for experience
            if (drop.getItem().equalsIgnoreCase("EXPERIENCE")) {
                killer.giveExp(amount);
                continue;
            }

            if (dropsAt >= dropCount) {
                continue;
            }
            dropsAt++;

            try {
                // Create item
                Material material = Material.valueOf(drop.getItem().toUpperCase());
                ItemStack item = new ItemStack(material, amount);

                // Add enchantments
                if (!drop.getEnchantments().isEmpty()) {
                    if (material == Material.ENCHANTED_BOOK) {
                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                        if (meta != null) {
                            for (CustomMobConfig.EnchantmentConfig enchant : drop.getEnchantments()) {
                                Enchantment enchantment = Enchantment.getByName(enchant.getType().toUpperCase());
                                if (enchantment != null) {
                                    int level = enchant.getActualLevel();
                                    meta.addStoredEnchant(enchantment, level, true);
                                }
                            }
                            item.setItemMeta(meta);
                        }
                    } else {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            for (CustomMobConfig.EnchantmentConfig enchant : drop.getEnchantments()) {
                                Enchantment enchantment = Enchantment.getByName(enchant.getType().toUpperCase());
                                if (enchantment != null) {
                                    int level = enchant.getActualLevel();
                                    meta.addEnchant(enchantment, level, true);
                                }
                            }
                            item.setItemMeta(meta);
                        }
                    }
                }

                // Drop the item
                location.getWorld().dropItemNaturally(location, item);
                plugin.debug("Dropped " + amount + " " + material + " for player " + killer.getName());


            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in random mob drop: " + drop.getItem());
            }
        }

//        // Drop experience (random 5-20) for random mobs
//        int exp = 5 + this.random.nextInt(16);
//        location.getWorld().spawn(location, ExperienceOrb.class, orb -> {
//            orb.setExperience(exp);
//        });
//
//        // Drop the items
//        for (ItemStack item : drops) {
//            location.getWorld().dropItemNaturally(location, item);
//        }
    }
}


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

    /**
     * Processes drops from a custom mob
     *
     * @param customMob The custom mob
     * @param killer The player who killed the mob (may be null)
     * @param location The death location
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
            handleRandomDrops(location, customMob.getEntity());
            return;
        }

        CustomMobConfig config = plugin.getConfigManager().getCustomMob(customMob.getId());
        if (config == null) {
            plugin.debug("no config for mob " + customMob.getEntityUuid() + " - skipping drops");
            return;
        }

        handleDrops(location, config, killer);
    }
    /**
     * Handles custom drops for a predefined mob
     *
     * @param location The death location
     * @param config The mob configuration
     */
    private void handleDrops(Location location, CustomMobConfig config, Player killer) {
        if (location.getWorld() == null) {
            return;
        }

        // Process drops
        for (CustomMobConfig.CustomDropConfig drop : config.getDrops()) {
            // Check chance
            if (random.nextDouble() >= drop.getChance()) {
                continue;
            }

            // Determine amount
            int amount = drop.getMinAmount();
            if (drop.getMaxAmount() > drop.getMinAmount()) {
                amount += random.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
            }

            // Special case for experience
            if (drop.getItem().equalsIgnoreCase("EXPERIENCE")) {
                killer.giveExp(amount);
                continue;
            }

            try {
                // Create item
                Material material = Material.valueOf(drop.getItem().toUpperCase());
                ItemStack item = new ItemStack(material, amount);

                // Add enchantments
                if (!drop.getEnchantments().isEmpty()) {
                    ItemMeta meta = item.getItemMeta();
                    for (CustomMobConfig.EnchantmentConfig enchant : drop.getEnchantments()) {
                        Enchantment enchantment = Enchantment.getByName(enchant.getType().toUpperCase());
                        if (enchantment != null) {
                            meta.addEnchant(enchantment, enchant.getLevel(), true);
                        }
                    }
                    item.setItemMeta(meta);
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
     * @param entity The entity
     */
    private void handleRandomDrops(Location location, LivingEntity entity) {
        if (location.getWorld() == null) {
            return;
        }

        // Generate some random drops based on the entity type
        ArrayList<ItemStack> drops = new ArrayList<>();

        // 50% chance for 1-3 of the mob's normal drops
        if (Math.random() < 0.5) {
            int count = 1 + this.random.nextInt(3);
            for (int i = 0; i < count; i++) {
                switch (entity.getType()) {
                    case ZOMBIE:
                        drops.add(new ItemStack(Material.ROTTEN_FLESH, 1 + this.random.nextInt(3)));
                        break;
                    case SKELETON:
                        drops.add(new ItemStack(Material.BONE, 1 + this.random.nextInt(3)));
                        if (Math.random() < 0.3) {
                            drops.add(new ItemStack(Material.ARROW, 1 + this.random.nextInt(5)));
                        }
                        break;
                    case SPIDER:
                        drops.add(new ItemStack(Material.STRING, 1 + this.random.nextInt(3)));
                        if (Math.random() < 0.3) {
                            drops.add(new ItemStack(Material.SPIDER_EYE, 1));
                        }
                        break;
                    case CREEPER:
                        if (Math.random() < 0.5) {
                            drops.add(new ItemStack(Material.GUNPOWDER, 1 + this.random.nextInt(3)));
                        }
                        break;
                    case ENDERMAN:
                        if (Math.random() < 0.4) {
                            drops.add(new ItemStack(Material.ENDER_PEARL, 1));
                        }
                        break;
                    default:
                        // No custom drops
                        break;
                }
            }
        }

        // 20% chance for a rare item
        if (Math.random() < 0.2) {
            double roll = Math.random();
            if (roll < 0.4) {
                // Common rare items (40%)
                Material[] materials = {
                    Material.IRON_INGOT, Material.GOLD_INGOT, Material.COAL, Material.REDSTONE, Material.LAPIS_LAZULI
                };
                drops.add(new ItemStack(materials[this.random.nextInt(materials.length)], 1 + this.random.nextInt(3)));
            } else if (roll < 0.7) {
                // Uncommon rare items (30%)
                Material[] materials = {
                    Material.EMERALD, Material.DIAMOND, Material.EXPERIENCE_BOTTLE, Material.GOLDEN_APPLE
                };
                drops.add(new ItemStack(materials[this.random.nextInt(materials.length)], 1));
            } else if (roll < 0.9) {
                // Rare items (20%)
                Material[] materials = {
                    Material.NETHERITE_SCRAP, Material.TOTEM_OF_UNDYING, Material.ENCHANTED_GOLDEN_APPLE
                };
                drops.add(new ItemStack(materials[this.random.nextInt(materials.length)], 1));
            } else {
                // Extremely rare (10%)
                // Create an enchanted book with a random enchantment
                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                if (meta != null) {
                    // Get a random enchantment
                    Enchantment[] enchantments = Enchantment.values();
                    Enchantment enchantment = enchantments[this.random.nextInt(enchantments.length)];
                    int level = 1 + this.random.nextInt(enchantment.getMaxLevel());

                    meta.addStoredEnchant(enchantment, level, true);
                    book.setItemMeta(meta);
                }
                drops.add(book);
            }
        }

        // Drop experience (random 5-20)
        int exp = 5 + this.random.nextInt(16);
        location.getWorld().spawn(location, ExperienceOrb.class, orb -> {
            orb.setExperience(exp);
        });

        // Drop the items
        for (ItemStack item : drops) {
            location.getWorld().dropItemNaturally(location, item);
        }
    }
}


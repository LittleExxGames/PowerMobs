package com.custommobs.events;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.config.CustomMobConfig;
import com.custommobs.mobs.CustomMob;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.Random;

/**
 * Handles custom mob death events
 */
@RequiredArgsConstructor
public class MobDeathListener implements Listener {

    private final CustomMobsPlugin plugin;
    private final Random random = new Random();
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        CustomMob customMob = CustomMob.getFromEntity(this.plugin, entity);
        
        if (customMob == null) {
            return;
        }
        
        // Clear default drops
        event.getDrops().clear();
        event.setDroppedExp(0);
        
//        // Get the mob configuration
//        CustomMobConfig config = this.plugin.getConfigManager().getCustomMob(customMob.getId());
//
//        if (config != null) {
//            // Handle custom drops
//            handleDrops(entity.getLocation(), config);
//        } else if (customMob.isRandom()) {
//            // Handle random mob drops
//            handleRandomDrops(entity.getLocation(), entity);
//        }

        // Get the killer player (if any)
        Player killer = entity.getKiller();
        plugin.debug("Killer and it happened!: " + killer);
        // Process drops using the drop handler
        plugin.getDropHandler().processDrops(customMob, killer, entity.getLocation());


        // Unregister the custom mob
        customMob.remove();
        this.plugin.getCustomMobManager().unregisterCustomMob(customMob);
    }
    
//    /**
//     * Handles custom drops for a predefined mob
//     *
//     * @param location The death location
//     * @param config The mob configuration
//     */
//    private void handleDrops(Location location, CustomMobConfig config) {
//        if (location.getWorld() == null) {
//            return;
//        }
//
//        for (CustomMobConfig.CustomDropConfig drop : config.getDrops()) {
//            // Check chance
//            if (Math.random() > drop.getChance()) {
//                continue;
//            }
//
//            // Calculate amount
//            final int amount;
//            if (drop.getMaxAmount() > drop.getMinAmount()) {
//                amount = drop.getMinAmount() + this.random.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
//            } else {
//                amount = drop.getMinAmount();
//            }
//
//            // Special case for experience
//            if (drop.getItem().equalsIgnoreCase("EXPERIENCE")) {
//                location.getWorld().spawn(location, ExperienceOrb.class, orb -> {
//                    orb.setExperience(amount);
//                });
//                continue;
//            }
//
//
//            // Create the item
//            Material material;
//            try {
//                material = Material.valueOf(drop.getItem().toUpperCase());
//            } catch (IllegalArgumentException e) {
//                this.plugin.getLogger().warning("Invalid drop material: " + drop.getItem());
//                continue;
//            }
//
//            ItemStack item = new ItemStack(material, amount);
//
//            // Apply enchantments
//            if (!drop.getEnchantments().isEmpty()) {
//                if (material == Material.ENCHANTED_BOOK) {
//                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
//                    if (meta != null) {
//                        for (CustomMobConfig.EnchantmentConfig enchant : drop.getEnchantments()) {
//                            try {
//                                Enchantment enchantment = Enchantment.getByName(enchant.getType());
//                                if (enchantment != null) {
//                                    meta.addStoredEnchant(enchantment, enchant.getLevel(), true);
//                                }
//                            } catch (IllegalArgumentException e) {
//                                this.plugin.getLogger().warning("Invalid enchantment: " + enchant.getType());
//                            }
//                        }
//                        item.setItemMeta(meta);
//                    }
//                } else {
//                    for (CustomMobConfig.EnchantmentConfig enchant : drop.getEnchantments()) {
//                        try {
//                            Enchantment enchantment = Enchantment.getByName(enchant.getType());
//                            if (enchantment != null) {
//                                item.addUnsafeEnchantment(enchantment, enchant.getLevel());
//                            }
//                        } catch (IllegalArgumentException e) {
//                            this.plugin.getLogger().warning("Invalid enchantment: " + enchant.getType());
//                        }
//                    }
//                }
//            }
//
//            // Drop the item
//            location.getWorld().dropItemNaturally(location, item);
//        }
//    }
//
//    /**
//     * Handles drops for a random mob
//     *
//     * @param location The death location
//     * @param entity The entity
//     */
//    private void handleRandomDrops(Location location, LivingEntity entity) {
//        if (location.getWorld() == null) {
//            return;
//        }
//
//        // Generate some random drops based on the entity type
//        ArrayList<ItemStack> drops = new ArrayList<>();
//
//        // 50% chance for 1-3 of the mob's normal drops
//        if (Math.random() < 0.5) {
//            int count = 1 + this.random.nextInt(3);
//            for (int i = 0; i < count; i++) {
//                switch (entity.getType()) {
//                    case ZOMBIE:
//                        drops.add(new ItemStack(Material.ROTTEN_FLESH, 1 + this.random.nextInt(3)));
//                        break;
//                    case SKELETON:
//                        drops.add(new ItemStack(Material.BONE, 1 + this.random.nextInt(3)));
//                        if (Math.random() < 0.3) {
//                            drops.add(new ItemStack(Material.ARROW, 1 + this.random.nextInt(5)));
//                        }
//                        break;
//                    case SPIDER:
//                        drops.add(new ItemStack(Material.STRING, 1 + this.random.nextInt(3)));
//                        if (Math.random() < 0.3) {
//                            drops.add(new ItemStack(Material.SPIDER_EYE, 1));
//                        }
//                        break;
//                    case CREEPER:
//                        if (Math.random() < 0.5) {
//                            drops.add(new ItemStack(Material.GUNPOWDER, 1 + this.random.nextInt(3)));
//                        }
//                        break;
//                    case ENDERMAN:
//                        if (Math.random() < 0.4) {
//                            drops.add(new ItemStack(Material.ENDER_PEARL, 1));
//                        }
//                        break;
//                    default:
//                        // No custom drops
//                        break;
//                }
//            }
//        }
//
//        // 20% chance for a rare item
//        if (Math.random() < 0.2) {
//            double roll = Math.random();
//            if (roll < 0.4) {
//                // Common rare items (40%)
//                Material[] materials = {
//                    Material.IRON_INGOT, Material.GOLD_INGOT, Material.COAL, Material.REDSTONE, Material.LAPIS_LAZULI
//                };
//                drops.add(new ItemStack(materials[this.random.nextInt(materials.length)], 1 + this.random.nextInt(3)));
//            } else if (roll < 0.7) {
//                // Uncommon rare items (30%)
//                Material[] materials = {
//                    Material.EMERALD, Material.DIAMOND, Material.EXPERIENCE_BOTTLE, Material.GOLDEN_APPLE
//                };
//                drops.add(new ItemStack(materials[this.random.nextInt(materials.length)], 1));
//            } else if (roll < 0.9) {
//                // Rare items (20%)
//                Material[] materials = {
//                    Material.NETHERITE_SCRAP, Material.TOTEM_OF_UNDYING, Material.ENCHANTED_GOLDEN_APPLE
//                };
//                drops.add(new ItemStack(materials[this.random.nextInt(materials.length)], 1));
//            } else {
//                // Extremely rare (10%)
//                // Create an enchanted book with a random enchantment
//                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
//                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
//                if (meta != null) {
//                    // Get a random enchantment
//                    Enchantment[] enchantments = Enchantment.values();
//                    Enchantment enchantment = enchantments[this.random.nextInt(enchantments.length)];
//                    int level = 1 + this.random.nextInt(enchantment.getMaxLevel());
//
//                    meta.addStoredEnchant(enchantment, level, true);
//                    book.setItemMeta(meta);
//                }
//                drops.add(book);
//            }
//        }
//
//        // Drop experience (random 5-20)
//        int exp = 5 + this.random.nextInt(16);
//        location.getWorld().spawn(location, ExperienceOrb.class, orb -> {
//            orb.setExperience(exp);
//        });
//
//        // Drop the items
//        for (ItemStack item : drops) {
//            location.getWorld().dropItemNaturally(location, item);
//        }
//    }
}
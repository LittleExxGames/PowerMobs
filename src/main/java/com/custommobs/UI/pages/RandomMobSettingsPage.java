package com.custommobs.UI.pages;

import com.custommobs.UI.framework.AbstractGUIPage;
import com.custommobs.UI.framework.GUIPageManager;
import com.custommobs.config.RandomMobConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Page for configuring random mob settings
 */
public class RandomMobSettingsPage extends AbstractGUIPage {

    public RandomMobSettingsPage(GUIPageManager pageManager) {
        super(pageManager, 54, ChatColor.DARK_PURPLE + "Random Mob Settings");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Random mob settings.");

        RandomMobConfig config = pageManager.getPlugin().getConfigManager().getRandomMobConfig();
        // Enabled/Disabled toggle
        Material toggleMaterial = config.isEnabled() ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        String toggleText = config.isEnabled() ? "Enabled" : "Disabled";
        ItemStack toggleButton = createGuiItem(toggleMaterial,
                ChatColor.GOLD + "Random Mobs: " + ChatColor.GREEN + toggleText,
                ChatColor.GRAY + "Click to toggle random mob generation");
        inventory.setItem(4, toggleButton);

        // Spawn chance slider
        ItemStack chanceItem = createGuiItem(Material.CLOCK,
                ChatColor.AQUA + "Spawn Chance: " + config.getChance(),
                ChatColor.GRAY + "Chance of a random mob spawning",
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(10, chanceItem);

        // Health multiplier range
        ItemStack healthItem = createGuiItem(Material.RED_DYE,
                ChatColor.RED + "Health Multiplier",
                ChatColor.GRAY + "Min: " + config.getMinHealthMultiplier(),
                ChatColor.GRAY + "Max: " + config.getMaxHealthMultiplier(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(12, healthItem);

        // Damage multiplier range
        ItemStack damageItem = createGuiItem(Material.IRON_SWORD,
                ChatColor.GOLD + "Damage Multiplier",
                ChatColor.GRAY + "Min: " + config.getMinDamageMultiplier(),
                ChatColor.GRAY + "Max: " + config.getMaxDamageMultiplier(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(14, damageItem);

        // Speed multiplier range
        ItemStack speedItem = createGuiItem(Material.FEATHER,
                ChatColor.WHITE + "Speed Multiplier",
                ChatColor.GRAY + "Min: " + config.getMinSpeedMultiplier(),
                ChatColor.GRAY + "Max: " + config.getMaxSpeedMultiplier(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(16, speedItem);

        // Ability settings
        ItemStack abilityItem = createGuiItem(Material.BLAZE_POWDER,
                ChatColor.LIGHT_PURPLE + "Ability Settings",
                ChatColor.GRAY + "Min Abilities: " + config.getMinAbilities(),
                ChatColor.GRAY + "Max Abilities: " + config.getMaxAbilities(),
                ChatColor.GRAY + "Possible Abilities: " + config.getPossibleAbilities().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(28, abilityItem);

        // Equipment settings
        ItemStack equipmentItem = createGuiItem(Material.DIAMOND_CHESTPLATE,
                ChatColor.BLUE + "Equipment Settings",
                ChatColor.GRAY + "Weapon Chance: " + config.getWeaponChance(),
                ChatColor.GRAY + "Armor Chances: " +
                        String.format("H:%.1f/C:%.1f/L:%.1f/B:%.1f",
                                config.getHelmetChance(),
                                config.getChestplateChance(),
                                config.getLeggingsChance(),
                                config.getBootsChance()),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(30, equipmentItem);

        // Drop settings
        ItemStack dropItem = createGuiItem(Material.CHEST,
                ChatColor.YELLOW + "Drop Settings",
                ChatColor.GRAY + "Min Drops: " + config.getMinDrops(),
                ChatColor.GRAY + "Max Drops: " + config.getMaxDrops(),
                ChatColor.GRAY + "Possible Drops: " + config.getDrops().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(32, dropItem);

        // Allowed mob types
        ItemStack mobTypesItem = createGuiItem(Material.ZOMBIE_SPAWN_EGG,
                ChatColor.GREEN + "Allowed Mob Types",
                ChatColor.GRAY + "Configured Types: " + config.getAllowedTypes().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(34, mobTypesItem);

        // Back button
        addBackButton(49, ChatColor.RED + "Back to Main Menu");
    }

    @Override
    public boolean handleClick(Player player, int slot) {
        if (slot == 49) {
            pageManager.navigateBack(player);
        }
        // Handle other clicks
        return true;
    }
}

package com.custommobs.UI.pages;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.UI.framework.AbstractGUIPage;
import com.custommobs.UI.framework.GUIPageManager;
import com.custommobs.config.CustomMobConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MobEditorPage extends AbstractGUIPage {
    private static final String title = ChatColor.DARK_PURPLE + "Mob Editor";
    private String selectedMobId;
    public MobEditorPage(GUIPageManager pageManager) {
        super(pageManager, 54, title);
    }

    public void build(){
        CustomMobsPlugin plugin = pageManager.getPlugin();
        String mobId = plugin.getGuiManager().getSelectedMobId();
        CustomMobConfig mobConfig = plugin.getConfigManager().getCustomMob(mobId);
        if (mobConfig == null) {
            //updateMobList();
            pageManager.navigateBack();
            return;
        }
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Mob Editor.");
        selectedMobId = mobId;

        // Mob info display
        Material icon = getMaterialForEntityType(mobConfig.getEntityType());
        ItemStack infoItem = createGuiItem(icon,
                ChatColor.GREEN + mobId,
                ChatColor.GRAY + "Type: " + mobConfig.getEntityType(),
                ChatColor.GRAY + "Name: " + ChatColor.translateAlternateColorCodes('&', mobConfig.getName()));
        inventory.setItem(4, infoItem);

        // Health setting
        ItemStack healthItem = createGuiItem(Material.RED_DYE,
                ChatColor.RED + "Health: " + mobConfig.getHealth(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(10, healthItem);

        // Damage multiplier
        ItemStack damageItem = createGuiItem(Material.IRON_SWORD,
                ChatColor.GOLD + "Damage Multiplier: " + mobConfig.getDamageMultiplier(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(12, damageItem);

        // Speed multiplier
        ItemStack speedItem = createGuiItem(Material.FEATHER,
                ChatColor.WHITE + "Speed Multiplier: " + mobConfig.getSpeedMultiplier(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(14, speedItem);

        // Entity type
        ItemStack typeItem = createGuiItem(Material.ZOMBIE_SPAWN_EGG,
                ChatColor.AQUA + "Entity Type: " + mobConfig.getEntityType(),
                ChatColor.GRAY + "Click to change");
        inventory.setItem(16, typeItem);

        // Abilities
        ItemStack abilitiesItem = createGuiItem(Material.BLAZE_POWDER,
                ChatColor.LIGHT_PURPLE + "Abilities",
                ChatColor.GRAY + "Current abilities: " + mobConfig.getAbilities().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(28, abilitiesItem);

        // Equipment
        ItemStack equipmentItem = createGuiItem(Material.DIAMOND_CHESTPLATE,
                ChatColor.BLUE + "Equipment",
                ChatColor.GRAY + "Current equipment: " + mobConfig.getEquipment().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(30, equipmentItem);

        // Drops
        ItemStack dropsItem = createGuiItem(Material.CHEST,
                ChatColor.YELLOW + "Drops",
                ChatColor.GRAY + "Drop count: " + mobConfig.getMinDrops() + "-" + mobConfig.getMaxDrops(),
                ChatColor.GRAY + "Possible drops: " + mobConfig.getDrops().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(32, dropsItem);

        // Spawn conditions
        ItemStack spawnItem = createGuiItem(Material.SPAWNER,
                ChatColor.GREEN + "Spawn Conditions",
                ChatColor.GRAY + "Configure where and when",
                ChatColor.GRAY + "this mob can spawn");
        inventory.setItem(34, spawnItem);

        // Test spawn
        ItemStack testItem = createGuiItem(Material.EGG,
                ChatColor.YELLOW + "Test Spawn",
                ChatColor.GRAY + "Spawn this mob at your location");
        inventory.setItem(40, testItem);

        // Delete mob
        ItemStack deleteItem = createGuiItem(Material.LAVA_BUCKET,
                ChatColor.RED + "Delete Mob",
                ChatColor.GRAY + "Permanently delete this mob",
                ChatColor.DARK_RED + "This cannot be undone!");
        inventory.setItem(45, deleteItem);

        // Back button
        ItemStack backButton = createGuiItem(Material.BARRIER,
                ChatColor.RED + "Back to Mob List");
        inventory.setItem(49, backButton);

        // Save button
        ItemStack saveButton = createGuiItem(Material.EMERALD,
                ChatColor.GREEN + "Save Changes",
                ChatColor.GRAY + "Save all changes to this mob");
        inventory.setItem(53, saveButton);
    }

    public boolean handleClick(Player player, int slot) {
        switch (slot) {
            case 49: // Back to mob list
                pageManager.navigateBack(player);
                break;
            case 40: // Test spawn
                player.closeInventory();
                player.performCommand("custommob spawn " + selectedMobId);
                break;
            case 45: // Delete mob
                // This would require implementation of a confirmation GUI or command
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "To delete a mob, use the command: /custommob delete " + selectedMobId);
                break;
            // Other editor actions would be implemented here
        }
        return true;
    }

    /**
     * Gets a material that represents an entity type
     */
    private Material getMaterialForEntityType(EntityType type) {
        return switch (type) {
            case ZOMBIE -> Material.ZOMBIE_HEAD;
            case SKELETON -> Material.SKELETON_SKULL;
            case CREEPER -> Material.CREEPER_HEAD;
            case SPIDER -> Material.SPIDER_EYE;
            case ENDERMAN -> Material.ENDER_PEARL;
            case SLIME -> Material.SLIME_BALL;
            case WITCH -> Material.BREWING_STAND;
            // Add more mappings as needed
            default -> Material.SPAWNER;
        };
    }
}

package com.custommobs.UI.pages;

import com.custommobs.UI.framework.AbstractGUIPage;
import com.custommobs.UI.framework.GUIPageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Page for creating a new custom mob
 */
public class CreateMobPage extends AbstractGUIPage {

    public CreateMobPage(GUIPageManager pageManager) {
        super(pageManager, 54, ChatColor.DARK_PURPLE + "Create Custom Mob");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Mob creation.");
        // Title
        ItemStack titleItem = createGuiItem(Material.DRAGON_EGG,
                ChatColor.GOLD + "Create New Custom Mob",
                ChatColor.GRAY + "Configure your new mob");
        inventory.setItem(4, titleItem);

        // Mob ID
        String selectedMobId = pageManager.getPlugin().getGuiManager().getSelectedMobId();
        ItemStack idItem = createGuiItem(Material.NAME_TAG,
                ChatColor.GREEN + "Set Mob ID",
                ChatColor.GRAY + "Current: " + (selectedMobId != null ? selectedMobId : "Not set"),
                ChatColor.GRAY + "Click to set a unique identifier");
        inventory.setItem(10, idItem);

        // Entity Type
        ItemStack typeItem = createGuiItem(Material.ZOMBIE_SPAWN_EGG,
                ChatColor.AQUA + "Entity Type",
                ChatColor.GRAY + "Choose the base entity type");
        inventory.setItem(12, typeItem);

        // Display Name
        ItemStack nameItem = createGuiItem(Material.OAK_SIGN,
                ChatColor.WHITE + "Set Display Name",
                ChatColor.GRAY + "Set the name shown above the mob");
        inventory.setItem(14, nameItem);

        // Health
        ItemStack healthItem = createGuiItem(Material.RED_DYE,
                ChatColor.RED + "Set Health",
                ChatColor.GRAY + "Default: 20.0");
        inventory.setItem(16, healthItem);

        // More settings options can go here...

        // Create button
        ItemStack createButton = createGuiItem(Material.EMERALD,
                ChatColor.GREEN + "Create Mob",
                ChatColor.GRAY + "Save and create this custom mob");
        inventory.setItem(40, createButton);

        // Back button
        addBackButton(36, ChatColor.RED + "Back to Main Menu");
    }

    @Override
    public boolean handleClick(Player player, int slot) {
        if (slot == 36) {
            pageManager.navigateBack(player);
        }
        // Handle other clicks
        return true;
    }
}


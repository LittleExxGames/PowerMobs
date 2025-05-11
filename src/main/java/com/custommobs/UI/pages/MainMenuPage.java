package com.custommobs.UI.pages;

import com.custommobs.UI.framework.AbstractGUIPage;
import com.custommobs.UI.framework.GUIPageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MainMenuPage extends AbstractGUIPage {
    public MainMenuPage(GUIPageManager pageManager) {
        super(pageManager, 54, ChatColor.WHITE+ "Main Menu");
    }

    @Override
    public void build(){
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Main menu.");
        // Custom Mobs List button
        ItemStack mobListButton = createGuiItem(Material.ZOMBIE_HEAD,
                ChatColor.GREEN + "Custom Mobs List",
                ChatColor.GRAY + "View and edit all custom mobs");
        inventory.setItem(20, mobListButton);

        // Create New Mob button
        ItemStack createMobButton = createGuiItem(Material.DRAGON_EGG,
                ChatColor.GOLD + "Create New Custom Mob",
                ChatColor.GRAY + "Create a brand new custom mob");
        inventory.setItem(22, createMobButton);

        // Random Mob Settings button
        ItemStack randomMobButton = createGuiItem(Material.COMPARATOR,
                ChatColor.AQUA + "Random Mob Settings",
                ChatColor.GRAY + "Configure random mob generation");
        inventory.setItem(24, randomMobButton);

        // Help button
        ItemStack helpButton = createGuiItem(Material.BOOK,
                ChatColor.YELLOW + "Help",
                ChatColor.GRAY + "Information about using this GUI");
        inventory.setItem(40, helpButton);

        // Reload Config button
        ItemStack reloadButton = createGuiItem(Material.REDSTONE,
                ChatColor.RED + "Reload Configuration",
                ChatColor.GRAY + "Reload all configurations from disk");
        inventory.setItem(49, reloadButton);
    }

    @Override
    public boolean handleClick(Player player, int slot) {
        //Help
        switch (slot) {
            case 20: // Custom Mobs List
                pageManager.navigateTo("mob_list", true, player);
                break;
            case 22: // Create New Mob
                pageManager.navigateTo("create_mob", true, player);
                break;
            case 24: // Random Mob Settings
                pageManager.navigateTo("random_mob_settings", true, player);
                break;
            case 40: // Help
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "=== CustomMobs GUI Help ===");
                player.sendMessage(ChatColor.YELLOW + "This GUI allows you to manage all your custom mobs.");
                player.sendMessage(ChatColor.YELLOW + "- View and edit existing mobs");
                player.sendMessage(ChatColor.YELLOW + "- Create new custom mobs");
                player.sendMessage(ChatColor.YELLOW + "- Configure random mob generation");
                player.sendMessage(ChatColor.YELLOW + "Use the tabs to navigate between sections.");
                break;
            case 49: // Reload Config
                player.closeInventory();
                player.performCommand("custommob reload");
                break;
        }
        // Handle other clicks
        return true;
    }

}

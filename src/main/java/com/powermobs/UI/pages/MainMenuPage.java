package com.powermobs.UI.pages;

import com.powermobs.UI.GUIManager;
import com.powermobs.UI.chat.ChatInputType;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.UI.framework.PowerMobType;
import com.powermobs.config.PowerManager;
import com.powermobs.config.PowerMobConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class MainMenuPage extends AbstractGUIPage {
    public MainMenuPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, 54, ChatColor.WHITE + "Main Menu");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Main menu.", "ui");
        // Power Mobs List button
        ItemStack mobListButton = createGuiItem(Material.ZOMBIE_HEAD,
                ChatColor.GREEN + "Power Mobs List",
                ChatColor.GRAY + "View and edit all power mobs");
        inventory.setItem(20, mobListButton);

        // Create New Mob button
        ItemStack createMobButton = createGuiItem(Material.DRAGON_EGG,
                ChatColor.GOLD + "Create New Power Mob",
                ChatColor.GRAY + "Create a brand new power mob");
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
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        switch (slot) {
            case 20: // Power Mobs List
                pageManager.navigateTo("mob_list", true, player);
                break;
            case 22: // Create New Mob
                startChatInput(player, ChatInputType.SET_ID, (value, p) -> {
                    PowerManager configManager = pageManager.getPlugin().getConfigManager();
                    String id = (String) value;
                    PowerMobConfig mob = configManager.getPowerMob(id);
                    if (mob != null) {
                        player.sendMessage(ChatColor.RED + "A mob with that name already exists!");
                        pageManager.navigateTo("main_menu", false, p);
                    } else {
                        guiManager.getCurrentPlayer().setType(PowerMobType.CUSTOM);
                        guiManager.getCurrentPlayer().setSelectedMobId(id);
                        mob = new PowerMobConfig(id);
                        boolean saved = configManager.savePowerMob(id, mob.toConfigMap());
                        if (!saved) {
                            pageManager.navigateTo("main_menu", false, p);
                        } else {
                            pageManager.navigateTo("mob_editor", true, p);
                        }
                    }
                });
                break;
            case 24: // Random Mob Settings
                guiManager.getCurrentPlayer().setType(PowerMobType.RANDOM);
                pageManager.navigateTo("random_mob_settings", true, player);
                break;
            case 40: // Help
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "=== PowerMobs GUI Help ===");
                player.sendMessage(ChatColor.YELLOW + "This GUI allows you to manage all your power mobs.");
                player.sendMessage(ChatColor.YELLOW + "- View and edit existing mobs");
                player.sendMessage(ChatColor.YELLOW + "- Create new power mobs");
                player.sendMessage(ChatColor.YELLOW + "- Configure random mob generation");
                player.sendMessage(ChatColor.YELLOW + "Use the tabs to navigate between sections.");
                break;
            case 49: // Reload Config
                player.closeInventory();
                player.performCommand("powermob reload");
                break;
        }
        return true;
    }

}

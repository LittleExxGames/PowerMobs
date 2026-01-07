package com.powermobs.UI.pages;

import com.powermobs.UI.GUIManager;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.config.PowerManager;
import com.powermobs.config.PowerMobConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Page displaying a list of all power mobs
 */
public class MobListPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54;
    private int pageNumber = 0;

    public MobListPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, INVENTORY_SIZE, ChatColor.DARK_PURPLE + "Power Mobs List");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Mob list.", "ui");
        // Get all power mobs
        PowerManager configManager = pageManager.getPlugin().getConfigManager();
        Map<String, PowerMobConfig> mobs = configManager.getPowerMobs();

        // Setup paging if needed
        int startIndex = pageNumber * 45;
        int endIndex = Math.min(startIndex + 45, mobs.size());

        // Add mob entries
        int slot = 0;
        List<String> mobIds = new ArrayList<>(mobs.keySet());
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= mobIds.size()) break;

            String mobId = mobIds.get(i);
            PowerMobConfig mobConfig = mobs.get(mobId);

            Material icon = guiManager.getMaterialForEntityType(mobConfig.getEntityType());

            ItemStack mobItem = createGuiItem(icon,
                    ChatColor.GREEN + mobId,
                    ChatColor.GRAY + "Type: " + mobConfig.getEntityType(),
                    ChatColor.GRAY + "Health: " + mobConfig.getMinHealth() + "-" + mobConfig.getMaxHealth() + " HP",
                    ChatColor.GRAY + "Click to edit this mob");

            inventory.setItem(slot, mobItem);
            slot++;
        }

        // Add navigation buttons
        if (pageNumber > 0) {
            addNavigationButton(45, false);
        }

        if ((pageNumber + 1) * 45 < mobs.size()) {
            addNavigationButton(53, true);
        }

        // Back button
        addBackButton(49, ChatColor.RED + "Back to Main Menu");
    }

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        if (slot < 45) {
            // A mob was clicked - open its editor
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                guiManager.getCurrentPlayer().setSelectedMobId(name);
                pageManager.navigateTo("mob_editor", true, player);
            }
        } else if (slot == 45 && pageNumber > 0) {
            // Previous page
            pageNumber--;
            build();
        } else if (slot == 53 && inventory.getItem(53) != null) {
            // Next page
            pageNumber++;
            build();
        } else if (slot == 49) {
            // Back to main menu
            pageManager.navigateBack(player);
        }
        return true;
    }
}


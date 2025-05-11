package com.custommobs.UI.pages;

import com.custommobs.UI.framework.AbstractGUIPage;
import com.custommobs.UI.framework.GUIPageManager;
import com.custommobs.config.ConfigManager;
import com.custommobs.config.CustomMobConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Page displaying a list of all custom mobs
 */
public class MobListPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private int pageNumber = 0;

    public MobListPage(GUIPageManager pageManager) {
        super(pageManager, INVENTORY_SIZE, ChatColor.DARK_PURPLE + "Custom Mobs List");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Mob list.");
        // Get all custom mobs
        ConfigManager configManager = pageManager.getPlugin().getConfigManager();
        Map<String, CustomMobConfig> mobs = configManager.getCustomMobs();

        // Setup paging if needed
        int startIndex = pageNumber * 45;
        int endIndex = Math.min(startIndex + 45, mobs.size());

        // Add mob entries
        int slot = 0;
        List<String> mobIds = new ArrayList<>(mobs.keySet());
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= mobIds.size()) break;

            String mobId = mobIds.get(i);
            CustomMobConfig mobConfig = mobs.get(mobId);

            Material icon = getMaterialForEntityType(mobConfig.getEntityType());

            ItemStack mobItem = createGuiItem(icon,
                    ChatColor.GREEN + mobId,
                    ChatColor.GRAY + "Type: " + mobConfig.getEntityType(),
                    ChatColor.GRAY + "Health: " + mobConfig.getHealth(),
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
    public boolean handleClick(Player player, int slot) {
        if (slot < 45) {
            // A mob was clicked - open its editor
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                pageManager.getPlugin().getGuiManager().setSelectedMobId(name);
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

    /**
     * Gets a material that represents an entity type
     */
    private Material getMaterialForEntityType(EntityType type) {
        switch (type) {
            case ZOMBIE:
                return Material.ZOMBIE_HEAD;
            case SKELETON:
                return Material.SKELETON_SKULL;
            case CREEPER:
                return Material.CREEPER_HEAD;
            case SPIDER:
                return Material.SPIDER_EYE;
            case ENDERMAN:
                return Material.ENDER_PEARL;
            case SLIME:
                return Material.SLIME_BALL;
            case WITCH:
                return Material.BREWING_STAND;
            // Add more mappings as needed
            default:
                return Material.SPAWNER;
        }
    }
}


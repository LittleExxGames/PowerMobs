package com.custommobs.UI;

import com.custommobs.UI.framework.GUIPageManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Interface representing a GUI page
 */
public interface GUIPage {

    /**
     * Get the inventory for this page
     * @return The Bukkit inventory
     */
    Inventory getInventory();

    /**
     * Build or rebuild the inventory content
     */
    void build();

    /**
     * Handle click events
     * @param player The player who clicked
     * @param slot The slot that was clicked
     * @return True if the event should be canceled
     */
    boolean handleClick(Player player, int slot);

    /**
     * Get the page manager that manages this page
     * @return The page manager
     */
    GUIPageManager getPageManager();
}


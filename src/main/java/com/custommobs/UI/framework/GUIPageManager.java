package com.custommobs.UI.framework;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.UI.GUIPage;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Manages GUIPages and navigation between them
 */
@Getter
public class GUIPageManager implements InventoryHolder {

    private final CustomMobsPlugin plugin;
    private final Map<String, GUIPage> pages;
    private GUIPage currentPage;
    private final Stack<GUIPage> pageHistory;

    public GUIPageManager(CustomMobsPlugin plugin) {
        this.plugin = plugin;
        this.pages = new HashMap<>();
        this.pageHistory = new Stack<>();
    }

    /**
     * Register a page with this manager
     * @param pageId Unique ID for the page
     * @param page The page to register
     */
    public void registerPage(String pageId, GUIPage page) {
        pages.put(pageId, page);
    }

    /**
     * Navigate to a specific page
     * @param pageId The ID of the page to navigate to
     * @param addToHistory Whether to add the current page to history
     * @return True if navigation was successful
     */
    public boolean navigateTo(String pageId, boolean addToHistory, Player player) {
        if (!pages.containsKey(pageId)) {
            plugin.getLogger().warning("Invalid page ID: " + pageId);
            return false;
        }

        if (currentPage != null && addToHistory) {
            plugin.debug(pageId + " added to page history.");
            pageHistory.push(currentPage);
        }

        currentPage = pages.get(pageId);
        currentPage.build();
        if (player != null && player.isOnline()) {
            player.openInventory(getInventory());
        }
        return true;
    }

    /**
     * Navigate back to the previous page
     * @return True if navigation was successful
     */
    public boolean navigateBack(Player player) {
        if (pageHistory.isEmpty()) {
            return false;
        }

        currentPage = pageHistory.pop();
        currentPage.build();

        if (player != null && player.isOnline()) {
            player.openInventory(getInventory());
        }
        return true;
    }

    /**
     * Prevents the display of a new page when there is no information is displayable.
     * @return
     */
    public boolean navigateBack() {
        if (pageHistory.isEmpty()) {
            return false;
        }
        plugin.debug("No information to display. Navigating back to previous page.");
        currentPage = pageHistory.pop();
        return true;
    }

    /**
     * Handle click events for the current page
     * @param player The player who clicked
     * @param slot The slot that was clicked
     * @return True if the event should be canceled
     */
    public boolean handleClick(Player player, int slot) {
        if (currentPage == null) {
            return true;
        }

        return currentPage.handleClick(player, slot);
    }


    @Override
    public Inventory getInventory() {
        if (currentPage == null) {
            return null;
        }
        return currentPage.getInventory();
    }
}


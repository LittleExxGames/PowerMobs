package com.powermobs.UI.framework;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.GUIPage;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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

    private final PowerMobsPlugin plugin;
    private final Map<String, GUIPage> pages;
    private final Stack<GUIPage> pageHistory;
    private GUIPage currentPage;
    private boolean swappingPage; // gets reset with an inventory close

    public GUIPageManager(PowerMobsPlugin plugin) {
        this.plugin = plugin;
        this.pages = new HashMap<>();
        this.pageHistory = new Stack<>();
    }

    /**
     * Register a page with this manager
     *
     * @param pageId Unique ID for the page
     * @param page   The page to register
     */
    public void registerPage(String pageId, GUIPage page) {
        pages.put(pageId, page);
    }

    /**
     * Navigate to a specific page
     *
     * @param pageId       The ID of the page to navigate to
     * @param addToHistory Whether to add the current page to history
     * @return True if navigation was successful
     */
    public boolean navigateTo(String pageId, boolean addToHistory, Player player) {
        if (!pages.containsKey(pageId)) {
            plugin.getLogger().warning("Invalid page ID: " + pageId);
            return false;
        }

        if (currentPage != null && addToHistory) {
            plugin.debug(pageId + " added to page history.", "ui");
            pageHistory.push(currentPage);
        }
        if (currentPage != null) {
            plugin.debug("Page navigating from: " + currentPage, "ui");
        }
        currentPage = pages.get(pageId);
        plugin.debug("Page navigated to: " + currentPage.toString(), "ui");
        swappingPage = true;
        currentPage.build();
        if (player != null && player.isOnline()) {
            player.openInventory(getInventory());
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> swappingPage = false, 3L);
        return true;
    }

    public boolean isSwappingPage(boolean swapping) {
        swappingPage = swapping;
        return swappingPage;
    }

    public void openCurrentPage(Player player) {
        if (currentPage != null) {
            plugin.debug("Reopening: " + currentPage, "ui");
            if (player != null && player.isOnline()) {
                player.openInventory(getInventory());
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> swappingPage = false, 3L);
        }
    }


    /**
     * Navigate back to the previous page
     *
     * @return True if navigation was successful
     */
    public boolean navigateBack(Player player) {
        if (pageHistory.isEmpty()) {
            plugin.getLogger().warning("No previous page to navigate to.");
            return false;
        }
        plugin.debug("Page navigating back from: " + currentPage.toString(), "ui");
        currentPage = pageHistory.pop();
        plugin.debug("Page navigated to: " + currentPage.toString(), "ui");
        swappingPage = true;
        currentPage.build();
        if (player != null && player.isOnline()) {
            player.openInventory(getInventory());
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> swappingPage = false, 1L);
        return true;
    }

    /**
     * Prevents the display of a new page when there is no information is displayable.
     *
     * @return
     */
    public boolean navigateBack() {
        if (pageHistory.isEmpty()) {
            plugin.getLogger().warning("No previous page to navigate to.");
            return false;
        }
        plugin.debug("Page navigating back from: " + currentPage.toString(), "ui");
        currentPage = pageHistory.pop();
        plugin.debug("Page navigated to: " + currentPage.toString(), "ui");
        return true;
    }

    /**
     * Handle click events for the current page
     *
     * @param player The player who clicked
     * @param slot   The slot that was clicked
     * @return True if the event should be canceled
     */
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        if (currentPage == null) {
            return true;
        }

        return currentPage.handleClick(player, slot, clickType);
    }


    @Override
    public Inventory getInventory() {
        if (currentPage == null) {
            return null;
        }
        return currentPage.getInventory();
    }
}


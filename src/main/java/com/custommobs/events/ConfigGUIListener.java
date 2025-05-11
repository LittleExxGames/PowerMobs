package com.custommobs.events;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.UI.framework.GUIPageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class ConfigGUIListener implements Listener {

    private final CustomMobsPlugin plugin;

    public ConfigGUIListener(CustomMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        // Check if the inventory belongs to the custom GUI
        if (holder instanceof GUIPageManager) {
            event.setCancelled(true); // Prevent taking items from the GUI

            if (event.getCurrentItem() == null) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            plugin.debug("Clicked on slot " + event.getRawSlot() + " in GUI");
            // Forward the click to the GUI handler
            plugin.getGuiManager().handleClick(player, event.getRawSlot());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        // Any cleanup needed when the inventory is closed
        if (holder instanceof GUIPageManager) {
            // Could implement saving of changes here, or other cleanup
        }
    }
}

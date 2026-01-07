package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.framework.GUIPageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfigGUIListener implements Listener {

    private final PowerMobsPlugin plugin;
    private final Map<UUID, Long> lastCloseTime = new HashMap<>();

    public ConfigGUIListener(PowerMobsPlugin plugin) {
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
            plugin.debug("Clicked on slot " + event.getRawSlot() + " in GUI", "ui");
            // Forward the click to the GUI handler
            plugin.getGuiManager().handleClick(player, event.getRawSlot(), event.getClick());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof GUIPageManager manager) {
            Player player = (Player) event.getPlayer();
            UUID playerId = player.getUniqueId();

            long now = System.currentTimeMillis();
            if (lastCloseTime.containsKey(playerId) && now - lastCloseTime.get(playerId) < 200) {
                return; // Ignore duplicate event within 200ms
            }
            lastCloseTime.put(playerId, now);
            try {
                plugin.debug("The inventory closed for player: " + manager.getCurrentPage(), "ui");
                //Temp
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!plugin.getGuiManager().getChatInputHandler().hasActiveSession((Player) event.getPlayer()) && !manager.isSwappingPage()) {
                        plugin.getGuiManager().setCurrentPlayer(null);
                        manager.isSwappingPage(false);
                        plugin.debug("Inventory closed for player: " + event.getPlayer().getName() + ", resetting GUI state.", "ui");
                    }
                }, 1L);
            } catch (Exception e) {
                plugin.getGuiManager().setCurrentPlayer(null);
                manager.isSwappingPage(false);
                plugin.debug("Inventory closed for player: " + event.getPlayer().getName() + ", resetting GUI state.", "ui");
            }

        }
    }
}

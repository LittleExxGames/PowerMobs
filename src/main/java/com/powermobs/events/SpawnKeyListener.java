package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnKeyListener implements Listener {
    private final PowerMobsPlugin plugin;


    public SpawnKeyListener(PowerMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        String keyId = plugin.getSpawnKeyManager().getSpawnKeyId(item);
        if (keyId == null) {
            return;
        }
        event.setCancelled(true);

        if (!event.getPlayer().hasPermission("powermobs.spawnkey.use")) {
            event.getPlayer().sendMessage("You do not have permission to use spawn keys.");
            return;
        }

        boolean used = plugin.getSpawnKeyManager().triggerSpawnKey(keyId, event);
        if (used) {
            item.setAmount(item.getAmount() - 1);
        }
    }
}

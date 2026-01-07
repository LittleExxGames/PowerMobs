package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.config.SpawnBlockerManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

/**
 * Handles spawn blocker placement and removal events
 */
@RequiredArgsConstructor
public class SpawnBlockerListener implements Listener {

    private final PowerMobsPlugin plugin;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        Block block = event.getBlockPlaced();
        Location location = block.getLocation();

        // Check if the placed item is a custom spawn blocker
        String blockerId = plugin.getSpawnBlockerManager().getSpawnBlockerId(item);
        if (blockerId != null) {
            SpawnBlockerManager.SpawnBlockerConfig config = plugin.getSpawnBlockerManager().getSpawnBlockerConfig(blockerId);
            if (config != null && config.enabled()) {
                plugin.getSpawnBlockerManager().registerSpawnBlocker(location, blockerId);

                // Align with vanilla notifier permission
                if (player.hasPermission("powermobs.spawnblocker.notify")) {
                    int chunkSize = config.chunkRange() * 2 - 1;
                    player.sendMessage(ChatColor.GREEN + "Spawn blocker placed! " +
                            ChatColor.YELLOW + "Range: " + chunkSize + "x" + chunkSize + " chunks");
                }
                plugin.debug("Player " + player.getName() + " placed spawn blocker '" + blockerId + "' at " + location, "spawn_blockers");
                return;
            }
        }

        // Check if the placed block is a vanilla spawn blocker
        Material placedMaterial = block.getType();
        for (String id : plugin.getSpawnBlockerManager().getSpawnBlockerIds()) {
            SpawnBlockerManager.SpawnBlockerConfig config = plugin.getSpawnBlockerManager().getSpawnBlockerConfig(id);
            if (config != null && config.enabled() &&
                    config.type().equals("VANILLA") &&
                    config.material() == placedMaterial) {

                plugin.getSpawnBlockerManager().registerSpawnBlocker(location, id);

                if (player.hasPermission("powermobs.spawnblocker.notify")) {
                    int chunkSize = config.chunkRange() * 2 - 1;
                    player.sendMessage(ChatColor.GREEN + "Spawn blocker activated! " +
                            ChatColor.YELLOW + "Range: " + chunkSize + "x" + chunkSize + " chunks");
                }
                plugin.debug("Player " + player.getName() + " placed vanilla spawn blocker '" + id + "' at " + location, "spawn_blockers");
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        // Scan newly loaded chunks for spawn blockers
        plugin.getSpawnBlockerManager().loadChunkBlockersFromPdc(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Location location = block.getLocation();

        SpawnBlockerManager.SpawnBlockerData blockerData = plugin.getSpawnBlockerManager().getSpawnBlockerAt(location);
        if (blockerData == null) {
            return;
        }
        plugin.getSpawnBlockerManager().removeSpawnBlocker(location);
        if (event.isDropItems() && blockerData.config().type().equals("CUSTOM")) {
            event.setDropItems(false);
            block.getWorld().dropItemNaturally( block.getLocation(), plugin.getSpawnBlockerManager().createSpawnBlockerItem(blockerData.blockerId()));
        }
        Player player = event.getPlayer();
        if (player.hasPermission("powermobs.spawnblocker.notify")) {
            player.sendMessage(ChatColor.RED + "Spawn blocker removed!");
        }
        plugin.debug("Player " + player.getName() + " broke spawn blocker at " + location, "spawn_blockers");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void entityExplodeEvent(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        for (Block block : event.blockList()) {
            Location location = block.getLocation();
            if (plugin.getSpawnBlockerManager().hasSpawnBlockerAt(location)) {
                plugin.getSpawnBlockerManager().removeSpawnBlocker(location);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void blockExplodeEvent(BlockExplodeEvent event) {
        if (event.isCancelled()) return;

        Location location = event.getBlock().getLocation();
        if (plugin.getSpawnBlockerManager().hasSpawnBlockerAt(location)) {
            plugin.getSpawnBlockerManager().removeSpawnBlocker(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void blockBurnEvent(BlockBurnEvent event) {
        if (event.isCancelled()) return;

        Location location = event.getBlock().getLocation();
        if (plugin.getSpawnBlockerManager().hasSpawnBlockerAt(location)) {
            plugin.getSpawnBlockerManager().removeSpawnBlocker(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void blockFormEvent(BlockFormEvent event) {
        if (event.isCancelled()) return;

        Location location = event.getBlock().getLocation();
        if (plugin.getSpawnBlockerManager().hasSpawnBlockerAt(location)) {
            plugin.getSpawnBlockerManager().removeSpawnBlocker(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void entityChangeBlockEvent(EntityChangeBlockEvent event) {
        if (event.isCancelled()) return;

        Location location = event.getBlock().getLocation();
        if (plugin.getSpawnBlockerManager().hasSpawnBlockerAt(location)) {
            plugin.getSpawnBlockerManager().removeSpawnBlocker(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void blockSpreadEvent(BlockSpreadEvent event) {
        if (event.isCancelled()) return;

        Location location = event.getBlock().getLocation();
        if (plugin.getSpawnBlockerManager().hasSpawnBlockerAt(location)) {
            plugin.getSpawnBlockerManager().removeSpawnBlocker(location);
        }
    }
}
package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.stats.CachedStats;
import com.powermobs.stats.StatsManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/*
Listens for a player connection to update stored info about players
 */
@RequiredArgsConstructor
public class PlayerConnectionListener implements Listener {

    private final PowerMobsPlugin plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        StatsManager db = plugin.getStatsManager();

        db.loadSpecificPlayerStats(uuid).thenAccept(mobStatsMap -> {
            if (mobStatsMap != null && !mobStatsMap.isEmpty()) {
                CachedStats.addJoinedPlayer(uuid, mobStatsMap);
            }
        });
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        CachedStats.removePlayer(uuid, true);
    }
}

package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

/**
 * Handles power mob death events
 */
@RequiredArgsConstructor
public class MobDeathListener implements Listener {

    private final PowerMobsPlugin plugin;

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        PowerMob powerMob = PowerMob.getFromEntity(this.plugin, entity);

        if (powerMob == null) {
            return;
        }

        // Clear default drops
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Get the killer player (if any)
        Player killer = entity.getKiller();
        plugin.debug("Killer: " + killer, "mob_combat");

        // Process drops using the drop handler
        plugin.getDropHandler().processDrops(powerMob, killer, entity.getLocation());

        // Clean up tracking data AFTER processing drops
        plugin.getDamageTracker().cleanupMob(powerMob.getEntityUuid());

        // Unregister the power mob
        powerMob.remove();
        this.plugin.getPowerMobManager().unregisterPowerMob(powerMob);
    }

}
package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listener for damage and death events to track player damage
 */
@RequiredArgsConstructor
public class DamageTrackingListener implements Listener {

    private final PowerMobsPlugin plugin;

    /**
     * Handles entity damage events for tracking damage done to power mobs
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        // Check if the entity is a power mob
        PowerMob powerMob = PowerMob.getFromEntity(plugin, victim);
        if (powerMob == null) {
            return;
        }

        // Register the damage with our tracker
        plugin.getDamageTracker().registerDamage(powerMob, event);
    }

    /**
     * Handles entity death events related to power mobs
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

    }
}

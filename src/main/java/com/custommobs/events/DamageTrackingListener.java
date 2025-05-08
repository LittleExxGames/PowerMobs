package com.custommobs.events;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
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

    private final CustomMobsPlugin plugin;

    /**
     * Handles entity damage events for tracking damage done to custom mobs
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity victim = (LivingEntity) event.getEntity();

        // Check if the entity is a custom mob
        CustomMob customMob = CustomMob.getFromEntity(plugin, victim);
        if (customMob == null) {
            return;
        }

        // Register the damage with our tracker
        plugin.getDamageTracker().registerDamage(customMob, event);
    }

    /**
     * Handles entity death events to clean up tracking data
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Check if the entity is a custom mob
        CustomMob customMob = CustomMob.getFromEntity(plugin, entity);
        if (customMob == null) {
            return;
        }

        // Clean up tracking data
        plugin.getDamageTracker().cleanupMob(customMob.getEntityUuid());
    }
}

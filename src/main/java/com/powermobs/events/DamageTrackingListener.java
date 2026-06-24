package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Objects;

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

        PowerMob powerMob = PowerMob.getFromEntity(plugin, victim);
        if (powerMob == null) {
            return;
        }
        if (Objects.equals(powerMob.getId(), "summoned-minion")){
            return;
        }

        plugin.getDamageTracker().registerDamage(powerMob, event);
    }

}

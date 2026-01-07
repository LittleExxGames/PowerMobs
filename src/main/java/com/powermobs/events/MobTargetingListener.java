package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.utils.MobTargetingUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/**
 * Listener for entity targeting events
 */
@RequiredArgsConstructor
public class MobTargetingListener implements Listener {

    private final PowerMobsPlugin plugin;

    /**
     * Handles entity targeting events
     */
    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Entity source = event.getEntity();
        LivingEntity target = event.getTarget();

        // If either entity is null, ignore
        if (source == null || target == null) {
            return;
        }

        // Check if this targeting should be allowed
        if (!MobTargetingUtil.shouldAllowTargeting(plugin, source, target)) {
            event.setCancelled(true);
        }
    }
}

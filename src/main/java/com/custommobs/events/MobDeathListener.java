package com.custommobs.events;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

/**
 * Handles custom mob death events
 */
@RequiredArgsConstructor
public class MobDeathListener implements Listener {

    private final CustomMobsPlugin plugin;
    private final Random random = new Random();
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        CustomMob customMob = CustomMob.getFromEntity(this.plugin, entity);
        
        if (customMob == null) {
            return;
        }
        
        // Clear default drops
        event.getDrops().clear();
        event.setDroppedExp(0);


        // Get the killer player (if any)
        Player killer = entity.getKiller();
        plugin.debug("Killer and it happened!: " + killer);
        // Process drops using the drop handler
        plugin.getDropHandler().processDrops(customMob, killer, entity.getLocation());

        // Clean up tracking data AFTER processing drops
        plugin.getDamageTracker().cleanupMob(customMob.getEntityUuid());

        // Unregister the custom mob
        customMob.remove();
        this.plugin.getCustomMobManager().unregisterCustomMob(customMob);
    }

}
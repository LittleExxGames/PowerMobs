package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/*Tracks the player death to include in the stat tracking*/
@RequiredArgsConstructor
public class PlayerDeathListener implements Listener {

    private final PowerMobsPlugin plugin;

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        LivingEntity entity = (LivingEntity)event.getDamageSource().getCausingEntity();
        PowerMob powerMob = PowerMob.getFromEntity(this.plugin, entity);

        if (powerMob == null) {
            return;
        }
        plugin.getDamageTracker().calculatePlayerDeathInvolvement(powerMob, event.getPlayer());
    }
}

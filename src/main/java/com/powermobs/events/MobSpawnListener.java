package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.timing.SpawnContext;
import com.powermobs.utils.HostileEntityTypes;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Listens for mob spawns and replaces them with power mobs
 */
@RequiredArgsConstructor
public class MobSpawnListener implements Listener {

    private final PowerMobsPlugin plugin;


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        LivingEntity entity = event.getEntity();

        // Skip if the entity is already a power mob
        if (PowerMob.isPowerMob(this.plugin, entity) || entity.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            this.plugin.debug("Skipping - already a custom mob", "mob_spawning");
            return;
        }

        // Skip if the spawn is from another plugin or command
        if (entity.hasMetadata("NPC") || entity.hasMetadata("shopkeeper")) {
            return;
        }

        try {
            HostileEntityTypes.valueOf(entity.getType().name());
        } catch (IllegalArgumentException e) {
            return;
        }

        // Check if spawning is blocked by a spawn blocker
        Location location = entity.getLocation();
        if (plugin.getSpawnBlockerManager().isSpawnBlocked(location)) {
            plugin.debug("Power mob spawn blocked at " + location + " by spawn blocker", "mob_spawning");
            return; // Don't spawn power mob, but allow vanilla spawn to continue
        }

        // Global Random chance to become a power mob
        if (Math.random() > this.plugin.getConfigManager().getSpawnChance()) {
            return;
        }

        // Check if we should replace this mob
        long currentTime = System.currentTimeMillis();

        String configId = this.plugin.getPowerMobManager().getFactory().findValidPowerMobConfig(entity, location, event.getSpawnReason());
        if (configId == null) {
            String at = "World: " + location.getWorld() + " -  Position: " + location.getBlockX() + "x, " + location.getBlockY() + "y, " + location.getBlockZ() + "z";
            this.plugin.debug("No suitable conditions found for mob " + entity.getType() + " at " + at + " - skipping spawn", "mob_spawning");
            return;
        }

        // Create spawn context for this attempt
        SpawnContext context = SpawnContext.builder()
                .mobConfigId(configId)
                .location(location)
                .source(SpawnContext.SpawnSource.NATURAL)
                .forceBypass(false)
                .delay(getSpawnDelay(configId))
                .spawnTime(currentTime)
                .spawnReason(event.getSpawnReason())
                .build();

        // Check if spawn is allowed based on timers
        boolean bypassTimer = context.shouldBypass(this.plugin.getConfigManager());
        if (!this.plugin.getSpawnTimerManager().canSpawn(configId, location, currentTime, bypassTimer)) {
            this.plugin.debug("Spawn blocked by timer for " + configId + " at " + location, "mob_spawning");
            return; // Don't spawn, timer still active
        }

        this.plugin.debug("Replacing " + entity.getType() + " with power mob " + configId +
                " (from " + event.getSpawnReason().name() + ")", "mob_spawning");


        // Cancel the original vanilla spawn
        event.setCancelled(true);

        // Create the power mob
        PowerMob powerMob = this.plugin.getPowerMobManager().createAndRegisterPowerMob(entity, configId);
        if (powerMob == null) {
            return;
        }

        // Record the spawn in timer system if needed
        boolean shouldSetTimer = context.shouldSetTimer(this.plugin.getConfigManager());
        this.plugin.getSpawnTimerManager().recordSpawn(configId, context, !shouldSetTimer);

        // Show spawn effect
        if (this.plugin.getConfigManager().isSpawnEffect()) {
            location.getWorld().spawnParticle(
                    Particle.EXPLOSION_EMITTER,
                    location.clone().add(0, 1, 0),
                    1,
                    0,
                    0,
                    0,
                    0
            );
        }

        // Announce spawn
        if (this.plugin.getConfigManager().isSpawnAnnouncements()) {
            for (Player player : location.getWorld().getPlayers()) {
                if (player.hasPermission("powermobs.announce") &&
                        player.getLocation().distance(location) <= 50) {
                    player.sendMessage(ChatColor.RED + "[PowerMobs] " + ChatColor.GOLD +
                            "A " + powerMob.getEntity().getCustomName() + ChatColor.GOLD + " has spawned nearby!");
                }
            }
        }
    }

    /**
     * Gets the spawn delay for a given config ID
     */
    private long getSpawnDelay(String configId) {
        if ("random".equals(configId)) {
            return this.plugin.getConfigManager().getRandomMobConfig().getSpawnCondition().getActualSpawnDelay() * 1000L;
        } else {
            return this.plugin.getConfigManager().getPowerMob(configId).getSpawnCondition().getActualSpawnDelay() * 1000L;
        }
    }

}
package com.powermobs.mobs.timing;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.config.PowerManager;
import com.powermobs.mobs.PowerMob;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages spawn timers for power mobs and random mobs
 * Supports both global and location-based timing systems
 */
@RequiredArgsConstructor
@Getter
public class SpawnTimerManager {

    // Special key for random mobs
    private static final String RANDOM_MOB_KEY = "Random_Mob";
    private final PowerMobsPlugin plugin;
    // Global timers - one timer per mob config ID
    private final Map<String, SpawnContext> globalTimers = new ConcurrentHashMap<>();
    // Location-based timers - keyed by chunk coordinate and mob config ID
    private final Map<String, SpawnContext> locationTimers = new ConcurrentHashMap<>();
    // NEW: Despawn timer tracking - maps PowerMob UUID to despawn time
    private final Map<UUID, BukkitTask> activeDespawnTasks = new ConcurrentHashMap<>();

    /**
     * Initializes the timer manager with configuration
     */
    public void initialize() {
        startCleanupTask();
        restoreDespawnTasks();
    }

    /**
     * Checks if a mob can spawn based on timer restrictions
     *
     * @param mobConfigId The mob configuration ID ("random" for random mobs)
     * @param location    Location of the purposed mob spawn
     * @param spawnTime   The current time of when the mob tried to spawn
     * @param bypassTimer Whether to bypass timer restrictions
     * @return True if the mob can spawn, false if still on cooldown
     */
    public boolean canSpawn(String mobConfigId, Location location, long spawnTime, boolean bypassTimer) {
        PowerManager configManager = plugin.getConfigManager();
        if (!configManager.isSpawnTimersEnabled()) {
            return true;
        }

        if (bypassTimer) {
            plugin.debug("Bypassing timer for mob: " + mobConfigId, "mob_spawning");
            return true;
        }
        if (configManager.isSpawnLocationBased()) {
            // Get all past spawn contexts for this mob type
            List<SpawnContext> matchingContexts = locationTimers.keySet().stream()
                    .filter(key -> key.startsWith(mobConfigId + ":"))
                    .map(locationTimers::get)
                    .toList();

            // Filter contexts that are within the location distance
            List<SpawnContext> nearbyContexts = matchingContexts.stream()
                    .filter(pastContext -> pastContext.getLocation().distance(location) <= configManager.getLocationDistance())
                    .toList();

            // Check if any nearby contexts still have active cooldowns
            for (SpawnContext nearbyContext : nearbyContexts) {
                if (!validTime(spawnTime, nearbyContext.getSpawnTime(), nearbyContext.getDelay())) {
                    // There's a nearby spawn that's still on cooldown
                    plugin.debug("Spawn blocked: nearby location still on cooldown", "mob_spawning");
                    return false;
                }
            }
            return true;
        } else {
            SpawnContext existingContext = globalTimers.get(mobConfigId);
            if (existingContext != null) {
                return validTime(spawnTime, existingContext.getSpawnTime(), existingContext.getDelay());
            } else {
                return true;
            }
        }
    }

    /**
     * Compares passed time against the required delay
     *
     * @param currentSpawnTime
     * @param lastSpawnTime
     * @param requiredDelay
     * @return
     */
    private boolean validTime(long currentSpawnTime, long lastSpawnTime, long requiredDelay) {
        long timeSinceSpawn = currentSpawnTime - lastSpawnTime;
        return timeSinceSpawn >= requiredDelay;
    }

    /**
     * Records a spawn event, updating the appropriate timer
     *
     * @param mobConfigId The mob configuration ID
     * @param context     The spawn context
     * @param bypassTime  Whether this spawn bypassed timer restrictions
     */
    public void recordSpawn(String mobConfigId, SpawnContext context, boolean bypassTime) {
        if (bypassTime || !plugin.getConfigManager().isSpawnTimersEnabled()) {
            return;
        }
        if (plugin.getConfigManager().isSpawnLocationBased()) {
            String locationKey = getLocationKey(mobConfigId, context.getLocation());
            locationTimers.put(locationKey, context);
        } else {
            globalTimers.put(mobConfigId, context);
        }
        plugin.debug("Recorded spawn for " + mobConfigId + " at " + context.getSpawnTime(), "mob_spawning");
    }

    /**
     * Gets the remaining cooldown time for a specific mob and location
     *
     * @param mobConfigId The mob configuration ID
     * @param location    The spawn location (null for global)
     * @return Remaining cooldown in milliseconds, 0 if no cooldown
     */
    public long getRemainingCooldown(String mobConfigId, Location location) {
        if (!plugin.getConfigManager().isSpawnTimersEnabled()) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();

        if (plugin.getConfigManager().isSpawnLocationBased() && location != null) {
            // Get all past spawn contexts for this mob type
            List<SpawnContext> matchingContexts = locationTimers.keySet().stream()
                    .filter(key -> key.startsWith(mobConfigId + ":"))
                    .map(locationTimers::get)
                    .toList();

            // Filter contexts that are within the location distance
            List<SpawnContext> nearbyContexts = matchingContexts.stream()
                    .filter(pastContext -> pastContext.getLocation().distance(location) <= plugin.getConfigManager().getLocationDistance())
                    .toList();

            // Find the longest remaining cooldown among nearby contexts
            long maxRemainingCooldown = 0;
            for (SpawnContext nearbyContext : nearbyContexts) {
                long timeSinceSpawn = currentTime - nearbyContext.getSpawnTime();
                long requiredDelay = nearbyContext.getDelay();
                long remainingCooldown = Math.max(0, requiredDelay - timeSinceSpawn);

                if (remainingCooldown > maxRemainingCooldown) {
                    maxRemainingCooldown = remainingCooldown;
                }
            }

            return maxRemainingCooldown;

        } else {
            // Global timer check
            SpawnContext context = globalTimers.get(mobConfigId);
            if (context == null) {
                return 0;
            }

            long timeSinceSpawn = currentTime - context.getSpawnTime();
            long requiredDelay = context.getDelay();

            return Math.max(0, requiredDelay - timeSinceSpawn);
        }
    }

    /**
     * Clears all spawn timers (useful for reloads or resets)
     */
    public void clearAllSpawnTimers() {
        globalTimers.clear();
        locationTimers.clear();
        plugin.debug("Cleared all spawn timers", "mob_spawning");
    }

    /**
     * Clears all despawn timers (useful for reloads or resets)
     */
    public void clearAllDespawnTimers() {
        for (BukkitTask task : activeDespawnTasks.values()) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        activeDespawnTasks.clear();
        plugin.debug("Cancelled all despawn tasks", "mob_spawning");
    }

    /**
     * Clears timers for a specific mob configuration
     *
     * @param mobConfigId The mob configuration ID to clear
     */
    public void clearTimersForMob(String mobConfigId) {
        globalTimers.remove(mobConfigId);

        // Remove location-based timers for this mob
        locationTimers.entrySet().removeIf(entry ->
                entry.getKey().startsWith(mobConfigId + ":"));

        plugin.debug("Cleared timers for mob: " + mobConfigId, "mob_spawning");
    }

    /**
     * Gets debug information about current timers
     *
     * @return Debug information string
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Spawn Timer Manager Debug ===\n");
        sb.append("Timer Enabled: ").append(plugin.getConfigManager().isSpawnTimersEnabled()).append("\n");
        sb.append("Location Based: ").append(plugin.getConfigManager().isSpawnLocationBased()).append("\n");
        sb.append("Distance Limiter: ").append(plugin.getConfigManager().getLocationDistance()).append("\n");
        sb.append("Global Timers: ").append(globalTimers.size()).append("\n");
        sb.append("Location Timers: ").append(locationTimers.size()).append("\n");
        sb.append("Active Despawn Tasks: ").append(activeDespawnTasks.size()).append("\n");

        long currentTime = System.currentTimeMillis();

        if (!globalTimers.isEmpty()) {
            sb.append("\nGlobal Timers:\n");
            for (Map.Entry<String, SpawnContext> entry : globalTimers.entrySet()) {
                long remaining = getRemainingCooldown(entry.getKey(), null);
                sb.append("  ").append(entry.getKey()).append(": ")
                        .append(remaining / 1000).append("s remaining\n");
            }
        }

        if (!activeDespawnTasks.isEmpty()) {
            sb.append("\nActive Despawn Tasks:\n");
            for (UUID mobUuid : activeDespawnTasks.keySet()) {
                PowerMob mob = plugin.getPowerMobManager().getPowerMob(mobUuid);
                if (mob != null) {
                    long remaining = getRemainingDespawnTime(mob);
                    sb.append("  ").append(mob.getId()).append(": ")
                            .append(remaining / 1000).append("s remaining\n");
                }
            }
        }

        return sb.toString();
    }

    private String getLocationKey(String mobConfigId, Location location) {
        int blockRange = plugin.getConfigManager().getLocationDistance();

        // Normalize block coordinates to the range grid
        int normalizedX = (location.getBlockX() / blockRange) * blockRange;
        int normalizedZ = (location.getBlockZ() / blockRange) * blockRange;

        return mobConfigId + ":" + location.getWorld().getName() +
                ":" + normalizedX + ":" + normalizedZ;
    }

    private long getSpawnDelay(String mobConfigId) {
        if ("random".equals(mobConfigId)) {
            return plugin.getConfigManager().getRandomMobConfig().getSpawnCondition().getActualSpawnDelay() * 1000L;
        }
        return plugin.getConfigManager().getPowerMob(mobConfigId).getSpawnCondition().getActualSpawnDelay() * 1000L;
    }


    /**
     * Schedules a despawn task for a power mob
     *
     * @param powerMob           The power mob to schedule for despawn
     * @param despawnTimeSeconds Time in seconds until despawn (0 = no despawn)
     */
    public void scheduleDespawnTask(PowerMob powerMob, int despawnTimeSeconds) {
        if (despawnTimeSeconds <= 0) {
            return; // No despawn timer
        }

        // Store despawn time in persistent data for restart recovery
        long despawnTime = System.currentTimeMillis() + (despawnTimeSeconds * 1000L);
        powerMob.getEntity().getPersistentDataContainer().set(
                new NamespacedKey(plugin, "despawn_time"),
                PersistentDataType.LONG,
                despawnTime
        );

        // Schedule the actual despawn task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (powerMob.isValid()) {
                    plugin.debug("Despawning mob " + powerMob.getId() + " after " + despawnTimeSeconds + " seconds", "mob_spawning");
                    powerMob.getEntity().remove();
                    plugin.getPowerMobManager().unregisterPowerMob(powerMob);
                }
                // Remove from active tasks tracking
                activeDespawnTasks.remove(powerMob.getPowerMobUuid());
            }
        }.runTaskLater(plugin, despawnTimeSeconds * 20L); // Convert seconds to ticks

        // Track the task for potential cancellation
        activeDespawnTasks.put(powerMob.getPowerMobUuid(), task);

        plugin.debug("Scheduled despawn for mob " + powerMob.getId() + " in " + despawnTimeSeconds + " seconds", "mob_spawning");
    }

    /**
     * Cancels a scheduled despawn task for a power mob
     *
     * @param powerMob The power mob to cancel despawn for
     */
    public void cancelDespawnTask(PowerMob powerMob) {
        UUID mobUuid = powerMob.getPowerMobUuid();

        // Cancel the scheduled task
        BukkitTask task = activeDespawnTasks.remove(mobUuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
            plugin.debug("Cancelled despawn task for mob: " + powerMob.getId(), "mob_spawning");
        }

        // Remove persistent data
        powerMob.getEntity().getPersistentDataContainer().remove(
                new NamespacedKey(plugin, "despawn_time")
        );
    }

    /**
     * Gets the remaining despawn time for a power mob
     *
     * @param powerMob The power mob
     * @return Milliseconds until despawn, -1 if no timer
     */
    public long getRemainingDespawnTime(PowerMob powerMob) {
        PersistentDataContainer pdc = powerMob.getEntity().getPersistentDataContainer();
        NamespacedKey despawnKey = new NamespacedKey(plugin, "despawn_time");

        if (!pdc.has(despawnKey, PersistentDataType.LONG)) {
            return -1; // No despawn timer
        }

        Long despawnTime = pdc.get(despawnKey, PersistentDataType.LONG);
        if (despawnTime == null) {
            return -1;
        }

        return Math.max(0, despawnTime - System.currentTimeMillis());
    }

    /**
     * Restores despawn tasks after server restart by checking all existing power mobs
     */
    private void restoreDespawnTasks() {
        int restored = 0;
        int expired = 0;

        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.entity.LivingEntity entity : world.getLivingEntities()) {
                if (PowerMob.isPowerMob(plugin, entity)) {
                    PowerMob powerMob = PowerMob.getFromEntity(plugin, entity);
                    if (powerMob == null) {
                        continue;
                    }

                    // Re-register the mob with the manager
                    plugin.getPowerMobManager().registerPowerMob(powerMob);

                    // Check for despawn timer in persistent data
                    PersistentDataContainer pdc = entity.getPersistentDataContainer();
                    NamespacedKey despawnKey = new NamespacedKey(plugin, "despawn_time");

                    if (pdc.has(despawnKey, PersistentDataType.LONG)) {
                        Long despawnTime = pdc.get(despawnKey, PersistentDataType.LONG);
                        if (despawnTime != null) {
                            long currentTime = System.currentTimeMillis();

                            if (despawnTime <= currentTime) {
                                // Timer expired while server was offline, despawn immediately
                                plugin.debug("Mob " + powerMob.getId() + " timer expired while offline, despawning...", "mob_spawning");
                                entity.remove();
                                expired++;
                            } else {
                                // Timer still valid, reschedule with remaining time
                                long remainingTime = despawnTime - currentTime;
                                long remainingTicks = remainingTime / 50; // Convert ms to ticks

                                BukkitTask task = new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (powerMob.isValid()) {
                                            plugin.debug("Despawning mob " + powerMob.getId() + " (restored task)", "mob_spawning");
                                            powerMob.getEntity().remove();
                                            plugin.getPowerMobManager().unregisterPowerMob(powerMob);
                                        }
                                        activeDespawnTasks.remove(powerMob.getPowerMobUuid());
                                    }
                                }.runTaskLater(plugin, remainingTicks);

                                activeDespawnTasks.put(powerMob.getPowerMobUuid(), task);
                                restored++;

                                plugin.debug("Restored despawn task for mob " + powerMob.getId() +
                                        " with " + (remainingTime / 1000) + " seconds remaining", "mob_spawning");
                            }
                        }
                    }
                }
            }
        }

        if (restored > 0 || expired > 0) {
            plugin.getLogger().info("Restored " + restored + " despawn tasks, removed " + expired + " expired mobs");
        }
    }

    /**
     * Cleanup method for shutdown - cancel all active despawn tasks
     */
    public void shutdown() {
        for (BukkitTask task : activeDespawnTasks.values()) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        activeDespawnTasks.clear();
        plugin.debug("Cancelled all active despawn tasks", "cleanup");
    }


    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredTimers();
            }
        }.runTaskTimerAsynchronously(plugin, 20 * 60 * 5, 20 * 60 * 5); // Every 5 minutes
    }

    private void cleanupExpiredTimers() {
        long currentTime = System.currentTimeMillis();

        boolean cleanedSpawnTimers = false;
        boolean cleanedDespawnTasks = false;

        // Clean up global timers - remove only if their cooldown has fully elapsed
        cleanedSpawnTimers |= globalTimers.entrySet().removeIf(entry -> {
            SpawnContext context = entry.getValue();
            long timeSinceSpawn = currentTime - context.getSpawnTime();
            long requiredDelay = context.getDelay();
            return timeSinceSpawn >= requiredDelay;
        });

        // Clean up location timers - same logic
        cleanedSpawnTimers |= locationTimers.entrySet().removeIf(entry -> {
            SpawnContext context = entry.getValue();
            long timeSinceSpawn = currentTime - context.getSpawnTime();
            long requiredDelay = context.getDelay();
            return timeSinceSpawn >= requiredDelay;
        });

        // Clean up despawn tasks for missing/invalid mobs
        for (Map.Entry<UUID, BukkitTask> entry : activeDespawnTasks.entrySet()) {
            UUID uuid = entry.getKey();
            BukkitTask task = entry.getValue();
            PowerMob mob = plugin.getPowerMobManager().getPowerMob(uuid);

            boolean invalid = (mob == null)
                    || !mob.isValid()
                    || mob.getEntity() == null
                    || !mob.getEntity().isValid()
                    || mob.getEntity().isDead();

            if (invalid) {
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }
                activeDespawnTasks.remove(uuid);
                cleanedDespawnTasks = true;
            }
        }

        if (cleanedSpawnTimers) {
            plugin.debug("Cleaned up expired spawn timers where cooldowns have elapsed", "cleanup");
        }
        if (cleanedDespawnTasks) {
            plugin.debug("Cleaned up orphaned despawn tasks", "cleanup");
        }
    }
}
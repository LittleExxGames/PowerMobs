package com.powermobs.mobs.timing;

import com.powermobs.config.PowerManager;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Context information for a spawn event, including bypass conditions
 */
@Getter
@Builder
public class SpawnContext {

    private final String mobConfigId;
    private final Location location;
    private final SpawnSource source;
    private final boolean forceBypass;
    private final long delay;
    private final long spawnTime;

    // Additional context data
    private final CreatureSpawnEvent.SpawnReason spawnReason;
    private final String spawnerType; // For custom spawners
    private final String itemUsed;    // For spawn items
    private final String commandUsed; // For commands

    /**
     * Determines if this spawn should bypass timer restrictions based on config settings
     *
     * @param config The config manager to check bypass settings
     * @return True if bypass should be applied
     */
    public boolean shouldBypass(PowerManager config) {
        if (forceBypass) {
            return true;
        }

        return switch (source) {
            case ITEM -> config.isItemBypass();
            case SPAWNER -> config.isSpawnerBypass();
            case COMMAND -> true; // Commands always bypass timers
            default -> false;
        };
    }

    /**
     * Determines if timer should be set after spawn
     *
     * @param config The config manager to check timer settings
     * @return True if timer should be recorded
     */
    public boolean shouldSetTimer(PowerManager config) {
        // If timers are disabled globally, don't set timers
        if (!config.isSpawnTimersEnabled()) {
            return false;
        }

        // Commands never set timers
        if (source == SpawnSource.COMMAND) {
            return false;
        }

        // For natural spawns, always set timer if enabled
        if (source == SpawnSource.NATURAL) {
            return true;
        }

        // For spawner spawns, set timer only if spawner bypass is disabled
        if (source == SpawnSource.SPAWNER) {
            return !config.isSpawnerBypass();
        }

        // For item spawns, set timer only if item bypass is disabled
        if (source == SpawnSource.ITEM) {
            return !config.isItemBypass();
        }
        return false;
    }

    /**
     * Sources that can trigger mob spawns
     */
    public enum SpawnSource {
        NATURAL,   // Natural world spawning
        SPAWNER,   // Spawner block or custom spawner
        ITEM,      // Custom spawn item
        COMMAND    // Command or admin action
    }
}
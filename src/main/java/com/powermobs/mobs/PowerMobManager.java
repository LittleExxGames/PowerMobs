package com.powermobs.mobs;

import com.powermobs.PowerMobsPlugin;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all power mobs in the game
 */
public class PowerMobManager {

    private final PowerMobsPlugin plugin;
    private final Map<UUID, PowerMob> powerMobs = new HashMap<>();

    @Getter
    private final NamespacedKey powerMobKey;

    @Getter
    private final PowerMobFactory factory;

    /**
     * Creates a new power mob manager
     *
     * @param plugin The plugin instance
     */
    public PowerMobManager(PowerMobsPlugin plugin) {
        this.plugin = plugin;
        this.powerMobKey = new NamespacedKey(plugin, "power_mob");
        this.factory = new PowerMobFactory(plugin);
    }

    /**
     * Registers a power mob
     *
     * @param powerMob The power mob to register
     */
    public void registerPowerMob(PowerMob powerMob) {
        this.powerMobs.put(powerMob.getPowerMobUuid(), powerMob);
    }

    /**
     * Unregisters a power mob
     *
     * @param powerMob The power mob to unregister
     */
    public void unregisterPowerMob(PowerMob powerMob) {
        this.powerMobs.remove(powerMob.getPowerMobUuid());
    }

    /**
     * Gets a power mob by its UUID
     *
     * @param uuid The power mob UUID
     * @return The power mob, or null if not found
     */
    public PowerMob getPowerMob(UUID uuid) {
        return this.powerMobs.get(uuid);
    }

    /**
     * Gets a power mob by its entity UUID
     *
     * @param entityUuid The entity UUID
     * @return The power mob, or null if not found
     */
    public PowerMob getPowerMobByEntityUuid(UUID entityUuid) {
        for (PowerMob powerMob : this.powerMobs.values()) {
            if (powerMob.getEntityUuid().equals(entityUuid)) {
                return powerMob;
            }
        }
        return null;
    }

    /**
     * Gets all power mobs
     *
     * @return An unmodifiable map of all power mobs
     */
    public Map<UUID, PowerMob> getPowerMobs() {
        return Collections.unmodifiableMap(this.powerMobs);
    }

    /**
     * Creates and registers a new power mob
     *
     * @param entity   The entity to enhance
     * @param configId The configuration ID
     * @return The created power mob, or null if creation failed
     */
    public PowerMob createAndRegisterPowerMob(LivingEntity entity, String configId) {
        PowerMob powerMob;
        if ("random".equals(configId)) {
            powerMob = this.factory.createRandomMob(entity);
        } else {
            powerMob = this.factory.createPowerMob(entity, configId);
        }
        if (powerMob != null) {
            registerPowerMob(powerMob);
            return powerMob;
        }
        return null;
    }

    public PowerMob spawnAndRegisterPowerMob(Location location, EntityType type, String configId, CreatureSpawnEvent.SpawnReason originalReason) {
    if (location.getWorld() == null) {
        plugin.getLogger().warning("Tried to spawn PowerMob in null world for config " + configId);
        return null;
    }

    // Spawn a fresh entity (this will fire a new CreatureSpawnEvent with CUSTOM/PLUGIN reason)
    LivingEntity spawned = (LivingEntity) location.getWorld().spawnEntity(location, type, CreatureSpawnEvent.SpawnReason.CUSTOM);

    PowerMob powerMob;
    if ("random".equals(configId)) {
        powerMob = factory.createRandomMob(spawned);
    } else {
        powerMob = factory.createPowerMob(spawned, configId);
    }

    if (powerMob == null) {
        spawned.remove();
        return null;
    }

    // Register in your existing registry
    registerPowerMob(powerMob);
    return powerMob;
}

    /**
     * Removes invalid power mobs
     */
    public void cleanupInvalidMobs() {
        for (PowerMob powerMob : new HashMap<>(this.powerMobs).values()) {
            if (!powerMob.isValid()) {
                powerMob.remove();
                unregisterPowerMob(powerMob);
            }
        }
    }

//

    /**
     * Removes all power mobs
     */
    public void cleanup() {
        for (PowerMob powerMob : new HashMap<>(this.powerMobs).values()) {
            powerMob.remove();
            unregisterPowerMob(powerMob);
        }
    }
}
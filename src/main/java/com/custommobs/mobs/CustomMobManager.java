package com.custommobs.mobs;

import com.custommobs.CustomMobsPlugin;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all custom mobs in the game
 */
public class CustomMobManager {

    private final CustomMobsPlugin plugin;
    private final Map<UUID, CustomMob> customMobs = new HashMap<>();
    
    @Getter
    private final NamespacedKey customMobKey;
    
    @Getter
    private final CustomMobFactory factory;
    
    /**
     * Creates a new custom mob manager
     * 
     * @param plugin The plugin instance
     */
    public CustomMobManager(CustomMobsPlugin plugin) {
        this.plugin = plugin;
        this.customMobKey = new NamespacedKey(plugin, "custom_mob");
        this.factory = new CustomMobFactory(plugin);
    }
    
    /**
     * Registers a custom mob
     * 
     * @param customMob The custom mob to register
     */
    public void registerCustomMob(CustomMob customMob) {
        this.customMobs.put(customMob.getCustomMobUuid(), customMob);
    }
    
    /**
     * Unregisters a custom mob
     * 
     * @param customMob The custom mob to unregister
     */
    public void unregisterCustomMob(CustomMob customMob) {
        this.customMobs.remove(customMob.getCustomMobUuid());
    }
    
    /**
     * Gets a custom mob by its UUID
     * 
     * @param uuid The custom mob UUID
     * @return The custom mob, or null if not found
     */
    public CustomMob getCustomMob(UUID uuid) {
        return this.customMobs.get(uuid);
    }
    
    /**
     * Gets a custom mob by its entity UUID
     * 
     * @param entityUuid The entity UUID
     * @return The custom mob, or null if not found
     */
    public CustomMob getCustomMobByEntityUuid(UUID entityUuid) {
        for (CustomMob customMob : this.customMobs.values()) {
            if (customMob.getEntityUuid().equals(entityUuid)) {
                return customMob;
            }
        }
        return null;
    }
    
    /**
     * Gets all custom mobs
     * 
     * @return An unmodifiable map of all custom mobs
     */
    public Map<UUID, CustomMob> getCustomMobs() {
        return Collections.unmodifiableMap(this.customMobs);
    }
    
    /**
     * Creates and registers a new custom mob
     * 
     * @param entity The entity to enhance
     * @param configId The configuration ID
     * @return The created custom mob, or null if creation failed
     */
    public CustomMob createAndRegisterCustomMob(LivingEntity entity, String configId) {
        if ("random".equals(configId)) {
            CustomMob customMob = this.factory.createRandomMob(entity);
            if (customMob != null) {
                registerCustomMob(customMob);
                return customMob;
            }
        } else {
            CustomMob customMob = this.factory.createCustomMob(entity, configId);
            if (customMob != null) {
                registerCustomMob(customMob);
                return customMob;
            }
        }
        return null;
    }
    
    /**
     * Removes invalid custom mobs
     */
    public void cleanupInvalidMobs() {
        for (CustomMob customMob : new HashMap<>(this.customMobs).values()) {
            if (!customMob.isValid()) {
                customMob.remove();
                unregisterCustomMob(customMob);
            }
        }
    }
    
    /**
     * Removes all custom mobs
     */
    public void cleanup() {
        for (CustomMob customMob : new HashMap<>(this.customMobs).values()) {
            customMob.remove();
            unregisterCustomMob(customMob);
        }
    }
}
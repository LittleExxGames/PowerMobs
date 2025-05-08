package com.custommobs.mobs.abilities.impl;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import com.custommobs.mobs.abilities.AbstractAbility;
import com.custommobs.utils.MobTargetingUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ability that creates a fire aura around the mob
 */
public class FireAuraAbility extends AbstractAbility {

    private final double radius;
    private final double damage;
    private final int tickRate;
    private final int duration;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    
    /**
     * Creates a new fire aura ability
     * 
     * @param plugin The plugin instance
     */
    public FireAuraAbility(CustomMobsPlugin plugin) {
        super(plugin, "fire-aura");
        
        ConfigurationSection config = plugin.getConfigManager().getConfig()
            .getConfigurationSection("abilities.fire-aura");
            
        if (config != null) {
            this.radius = config.getDouble("radius", 3.0);
            this.damage = config.getDouble("damage", 1.0);
            this.tickRate = config.getInt("tick-rate", 20);
            this.duration = config.getInt("duration", 5);
        } else {
            this.radius = 3.0;
            this.damage = 1.0;
            this.tickRate = 20;
            this.duration = 5;
        }
    }
    
    @Override
    public void apply(CustomMob customMob) {
        UUID entityUuid = customMob.getEntityUuid();
        
        // Cancel existing task if it exists
        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
        }
        
        // Create a new task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!customMob.isValid()) {
                remove(customMob);
                return;
            }
            
            // Get entities in radius
            Location location = customMob.getEntity().getLocation();
            for (Entity entity : location.getWorld().getNearbyEntities(location, this.radius, this.radius, this.radius)) {
                if (entity instanceof LivingEntity && 
                    entity.getUniqueId() != customMob.getEntityUuid() && 
                    entity.getLocation().distance(location) <= this.radius) {

                    // Only target players and their allied entities
                    if (MobTargetingUtil.shouldAllowTargeting(this.plugin, customMob.getEntity(), entity)) {
                        // Set entity on fire and damage it
                        entity.setFireTicks(this.duration * 20);
                        if (entity instanceof LivingEntity) {
                            ((LivingEntity) entity).damage(this.damage, customMob.getEntity());
                        }
                    }

                }
            }
            
            // Show particles
            location.getWorld().spawnParticle(
                Particle.FLAME, 
                location, 
                20, 
                this.radius / 2, 
                0.5, 
                this.radius / 2, 
                0.01
            );
            
        }, 0, this.tickRate);
        
        this.tasks.put(entityUuid, task);
    }


    @Override
    public void remove(CustomMob customMob) {
        UUID entityUuid = customMob.getEntityUuid();
        
        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
            this.tasks.remove(entityUuid);
        }
    }
}
package com.custommobs.mobs.abilities.impl;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import com.custommobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ability that makes a mob leap at its target
 */
public class LeapAbility extends AbstractAbility {

    private final double height;
    private final double forward;
    private final int cooldown;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    /**
     * Creates a new leap ability
     * 
     * @param plugin The plugin instance
     */
    public LeapAbility(CustomMobsPlugin plugin) {
        super(plugin, "leap");
        
        ConfigurationSection config = plugin.getConfigManager().getConfig()
            .getConfigurationSection("abilities.leap");
            
        if (config != null) {
            this.height = config.getDouble("height", 1.5);
            this.forward = config.getDouble("forward", 1.2);
            this.cooldown = config.getInt("cooldown", 8);
        } else {
            this.height = 1.5;
            this.forward = 1.2;
            this.cooldown = 8;
        }
    }
    
    @Override
    public void apply(CustomMob customMob) {
        UUID entityUuid = customMob.getEntityUuid();
        
        // Cancel existing task if it exists
        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
        }
        
        // Create a new task that checks for targets and leaps
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!customMob.isValid()) {
                remove(customMob);
                return;
            }
            
            // Check cooldown
            if (this.cooldowns.containsKey(entityUuid)) {
                long lastUse = this.cooldowns.get(entityUuid);
                if (System.currentTimeMillis() - lastUse < this.cooldown * 1000L) {
                    return;
                }
            }
            
            // Get the entity
            LivingEntity entity = customMob.getEntity();
            if (!(entity instanceof Mob)) {
                return;
            }
            
            Mob mob = (Mob) entity;
            
            // Check if the mob has a target
            LivingEntity target = mob.getTarget();
            if (target == null) {
                // Try to find a nearby player
                Player nearestPlayer = null;
                double nearestDistance = Double.MAX_VALUE;
                
                for (Entity nearby : entity.getNearbyEntities(10, 5, 10)) {
                    if (nearby instanceof Player) {
                        Player player = (Player) nearby;
                        // Only target players in SURVIVAL or ADVENTURE mode
                        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                            double distance = nearby.getLocation().distance(entity.getLocation());
                            if (distance < nearestDistance) {
                                nearestPlayer = player;
                                nearestDistance = distance;
                            }
                        }
                    }
                }
                
                if (nearestPlayer != null) {
                    target = nearestPlayer;
                    mob.setTarget(nearestPlayer);
                } else {
                    return;
                }
            }
            
            // Check if the target is in range (between 3 and 10 blocks)
            double distance = target.getLocation().distance(entity.getLocation());
            if (distance < 3 || distance > 10) {
                return;
            }
            
            // Check if we have a clear line of sight
            if (!mob.hasLineOfSight(target)) {
                return;
            }
            
            // Calculate leap vector
            Vector direction = target.getLocation().toVector().subtract(entity.getLocation().toVector());
            direction.normalize().multiply(this.forward).setY(this.height);
            
            // Apply the velocity
            entity.setVelocity(direction);
            
            // Set cooldown
            this.cooldowns.put(entityUuid, System.currentTimeMillis());
            
        }, 20, 10);
        
        this.tasks.put(entityUuid, task);
    }
    
    @Override
    public void remove(CustomMob customMob) {
        UUID entityUuid = customMob.getEntityUuid();
        
        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
            this.tasks.remove(entityUuid);
        }
        
        this.cooldowns.remove(entityUuid);
    }
}
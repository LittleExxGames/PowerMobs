package com.custommobs.mobs.abilities.impl;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import com.custommobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Ability that allows mobs to teleport when damaged
 */
public class TeleportAbility extends AbstractAbility implements Listener {

    private final double chance;
    private final double maxDistance;
    private final int cooldown;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();
    
    /**
     * Creates a new teleport ability
     * 
     * @param plugin The plugin instance
     */
    public TeleportAbility(CustomMobsPlugin plugin) {
        super(plugin, "teleport");
        
        ConfigurationSection config = plugin.getConfigManager().getConfig()
            .getConfigurationSection("abilities.teleport");
            
        if (config != null) {
            this.chance = config.getDouble("chance", 0.3);
            this.maxDistance = config.getDouble("max-distance", 10.0);
            this.cooldown = config.getInt("cooldown", 5);
        } else {
            this.chance = 0.3;
            this.maxDistance = 10.0;
            this.cooldown = 5;
        }
        
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    public void apply(CustomMob customMob) {
        // This ability is event-based, so we don't need to do anything here
    }
    
    @Override
    public void remove(CustomMob customMob) {
        this.cooldowns.remove(customMob.getEntityUuid());
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        CustomMob customMob = CustomMob.getFromEntity(this.plugin, entity);
        
        if (customMob == null) {
            return;
        }
        
        // Check if the mob has this ability
        boolean hasAbility = customMob.getAbilities().stream()
            .anyMatch(ability -> ability.getId().equals(this.id));
            
        if (!hasAbility) {
            return;
        }
        
        // Check cooldown
        if (this.cooldowns.containsKey(customMob.getEntityUuid())) {
            long lastUse = this.cooldowns.get(customMob.getEntityUuid());
            if (System.currentTimeMillis() - lastUse < this.cooldown * 1000L) {
                return;
            }
        }
        
        // Random chance to trigger
        if (Math.random() > this.chance) {
            return;
        }
        
        // Find a safe location to teleport to
        Location current = entity.getLocation();
        Location target = null;
        
        for (int i = 0; i < 10; i++) {
            double distance = this.random.nextDouble() * this.maxDistance;
            double angle = this.random.nextDouble() * 2 * Math.PI;
            
            double x = current.getX() + distance * Math.cos(angle);
            double z = current.getZ() + distance * Math.sin(angle);
            
            Location potential = new Location(current.getWorld(), x, current.getY(), z);
            
            // Find the ground
            potential = findSafeY(potential);
            
            if (potential != null) {
                target = potential;
                break;
            }
        }
        
        if (target != null) {
            // Play effects at the original location
            current.getWorld().spawnParticle(Particle.PORTAL, current, 30, 0.5, 1.0, 0.5, 0.1);
            current.getWorld().playSound(current, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            
            // Teleport
            entity.teleport(target);
            
            // Play effects at the target location
            target.getWorld().spawnParticle(Particle.PORTAL, target, 30, 0.5, 1.0, 0.5, 0.1);
            target.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            
            // Set cooldown
            this.cooldowns.put(customMob.getEntityUuid(), System.currentTimeMillis());
        }
    }
    
    /**
     * Finds a safe Y coordinate for a location
     * 
     * @param location The location to check
     * @return A safe location, or null if none was found
     */
    private Location findSafeY(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        
        // Start at current Y and look down
        int startY = Math.min(location.getBlockY(), location.getWorld().getMaxHeight() - 2);
        
        for (int y = startY; y > location.getWorld().getMinHeight() + 1; y--) {
            location.setY(y);
            
            if (location.getBlock().getType().isSolid() && 
                !location.clone().add(0, 1, 0).getBlock().getType().isSolid() && 
                !location.clone().add(0, 2, 0).getBlock().getType().isSolid()) {
                
                // Found a solid block with 2 air blocks above it
                return location.clone().add(0.5, 1, 0.5);
            }
        }
        
        return null;
    }
}
package com.custommobs.mobs.abilities.impl;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import com.custommobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Ability that summons minions when the mob is attacked
 */
public class SummonMinionsAbility extends AbstractAbility implements Listener {

    private final EntityType mobType;
    private final int count;
    private final int cooldown;
    private final double health;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    /**
     * Creates a new summon minions ability
     * 
     * @param plugin The plugin instance
     */
    public SummonMinionsAbility(CustomMobsPlugin plugin) {
        super(plugin, "summon-minions");
        
        ConfigurationSection config = plugin.getConfigManager().getConfig()
            .getConfigurationSection("abilities.summon-minions");
            
        if (config != null) {
            String typeString = config.getString("mob-type", "ZOMBIE");
            EntityType type;
            try {
                type = EntityType.valueOf(typeString.toUpperCase());
                if (!type.isAlive() || !type.isSpawnable()) {
                    plugin.getLogger().warning("Invalid mob type for summon-minions ability: " + typeString);
                    type = EntityType.ZOMBIE;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid mob type for summon-minions ability: " + typeString);
                type = EntityType.ZOMBIE;
            }
            
            this.mobType = type;
            this.count = config.getInt("count", 2);
            this.cooldown = config.getInt("cooldown", 30);
            this.health = config.getDouble("health", 10);
        } else {
            this.mobType = EntityType.ZOMBIE;
            this.count = 2;
            this.cooldown = 30;
            this.health = 10;
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
        
        // Check health percentage (25% or less)
        double healthPercent = entity.getHealth() / Objects.requireNonNull(
            entity.getAttribute(Attribute.MAX_HEALTH)
        ).getValue();
        
        if (healthPercent > 0.25) {
            return;
        }
        
        // Summon minions
        Location location = entity.getLocation();
        
        for (int i = 0; i < this.count; i++) {
            // Calculate spawn position (in a circle around the mob)
            double angle = 2 * Math.PI * i / this.count;
            double x = location.getX() + 2 * Math.cos(angle);
            double z = location.getZ() + 2 * Math.sin(angle);
            
            Location spawnLoc = new Location(location.getWorld(), x, location.getY(), z);
            
            // Spawn the minion
            if (location.getWorld() != null) {
                LivingEntity minion = (LivingEntity) location.getWorld().spawnEntity(spawnLoc, this.mobType);
                
                // Set health
                if (minion.getAttribute(Attribute.MAX_HEALTH) != null) {
                    Objects.requireNonNull(
                        minion.getAttribute(Attribute.MAX_HEALTH)
                    ).setBaseValue(this.health);
                    
                    minion.setHealth(this.health);
                }
                
                // Set the minion's target to the attacker
                if (minion instanceof Mob && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                    ((Mob) minion).setTarget(entity.getLastDamageCause().getEntity() instanceof LivingEntity ? 
                        (LivingEntity) entity.getLastDamageCause().getEntity() : null);
                }
                
                // Play spawn effect
                location.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc, 10, 0.5, 0.5, 0.5, 0.1);
                location.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }
        }
        
        // Set cooldown
        this.cooldowns.put(customMob.getEntityUuid(), System.currentTimeMillis());
    }
}
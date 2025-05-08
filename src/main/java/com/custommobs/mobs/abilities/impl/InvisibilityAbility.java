package com.custommobs.mobs.abilities.impl;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import com.custommobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Ability that makes a mob invisible when below certain health
 */
public class InvisibilityAbility extends AbstractAbility implements Listener {

    private final double healthThreshold;
    private final int duration;
    private final int cooldown;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    /**
     * Creates a new invisibility ability
     * 
     * @param plugin The plugin instance
     */
    public InvisibilityAbility(CustomMobsPlugin plugin) {
        super(plugin, "invisibility");
        
        ConfigurationSection config = plugin.getConfigManager().getConfig()
            .getConfigurationSection("abilities.invisibility");
            
        if (config != null) {
            this.healthThreshold = config.getDouble("health-threshold", 0.3);
            this.duration = config.getInt("duration", 10);
            this.cooldown = config.getInt("cooldown", 60);
        } else {
            this.healthThreshold = 0.3;
            this.duration = 10;
            this.cooldown = 60;
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
        
        // Remove invisibility if it's active
        customMob.getEntity().removePotionEffect(PotionEffectType.INVISIBILITY);
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
        
        // Calculate damage and new health
        double damage = event.getFinalDamage();
        double health = entity.getHealth();
        double maxHealth = Objects.requireNonNull(
            entity.getAttribute(Attribute.MAX_HEALTH)
        ).getValue();
        double newHealth = health - damage;
        
        // Check if health would drop below threshold
        if (newHealth / maxHealth <= this.healthThreshold) {
            // Apply invisibility
            entity.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY, 
                this.duration * 20, 
                0, 
                false, 
                true, 
                true
            ));
            
            // Show particles
            if (entity.getLocation().getWorld() != null) {
                entity.getLocation().getWorld().spawnParticle(
                    Particle.SMOKE,
                    entity.getLocation().add(0, 1, 0), 
                    20, 
                    0.5, 
                    0.5, 
                    0.5, 
                    0.1
                );
            }
            
            // Set cooldown
            this.cooldowns.put(customMob.getEntityUuid(), System.currentTimeMillis());
        }
    }
}
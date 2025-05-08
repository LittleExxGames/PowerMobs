package com.custommobs.mobs.abilities.impl;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import com.custommobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ability that summons lightning when the mob attacks
 */
public class LightningStrikeAbility extends AbstractAbility implements Listener {

    private final double chance;
    private final double damageMultiplier;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    /**
     * Creates a new lightning strike ability
     * 
     * @param plugin The plugin instance
     */
    public LightningStrikeAbility(CustomMobsPlugin plugin) {
        super(plugin, "lightning-strike");
        
        ConfigurationSection config = plugin.getConfigManager().getConfig()
            .getConfigurationSection("abilities.lightning-strike");
            
        if (config != null) {
            this.chance = config.getDouble("chance", 0.25);
            this.damageMultiplier = config.getDouble("damage-multiplier", 1.0);
        } else {
            this.chance = 0.25;
            this.damageMultiplier = 1.0;
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
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity)) {
            return;
        }
        
        LivingEntity damager = (LivingEntity) event.getDamager();
        CustomMob customMob = CustomMob.getFromEntity(this.plugin, damager);
        
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
            if (System.currentTimeMillis() - lastUse < 5000) {
                return;
            }
        }
        
        // Random chance to trigger
        if (Math.random() > this.chance) {
            return;
        }
        
        // Trigger lightning
        if (event.getEntity().getLocation().getWorld() != null) {
            event.getEntity().getLocation().getWorld().strikeLightning(event.getEntity().getLocation());
            
            // Increase damage
            event.setDamage(event.getDamage() * this.damageMultiplier);
            
            // Set cooldown
            this.cooldowns.put(customMob.getEntityUuid(), System.currentTimeMillis());
        }
    }
}
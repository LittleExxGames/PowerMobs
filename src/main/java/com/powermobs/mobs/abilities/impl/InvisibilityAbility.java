package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Ability that makes a mob invisible when below certain health
 */
public class InvisibilityAbility extends AbstractAbility implements Listener {

    private final String title = "Invisibility";
    private final String description = "Makes the mob invisible when below a certain health threshold.";
    private final Material material = Material.GLASS;
    private final double healthThreshold;
    private final int duration;
    private final int cooldown;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * Creates a new invisibility ability
     *
     * @param plugin The plugin instance
     */
    public InvisibilityAbility(PowerMobsPlugin plugin) {
        super(plugin, "invisibility");

        ConfigurationSection config = plugin.getConfigManager().getAbilitiesConfigManager().getConfig().getConfigurationSection("abilities.invisibility");

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
    public void apply(PowerMob powerMob) {
        // This ability is event-based, so we don't need to do anything here
    }

    @Override
    public void remove(PowerMob powerMob) {
        this.cooldowns.remove(powerMob.getEntityUuid());

        // Remove invisibility if it's active
        powerMob.getEntity().removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public Material getMaterial() {
        return this.material;
    }

    @Override
    public List<String> getStatus() {
        return List.of();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        PowerMob powerMob = PowerMob.getFromEntity(this.plugin, entity);

        if (powerMob == null) {
            return;
        }

        // Check if the mob has this ability
        boolean hasAbility = powerMob.getAbilities().stream()
                .anyMatch(ability -> ability.getId().equals(this.id));

        if (!hasAbility) {
            return;
        }

        // Check cooldown
        if (this.cooldowns.containsKey(powerMob.getEntityUuid())) {
            long lastUse = this.cooldowns.get(powerMob.getEntityUuid());
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
            this.cooldowns.put(powerMob.getEntityUuid(), System.currentTimeMillis());
        }
    }
}
package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.powermobs.utils.MobTargetingUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Ability that creates a fire aura around the mob
 */
public class FireAuraAbility extends AbstractAbility {

    private final String title = "Fire Aura";
    private final String description = "Ignites entities within the aura.";
    private final Material material = Material.CAMPFIRE;
    private final double defaultRadius = 3.0;
    private final double defaultDamage = 1.0;
    private final int defaultTickRate = 20;
    private final int defaultDuration = 5;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    /**
     * Creates a new fire aura ability
     *
     * @param plugin The plugin instance
     */
    public FireAuraAbility(PowerMobsPlugin plugin) {
        super(plugin, "fire-aura");
    }

    @Override
    public void apply(PowerMob powerMob) {
        UUID entityUuid = powerMob.getEntityUuid();

        final double radius = powerMob.getAbilityDouble(this.id, "radius", this.defaultRadius);
        final double damage = powerMob.getAbilityDouble(this.id, "damage", this.defaultDamage);
        final int tickRate = powerMob.getAbilityInt(this.id, "tick-rate", this.defaultTickRate);
        final int durationSeconds = powerMob.getAbilityInt(this.id, "duration", this.defaultDuration);

        // Cancel existing task if it exists
        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
        }

        // Create a new task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!powerMob.isValid()) {
                remove(powerMob);
                return;
            }

            // Get entities in radius
            Location location = powerMob.getEntity().getLocation();
            for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
                if (entity instanceof LivingEntity &&
                        entity.getUniqueId() != powerMob.getEntityUuid() &&
                        entity.getLocation().distance(location) <= radius) {

                    // Only target players and their allied entities
                    if (MobTargetingUtil.shouldAllowTargeting(this.plugin, powerMob.getEntity(), entity)) {
                        // Set entity on fire and damage it
                        entity.setFireTicks(durationSeconds * 20);
                        ((LivingEntity) entity).damage(damage, powerMob.getEntity());
                    }

                }
            }

            // Show particles
            location.getWorld().spawnParticle(
                    Particle.FLAME,
                    location,
                    20,
                    radius / 2,
                    0.5,
                    radius / 2,
                    0.01
            );

        }, 0, tickRate);

        this.tasks.put(entityUuid, task);
    }


    @Override
    public void remove(PowerMob powerMob) {
        UUID entityUuid = powerMob.getEntityUuid();

        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
            this.tasks.remove(entityUuid);
        }
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

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Radius in blocks"));
        m.put("damage", AbilityConfigField.dbl("damage", defaultDamage, "Damage per tick"));
        m.put("tick-rate", AbilityConfigField.integer("tick-rate", defaultTickRate, "# Ticks between damage (20 ticks = 1 second)"));
        m.put("duration", AbilityConfigField.integer("duration", defaultDuration, "Fire duration in seconds"));
        return m;
    }
}
package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.powermobs.utils.MobTargetingUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ability that creates a fire aura around the mob
 */
public class FireAuraAbility extends AbstractAbility {

    private final String title = "Fire Aura";
    private final String description = "Ignites entities within the aura.";
    private final Material material = Material.CAMPFIRE;
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
    public FireAuraAbility(PowerMobsPlugin plugin) {
        super(plugin, "fire-aura");

        ConfigurationSection config = plugin.getConfigManager().getAbilitiesConfigManager().getConfig().getConfigurationSection("abilities.fire-aura");

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
    public void apply(PowerMob powerMob) {
        UUID entityUuid = powerMob.getEntityUuid();

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
            for (Entity entity : location.getWorld().getNearbyEntities(location, this.radius, this.radius, this.radius)) {
                if (entity instanceof LivingEntity &&
                        entity.getUniqueId() != powerMob.getEntityUuid() &&
                        entity.getLocation().distance(location) <= this.radius) {

                    // Only target players and their allied entities
                    if (MobTargetingUtil.shouldAllowTargeting(this.plugin, powerMob.getEntity(), entity)) {
                        // Set entity on fire and damage it
                        entity.setFireTicks(this.duration * 20);
                        ((LivingEntity) entity).damage(this.damage, powerMob.getEntity());
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
        return material;
    }

    @Override
    public List<String> getStatus() {
        return List.of();
    }
}
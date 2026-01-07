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
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Ability that regenerates the mob's health over time
 */
public class RegenerationAbility extends AbstractAbility {

    private final String title = "Regeneration";
    private final String description = "Occasionally heals the mob by a certain amount over time.";
    private final Material material = Material.GOLDEN_APPLE;
    private final double amount;
    private final int tickRate;
    private final boolean particles;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    /**
     * Creates a new regeneration ability
     *
     * @param plugin The plugin instance
     */
    public RegenerationAbility(PowerMobsPlugin plugin) {
        super(plugin, "regeneration");

        ConfigurationSection config = plugin.getConfigManager().getAbilitiesConfigManager().getConfig().getConfigurationSection("abilities.regeneration");

        if (config != null) {
            this.amount = config.getDouble("amount", 0.5);
            this.tickRate = config.getInt("tick-rate", 20);
            this.particles = config.getBoolean("particles", true);
        } else {
            this.amount = 0.5;
            this.tickRate = 20;
            this.particles = true;
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

            LivingEntity entity = powerMob.getEntity();
            double maxHealth = Objects.requireNonNull(
                    entity.getAttribute(Attribute.MAX_HEALTH)
            ).getValue();

            // Only regenerate if not at full health
            if (entity.getHealth() < maxHealth) {
                // Calculate new health
                double newHealth = Math.min(entity.getHealth() + this.amount, maxHealth);
                entity.setHealth(newHealth);

                // Show particles
                if (this.particles && entity.getLocation().getWorld() != null) {
                    entity.getLocation().getWorld().spawnParticle(
                            Particle.HEART,
                            entity.getLocation().add(0, 1, 0),
                            1,
                            0.5,
                            0.5,
                            0.5,
                            0
                    );
                }
            }

        }, this.tickRate, this.tickRate);

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
}
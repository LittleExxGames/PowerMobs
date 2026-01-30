package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
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
    private final double defaultAmount = 0.5;
    private final int defaultTickRate = 20;
    private final boolean defaultParticles = true;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    /**
     * Creates a new regeneration ability
     *
     * @param plugin The plugin instance
     */
    public RegenerationAbility(PowerMobsPlugin plugin) {
        super(plugin, "regeneration");
    }

    @Override
    public void apply(PowerMob powerMob) {
        UUID entityUuid = powerMob.getEntityUuid();

        final double amount = powerMob.getAbilityDouble(this.id, "amount", this.defaultAmount);
        final int tickRate = powerMob.getAbilityInt(this.id, "tick-rate", this.defaultTickRate);
        final boolean particles = powerMob.getAbilityBoolean(this.id, "particles", this.defaultParticles);

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
                double newHealth = Math.min(entity.getHealth() + amount, maxHealth);
                entity.setHealth(newHealth);

                // Show particles
                if (particles && entity.getLocation().getWorld() != null) {
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

    }, tickRate, tickRate);

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
        return Map.of(
                "amount", AbilityConfigField.dbl("amount", this.defaultAmount, "Health to regen per trigger"),
                "tick-rate", AbilityConfigField.integer("tick-rate", this.defaultTickRate, "Tick rate in ticks"),
                "particles", AbilityConfigField.bool("particles", this.defaultParticles, "Show particles"));
    }
}
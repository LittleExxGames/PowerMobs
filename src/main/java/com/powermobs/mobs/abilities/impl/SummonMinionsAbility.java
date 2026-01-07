package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Ability that summons minions when the mob is attacked
 */
public class SummonMinionsAbility extends AbstractAbility implements Listener {

    private final String title = "Summon Minions";
    private final String description = "A chance the mob will summon minions when it is attacked.";
    private final Material material = Material.ZOMBIE_SPAWN_EGG;
    private final EntityType mobType;
    private final int count;
    private final int cooldown;
    private final double health;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final NamespacedKey minionOwnerKey; // add

    /**
     * Creates a new summon minions ability
     *
     * @param plugin The plugin instance
     */
    public SummonMinionsAbility(PowerMobsPlugin plugin) {
        super(plugin, "summon-minions");

        ConfigurationSection config = plugin.getConfigManager().getAbilitiesConfigManager().getConfig().getConfigurationSection("abilities.summon-minions");

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

        this.minionOwnerKey = new NamespacedKey(plugin, "minion_owner");
    }

    @Override
    public void apply(PowerMob powerMob) {
        // This ability is event-based, so we don't need to do anything here
    }

    @Override
    public void remove(PowerMob powerMob) {
        this.cooldowns.remove(powerMob.getEntityUuid());
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

        // Check health percentage (25% or less)
        double healthPercent = entity.getHealth() / Objects.requireNonNull(
                entity.getAttribute(Attribute.MAX_HEALTH)
        ).getValue();

        if (healthPercent > 0.25) {
            return;
        }

        // Try to find the player attacker
        LivingEntity attacker = null;

        if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
            Entity damager = damageByEntityEvent.getDamager();

            // Direct player attack
            if (damager instanceof Player) {
                attacker = (LivingEntity) damager;
            }
            // Projectile from player
            else if (damager instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof Player) {
                    attacker = (LivingEntity) projectile.getShooter();
                }
            }
            // Other entity attacks (could be from player's pet/summon)
            else if (damager instanceof LivingEntity) {
                attacker = findNearestPlayer(entity.getLocation());
            }
        } else {
            // For other damage types (fall, fire, etc.), find nearest player
            attacker = findNearestPlayer(entity.getLocation());
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

                // add: mark/register as plugin power mob
                PowerMob minionCustom = new PowerMob(this.plugin, minion, "summoned-minion");
                this.plugin.getPowerMobManager().registerPowerMob(minionCustom);

                // add: tag owner to prevent friendly targeting or for later logic
                minion.getPersistentDataContainer().set(minionOwnerKey,
                        PersistentDataType.STRING, entity.getUniqueId().toString());

                // Set the minion's target to the attacker (if found)
                if (minion instanceof Mob && attacker != null) {
                    ((Mob) minion).setTarget(attacker);
                }


                // Play spawn effect
                location.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc, 10, 0.5, 0.5, 0.5, 0.1);
                location.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }
        }

        // Set cooldown
        this.cooldowns.put(powerMob.getEntityUuid(), System.currentTimeMillis());
    }

    /**
     * Finds the nearest player to the given location within a reasonable range
     *
     * @param location The location to search around
     * @return The nearest player, or null if none found
     */
    private LivingEntity findNearestPlayer(Location location) {
        if (location.getWorld() == null) {
            return null;
        }

        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        // Search within 50 blocks
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) < 50) {
                double distance = player.getLocation().distance(location);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }

        return nearestPlayer;
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
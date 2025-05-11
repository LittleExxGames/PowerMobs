package com.custommobs.mobs.tracking;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks damage done to custom mobs
 */
public class DamageTracker {

    private final CustomMobsPlugin plugin;
    private final Map<UUID, Map<UUID, Double>> mobDamageTracker = new HashMap<>();

    /**
     * Creates a new damage tracker
     *
     * @param plugin The plugin instance
     */
    public DamageTracker(CustomMobsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers damage done to a custom mob
     *
     * @param mob The custom mob
     * @param event The damage event
     */
    public void registerDamage(CustomMob mob, EntityDamageByEntityEvent event) {
        UUID mobUuid = mob.getEntityUuid();
        double damage = event.getFinalDamage();

        // Get or create damage map for this mob
        Map<UUID, Double> damageMap = mobDamageTracker.computeIfAbsent(mobUuid, k -> new HashMap<>());

        // Determine the player that caused the damage
        Player player = getPlayerFromDamageEvent(event);
        if (player == null) {
            // Check if damage was caused by a player ally (wolf, golem, etc.)
            Entity damager = getEntityDamager(event);
            if (damager != null && isPlayerAlly(damager) && plugin.getConfigManager().isCountAllyDamage()) {
                // Get the owner UUID
                UUID ownerUuid = getAllyOwnerUuid(damager);
                if (ownerUuid != null) {
                    // Add damage to the owner's total
                    damageMap.merge(ownerUuid, damage, Double::sum);
                    plugin.debug("Registered " + damage + " ally damage to mob " + mobUuid + " from " +
                            (Bukkit.getPlayer(ownerUuid) != null ? Bukkit.getPlayer(ownerUuid).getName() : ownerUuid));
                }
            }
            return;
        }

        // Add damage to the player's total
        damageMap.merge(player.getUniqueId(), damage, Double::sum);
        plugin.debug("Registered " + damage + " damage to mob " + mobUuid + " from " + player.getName());
        plugin.debug("After adding damage, total damage map for mob " + mobUuid + ": " + damageMap);
    }

    /**
     * Checks if a player (or their allies) has done enough damage to a mob to qualify for drops
     *
     * @param mob The custom mob
     * @param player The player to check
     * @return True if the player has done enough damage
     */
    public boolean hasPlayerDoneEnoughDamage(CustomMob mob, Player player) {
        UUID mobUuid = mob.getEntityUuid();

        // If we're not tracking this mob, return false
        if (!mobDamageTracker.containsKey(mobUuid)) {
            plugin.debug("Mob " + mobUuid + " is not being tracked, so it cannot be qualified for drops");
            return false;
        }

        Map<UUID, Double> damageMap = mobDamageTracker.get(mobUuid);
        double totalDamage = getTotalDamage(damageMap);
        double playerDamage = damageMap.getOrDefault(player.getUniqueId(), 0.0);

        // Calculate the percentage of damage done by the player
        double percentage = (totalDamage > 0) ? (playerDamage / totalDamage) * 100.0 : 0.0;

        // Check if it meets the requirement
        double required = plugin.getConfigManager().getPlayerDamageRequirement();
        boolean meetsRequirement = percentage >= required;

        plugin.debug(player.getName() + " did " + percentage + "% damage to mob " + mobUuid +
                " (required: " + required + "%) - " + (meetsRequirement ? "Qualified" : "Not qualified") + " for drops");

        return meetsRequirement;
    }

    /**
     * Cleans up tracking data for a mob
     *
     * @param mobUuid The UUID of the mob to clean up
     */
    public void cleanupMob(UUID mobUuid) {
        mobDamageTracker.remove(mobUuid);
    }

    /**
     * Gets the total damage done to a mob
     *
     * @param damageMap The damage map for the mob
     * @return The total damage
     */
    private double getTotalDamage(Map<UUID, Double> damageMap) {
        return damageMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Gets the player from a damage event, or null if the damage wasn't caused by a player
     *
     * @param event The damage event
     * @return The player, or null
     */
    private Player getPlayerFromDamageEvent(EntityDamageByEntityEvent event) {
        Entity damager = getEntityDamager(event);
        if (damager instanceof Player) {
            return (Player) damager;
        }
        return null;
    }

    /**
     * Gets the actual damaging entity, accounting for projectiles
     *
     * @param event The damage event
     * @return The damaging entity
     */
    private Entity getEntityDamager(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        // Handle projectiles
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity) {
                return (Entity) shooter;
            }
        }

        return damager;
    }

    /**
     * Checks if an entity is a player ally (tamed wolf, iron golem, etc.)
     *
     * @param entity The entity to check
     * @return True if the entity is a player ally
     */
    private boolean isPlayerAlly(Entity entity) {
        // Check if it's a tameable entity (wolf, cat, etc.)
        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            return tameable.isTamed() && tameable.getOwner() != null;
        }

//        // Check for player-created iron golems
//        if (entity instanceof org.bukkit.entity.IronGolem) {
//            org.bukkit.entity.IronGolem golem = (org.bukkit.entity.IronGolem) entity;
//            return golem.isPlayerCreated();
//        }

        return false;
    }

    /**
     * Gets the UUID of the player who owns an ally
     *
     * @param entity The ally entity
     * @return The owner's UUID, or null
     */
    private UUID getAllyOwnerUuid(Entity entity) {
        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            if (tameable.getOwner() != null) {
                return tameable.getOwner().getUniqueId();
            }
        }

        return null;
    }
}


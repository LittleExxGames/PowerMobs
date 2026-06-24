package com.powermobs.mobs.tracking;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.stats.CachedStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tracks damage done to power mobs
 */
public class DamageTracker {

    private final PowerMobsPlugin plugin;
    private final Map<UUID, Map<UUID, Double>> mobDamageTracker = new HashMap<>();

    /**
     * Creates a new damage tracker
     *
     * @param plugin The plugin instance
     */
    public DamageTracker(PowerMobsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers damage done to a power mob
     *
     * @param mob   The power mob
     * @param event The damage event
     */
    public void registerDamage(PowerMob mob, EntityDamageByEntityEvent event) {
        UUID mobUuid = mob.getEntityUuid();
        double damage = event.getFinalDamage();

        // Get or create damage map for this mob
        Map<UUID, Double> damageMap = mobDamageTracker.computeIfAbsent(mobUuid, k -> new HashMap<>());

        // Determine the player that caused the damage
        Player player = getPlayerFromDamageEvent(event);
        if (player == null) {
            // Check if damage was caused by a player ally (wolf, golem, etc.)
            Entity damager = getEntityDamager(event);
            if (isPlayerAlly(damager) && plugin.getConfigManager().isCountAllyDamage()) {
                UUID ownerUuid = getAllyOwnerUuid(damager);
                if (ownerUuid != null) {
                    // Add damage to the owner's total
                    damageMap.merge(ownerUuid, damage, Double::sum);
                    plugin.debug("Registered " + String.format("%.2f", damage) + " ally damage to mob " + mobUuid + " from " +
                            (Bukkit.getPlayer(ownerUuid) != null ? Bukkit.getPlayer(ownerUuid).getName() : ownerUuid), "mob_combat");
                }
            }
            return;
        }

        // Add damage to the player's total
        damageMap.merge(player.getUniqueId(), damage, Double::sum);
        CachedStats.updatePlayerStats(player.getUniqueId(), mob.getId(), 0, 0, damage, 0);
        plugin.debug("Registered " + String.format("%.2f", damage) + " damage to mob " + mob.getId() + "    UUID: " + mobUuid + " from " + player.getName(), "mob_combat");
        plugin.debug("After adding damage, total damage map for mob " + mobUuid + ": " + formatDamageMapForDebug(damageMap), "mob_combat");
    }

    public void registerSpecialDamage(Player player, PowerMob mob, double damage) {
        Map<UUID, Double> damageMap = mobDamageTracker.computeIfAbsent(mob.getEntityUuid(), k -> new HashMap<>());

        damageMap.merge(player.getUniqueId(), damage, Double::sum);
        CachedStats.updatePlayerStats(player.getUniqueId(), mob.getId(), 0, 0, damage, 0);

        plugin.debug("Registered " + String.format("%.2f", damage) + " damage to mob " + mob.getId() + "    UUID: " + mob.getEntityUuid() + " from " + player.getName(), "mob_combat");
    }

    public void calculateMobDeathInvolvement(PowerMob mob, int include, double damagePercent){
        UUID identifier = mob.getEntityUuid();
        if (!mobDamageTracker.containsKey(identifier)) {
            plugin.debug("Mob id: " + mob.getId() + "   UUID: " + identifier + " is not being tracked, so it cannot be qualified for drops", "drops");
            return;
        }
        Map<UUID, Double> damageMap = mobDamageTracker.get(identifier);

        double totalDamage = getTotalDamage(damageMap);
        if (totalDamage <= 0.0) {
            return;
        }

        double requiredFraction = damagePercent / 100.0;

        List<Map.Entry<UUID, Double>> topPlayers = damageMap.entrySet().stream()
                .filter(entry -> (entry.getValue() / totalDamage) >= requiredFraction)
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(include)
                .toList();
        for (Map.Entry<UUID, Double> entry : topPlayers){
            CachedStats.updatePlayerStats(entry.getKey(), mob.getId(), 1, 0, 0, 0);
        }
        for (Map.Entry<UUID, Double> entry : damageMap.entrySet()){
            CachedStats.updatePlayerStats(entry.getKey(), mob.getId(), 0, 0, 0, totalDamage);
        }
    }

    public void calculatePlayerDeathInvolvement(PowerMob mob, Player player){
        CachedStats.updatePlayerStats(player.getUniqueId(), mob.getId(), 0, 1, 0, 0);
    }

    private String formatDamageMapForDebug(Map<UUID, Double> damageMap) {
        return damageMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> resolvePlayerName(e.getKey()) + "=" + String.format("%.2f", e.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String resolvePlayerName(UUID playerUuid) {
        Player online = Bukkit.getPlayer(playerUuid);
        if (online != null) {
            return online.getName();
        }

        String offlineName = Bukkit.getOfflinePlayer(playerUuid).getName();
        return (offlineName != null && !offlineName.isBlank()) ? offlineName : playerUuid.toString();
    }

    /**
     * Checks if a player (or their allies) has done enough damage to a mob to qualify for drops
     *
     * @param mob    The power mob
     * @param player The player to check
     * @return True if the player has done enough damage
     */
    public boolean hasPlayerDoneEnoughDamage(PowerMob mob, Player player) {
        UUID mobUuid = mob.getEntityUuid();

        if (!mobDamageTracker.containsKey(mobUuid)) {
            plugin.debug("Mob " + mobUuid + " is not being tracked, so it cannot be qualified for drops", "drops");
            return false;
        }

        Map<UUID, Double> damageMap = mobDamageTracker.get(mobUuid);
        double totalDamage = getTotalDamage(damageMap);
        double playerDamage = damageMap.getOrDefault(player.getUniqueId(), 0.0);

        double percentage = (totalDamage > 0) ? (playerDamage / totalDamage) * 100.0 : 0.0;

        double required = plugin.getConfigManager().getPlayerDamageRequirement();
        boolean meetsRequirement = percentage >= required;

        plugin.debug(player.getName() + " did " + percentage + "% damage to mob " + mobUuid, "mob combat");
        plugin.debug("Required Damage %: " + required + "% - " + (meetsRequirement ? "Qualified" : "Not qualified") + " for drops", "drops");

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
        if (damager instanceof Projectile projectile) {
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
        if (entity instanceof Tameable tameable) {
            return tameable.isTamed() && tameable.getOwner() != null;
        }
        return false;
    }

    /**
     * Gets the UUID of the player who owns an ally
     *
     * @param entity The ally entity
     * @return The owner's UUID, or null
     */
    private UUID getAllyOwnerUuid(Entity entity) {
        if (entity instanceof Tameable tameable) {
            if (tameable.getOwner() != null) {
                return tameable.getOwner().getUniqueId();
            }
        }

        return null;
    }
}


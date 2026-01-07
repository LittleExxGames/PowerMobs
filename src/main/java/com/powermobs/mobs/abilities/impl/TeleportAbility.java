package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Ability that allows mobs to teleport when damaged or after inactivity
 */
public class TeleportAbility extends AbstractAbility implements Listener {

    private final String title = "Teleportation";
    private final String description = "The mob will teleport away when attacked, or to the last attacker after inactivity.";
    private final Material material = Material.ENDER_PEARL;
    private final double chance;
    private final double maxAwayDistance;
    private final double maxToDistance;
    private final int cooldown;
    private final int inactivityTime;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, PlayerAttackerInfo> lastAttackers = new HashMap<>();
    private final Map<UUID, BukkitRunnable> inactivityTasks = new HashMap<>();
    private final Random random = new Random();

    /**
     * Creates a new teleport ability
     *
     * @param plugin The plugin instance
     */
    public TeleportAbility(PowerMobsPlugin plugin) {
        super(plugin, "teleport");

        ConfigurationSection config = plugin.getConfigManager().getAbilitiesConfigManager().getConfig().getConfigurationSection("abilities.teleport");

        if (config != null) {
            this.chance = config.getDouble("chance", 0.3);
            this.maxAwayDistance = config.getDouble("max-away-distance", 10.0);
            this.maxToDistance = config.getDouble("max-to-distance", 100.0);
            this.cooldown = config.getInt("cooldown", 5);
            this.inactivityTime = config.getInt("inactivity-time", 30);
        } else {
            this.chance = 0.3;
            this.maxAwayDistance = 10.0;
            this.maxToDistance = 100.0;
            this.cooldown = 5;
            this.inactivityTime = 30;
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
        UUID mobUuid = powerMob.getEntityUuid();
        this.cooldowns.remove(mobUuid);
        this.lastAttackers.remove(mobUuid);

        // Cancel any pending inactivity task
        BukkitRunnable task = this.inactivityTasks.remove(mobUuid);
        if (task != null) {
            task.cancel();
        }
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

        UUID mobUuid = powerMob.getEntityUuid();

        // Try to find the player attacker
        Player playerAttacker = null;
        if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
            Entity damager = damageByEntityEvent.getDamager();

            // Direct player attack
            if (damager instanceof Player) {
                playerAttacker = (Player) damager;
            }
            // Projectile from player
            else if (damager instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof Player) {
                    playerAttacker = (Player) projectile.getShooter();
                }
            }
        }

        // Update last attacker info if it was a player
        if (playerAttacker != null) {
            this.lastAttackers.put(mobUuid, new PlayerAttackerInfo(playerAttacker.getUniqueId(), System.currentTimeMillis()));

            // Cancel any existing inactivity task
            BukkitRunnable existingTask = this.inactivityTasks.remove(mobUuid);
            if (existingTask != null) {
                existingTask.cancel();
            }

            // Start new inactivity task
            scheduleInactivityTeleport(powerMob, entity);
        }

        // Check cooldown
        if (this.cooldowns.containsKey(mobUuid)) {
            long lastUse = this.cooldowns.get(mobUuid);
            if (System.currentTimeMillis() - lastUse < this.cooldown * 1000L) {
                return;
            }
        }

        // Random chance to trigger immediate teleport away
        if (Math.random() > this.chance) {
            return;
        }

        // Teleport away from attacker
        Location target = findAwayTeleportLocation(entity.getLocation());
        if (target != null) {
            performTeleport(entity, target, mobUuid);
        }
    }

    /**
     * Schedules a task to teleport to the last attacker after inactivity
     *
     * @param powerMob The power mob
     * @param entity   The living entity
     */
    private void scheduleInactivityTeleport(PowerMob powerMob, LivingEntity entity) {
        UUID mobUuid = powerMob.getEntityUuid();

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if mob still exists
                if (entity.isDead() || !entity.isValid()) {
                    inactivityTasks.remove(mobUuid);
                    return;
                }

                // Check if mob still has this ability
                boolean hasAbility = powerMob.getAbilities().stream()
                        .anyMatch(ability -> ability.getId().equals(id));
                if (!hasAbility) {
                    inactivityTasks.remove(mobUuid);
                    return;
                }

                // Check cooldown
                if (cooldowns.containsKey(mobUuid)) {
                    long lastUse = cooldowns.get(mobUuid);
                    if (System.currentTimeMillis() - lastUse < cooldown * 1000L) {
                        // Reschedule for later
                        scheduleInactivityTeleport(powerMob, entity);
                        return;
                    }
                }

                // Get last attacker info
                PlayerAttackerInfo attackerInfo = lastAttackers.get(mobUuid);
                if (attackerInfo != null) {
                    Player player = Bukkit.getPlayer(attackerInfo.playerUuid);

                    // Check if player is still online and alive
                    if (player != null && player.isOnline() && !player.isDead()) {

                        // Enforce max follow distance for teleport-to-player
                        if (player.getWorld() != entity.getWorld()) {
                            inactivityTasks.remove(mobUuid);
                            return; // different world: do not follow-teleport
                        }
                        double maxToDistSq = maxToDistance * maxToDistance;
                        if (entity.getLocation().distanceSquared(player.getLocation()) > maxToDistSq) {
                            // Too far to follow via teleport, stop chasing
                            inactivityTasks.remove(mobUuid);
                            return;
                        }
                        Location target = findPlayerTeleportLocation(player);
                        if (target != null) {
                            performTeleport(entity, target, mobUuid);
                        }
                    } else {
                        // Player is dead or offline, clear the attacker info
                        lastAttackers.remove(mobUuid);
                    }
                }

                inactivityTasks.remove(mobUuid);
            }
        };

        task.runTaskLater(this.plugin, this.inactivityTime * 20L); // Convert seconds to ticks
        this.inactivityTasks.put(mobUuid, task);
    }

    /**
     * Finds a location to teleport away from the current position
     *
     * @param current The current location
     * @return A safe teleport location, or null if none found
     */
    private Location findAwayTeleportLocation(Location current) {
        for (int i = 0; i < 10; i++) {
            double distance = this.random.nextDouble() * this.maxAwayDistance;
            double angle = this.random.nextDouble() * 2 * Math.PI;

            double x = current.getX() + distance * Math.cos(angle);
            double z = current.getZ() + distance * Math.sin(angle);

            Location potential = new Location(current.getWorld(), x, current.getY(), z);

            // Find the ground
            potential = findSafeY(potential);

            if (potential != null) {
                return potential;
            }
        }
        return null;
    }

    /**
     * Finds a safe location near the player to teleport to
     *
     * @param player The player to teleport near
     * @return A safe teleport location, or null if none found
     */
    private Location findPlayerTeleportLocation(Player player) {
        if (player == null || !player.isOnline()) {
            return null;
        }

        Location playerLoc = player.getLocation();

        // Try to find a safe spot within 3-8 blocks of the player
        for (int i = 0; i < 15; i++) {
            // Random distance between 3 and 8 blocks from player
            double distance = 3 + this.random.nextDouble() * 5;
            double angle = this.random.nextDouble() * 2 * Math.PI;

            double x = playerLoc.getX() + distance * Math.cos(angle);
            double z = playerLoc.getZ() + distance * Math.sin(angle);

            Location potential = new Location(playerLoc.getWorld(), x, playerLoc.getY(), z);

            // Find the ground
            potential = findSafeY(potential);

            if (potential != null) {
                return potential;
            }
        }

        // If no safe spot found nearby, try the player's exact location area
        return findSafeY(playerLoc.clone());
    }

    /**
     * Performs the teleportation with effects
     *
     * @param entity  The entity to teleport
     * @param target  The target location
     * @param mobUuid The mob's UUID for cooldown tracking
     */
    private void performTeleport(LivingEntity entity, Location target, UUID mobUuid) {
        Location current = entity.getLocation();

        // Play effects at the original location
        current.getWorld().spawnParticle(Particle.PORTAL, current, 30, 0.5, 1.0, 0.5, 0.1);
        current.getWorld().playSound(current, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Teleport
        entity.teleport(target);

        // Play effects at the target location
        target.getWorld().spawnParticle(Particle.PORTAL, target, 30, 0.5, 1.0, 0.5, 0.1);
        target.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Set cooldown
        this.cooldowns.put(mobUuid, System.currentTimeMillis());
    }

    /**
     * Finds a safe Y coordinate for a location
     *
     * @param location The location to check
     * @return A safe location, or null if none was found
     */
    private Location findSafeY(Location location) {
        if (location.getWorld() == null) {
            return null;
        }

        // Start at current Y and look down
        int startY = Math.min(location.getBlockY(), location.getWorld().getMaxHeight() - 2);

        for (int y = startY; y > location.getWorld().getMinHeight() + 1; y--) {
            location.setY(y);

            if (location.getBlock().getType().isSolid() &&
                    !location.clone().add(0, 1, 0).getBlock().getType().isSolid() &&
                    !location.clone().add(0, 2, 0).getBlock().getType().isSolid()) {

                // Found a solid block with 2 air blocks above it
                return location.clone().add(0.5, 1, 0.5);
            }
        }

        return null;
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

    /**
     * Stores information about the last player attacker
     */
    private record PlayerAttackerInfo(UUID playerUuid, long lastAttackTime) {
    }
}
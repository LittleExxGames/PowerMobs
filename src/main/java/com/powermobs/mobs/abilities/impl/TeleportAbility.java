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
        List<PotentialLocation> candidates = new ArrayList<>();

        // Far away positions
        for (int i = 0; i < 7; i++) {
            double distance = (0.5 + this.random.nextDouble() * 0.5) * this.maxAwayDistance; // 50-100% of max
            double angle = this.random.nextDouble() * 2 * Math.PI;
            candidates.add(new PotentialLocation(
                    current.getX() + distance * Math.cos(angle),
                    current.getZ() + distance * Math.sin(angle),
                    current.getY()
            ));
        }

        // Mid-range positions
        for (int i = 0; i < 3; i++) {
            double distance = this.random.nextDouble() * this.maxAwayDistance * 0.5; // 0-50% of max
            double angle = this.random.nextDouble() * 2 * Math.PI;
            candidates.add(new PotentialLocation(
                    current.getX() + distance * Math.cos(angle),
                    current.getZ() + distance * Math.sin(angle),
                    current.getY()
            ));
        }

        // Try candidates in order
        for (PotentialLocation candidate : candidates) {
            Location potential = new Location(current.getWorld(), candidate.x, candidate.y, candidate.z);
            Location safe = findSafeYWithinRadius(current, potential, this.maxAwayDistance);
            if (safe != null) {
                return safe;
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
        final double maxNearPlayerRadius = 8.0;

        // Generate candidates in a ring around the player (avoids being too close or too far)
        List<PotentialLocation> candidates = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2 * i) / 12.0; // Evenly distributed angles
            double distance = 4 + this.random.nextDouble() * 3; // 4-7 blocks

            candidates.add(new PotentialLocation(
                    playerLoc.getX() + distance * Math.cos(angle),
                    playerLoc.getZ() + distance * Math.sin(angle),
                    playerLoc.getY()
            ));
        }

        // Shuffle for randomness but with better coverage
        Collections.shuffle(candidates, this.random);

        // Try first 10 shuffled candidates
        for (int i = 0; i < Math.min(10, candidates.size()); i++) {
            PotentialLocation candidate = candidates.get(i);
            Location potential = new Location(playerLoc.getWorld(), candidate.x, candidate.y, candidate.z);
            Location safe = findSafeYWithinRadius(playerLoc, potential, maxNearPlayerRadius);
            if (safe != null) {
                return safe;
            }
        }

        // Fallback: try player's exact location
        return findSafeYWithinRadius(playerLoc, playerLoc.clone(), maxNearPlayerRadius);
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
     * Optimized safe Y finder with reduced block access
     */
    private Location findSafeYWithinRadius(Location origin, Location location, double maxRadius) {
        if (origin == null || location == null || location.getWorld() == null || origin.getWorld() == null) {
            return null;
        }
        if (origin.getWorld() != location.getWorld()) {
            return null;
        }

        double maxRadiusSq = maxRadius * maxRadius;
        double dx = location.getX() - origin.getX();
        double dz = location.getZ() - origin.getZ();
        double horizontalSq = (dx * dx) + (dz * dz);

        if (horizontalSq > maxRadiusSq) {
            return null;
        }

        double maxVertical = Math.sqrt(maxRadiusSq - horizontalSq);
        World world = location.getWorld();

        int worldMaxY = world.getMaxHeight() - 2;
        int worldMinY = world.getMinHeight() + 1;
        int minAllowedY = (int) Math.ceil(origin.getY() - maxVertical);
        int maxAllowedY = (int) Math.floor(origin.getY() + maxVertical);
        int minY = Math.max(worldMinY, minAllowedY);
        int maxY = Math.min(worldMaxY, maxAllowedY);

        if (minY > maxY) {
            return null;
        }

        int startY = Math.min(Math.max(location.getBlockY(), minY), maxY);
        int blockX = location.getBlockX();
        int blockZ = location.getBlockZ();

        // Reuse location objects to reduce GC pressure
        Location checkLoc = new Location(world, blockX, startY, blockZ);
        Location aboveLoc1 = new Location(world, blockX, startY + 1, blockZ);
        Location aboveLoc2 = new Location(world, blockX, startY + 2, blockZ);

        // Spiral search from starting Y
        for (int offset = 0; offset <= Math.max(startY - minY, maxY - startY); offset++) {
            // Check below
            int yBelow = startY - offset;
            if (yBelow >= minY) {
                checkLoc.setY(yBelow);
                aboveLoc1.setY(yBelow + 1);
                aboveLoc2.setY(yBelow + 2);

                // Single block access per level - cache the results
                if (checkLoc.getBlock().getType().isSolid()
                        && !aboveLoc1.getBlock().getType().isSolid()
                        && !aboveLoc2.getBlock().getType().isSolid()) {

                    Location result = new Location(world, blockX + 0.5, yBelow + 1, blockZ + 0.5);
                    if (result.distanceSquared(origin) <= maxRadiusSq) {
                        return result;
                    }
                }
            }

            // Check above
            if (offset > 0) {
                int yAbove = startY + offset;
                if (yAbove <= maxY) {
                    checkLoc.setY(yAbove);
                    aboveLoc1.setY(yAbove + 1);
                    aboveLoc2.setY(yAbove + 2);

                    if (checkLoc.getBlock().getType().isSolid()
                            && !aboveLoc1.getBlock().getType().isSolid()
                            && !aboveLoc2.getBlock().getType().isSolid()) {

                        Location result = new Location(world, blockX + 0.5, yAbove + 1, blockZ + 0.5);
                        if (result.distanceSquared(origin) <= maxRadiusSq) {
                            return result;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Helper class to store potential teleport coordinates
     */
    private record PotentialLocation(double x, double z, double y) {}

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
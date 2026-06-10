package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.powermobs.utils.MobTargetingUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class VexedAbility extends AbstractAbility implements Listener {

    private final String title = "Vexed";
    private final String description = "Spawns charging vexes above nearby players over a short duration.";
    private final Material material = Material.VEX_SPAWN_EGG;
    private final double defaultChance = 0.3;
    private final int defaultMaxDistance = 15;
    private final int defaultVexDamage = 4;
    private final int defaultVexCount = 5;
    private final int defaultDurationSeconds = 4;
    private final double defaultVelocityTickSpeed = 0.4;
    private final int defaultSpawnDistance = 5;
    private final double defaultHitboxSize = 0.4;
    private final boolean defaultOnlyTarget = true;
    private final int defaultCooldown = 10;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Set<UUID> activeBursts = new HashSet<>();


    public VexedAbility(PowerMobsPlugin plugin) {
        super(plugin, "vexed");

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {
        if (powerMob == null || !powerMob.isValid()) {
            return;
        }

        UUID mobUuid = powerMob.getEntityUuid();

        if (this.activeTasks.containsKey(mobUuid)) {
            return;
        }
        double chance = powerMob.getAbilityDouble(this.id, "chance", this.defaultChance);
        int maxDistance = Math.max(1, powerMob.getAbilityInt(this.id, "max-distance", this.defaultMaxDistance));
        int vexDamage = powerMob.getAbilityInt(this.id, "vex-damage", this.defaultVexDamage);
        int vexCount = Math.max(1, powerMob.getAbilityInt(this.id, "vex-count", this.defaultVexCount));
        int durationSeconds = Math.max(1, powerMob.getAbilityInt(this.id, "duration-seconds", this.defaultDurationSeconds));
        double velocityTickSpeed = Math.max(0.0001, powerMob.getAbilityDouble(this.id, "velocity-tick-speed", this.defaultVelocityTickSpeed));
        int spawnDistance = Math.max(1, powerMob.getAbilityInt(this.id, "spawn-distance", this.defaultSpawnDistance));
        double hitboxSize = powerMob.getAbilityDouble(this.id, "hitbox-size", this.defaultHitboxSize);
        boolean onlyTarget = powerMob.getAbilityBoolean(this.id, "only-target", this.defaultOnlyTarget);
        int cooldownSeconds = powerMob.getAbilityInt(this.id, "cooldown", this.defaultCooldown);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!powerMob.isValid()) {
                    cleanup();
                    return;
                }

                if (activeBursts.contains(mobUuid)) {
                    return;
                }

                long now = System.currentTimeMillis();
                long lastUse = cooldowns.getOrDefault(mobUuid, 0L);
                if (now - lastUse < cooldownSeconds * 1000L) {
                    return;
                }

                if (Math.random() > chance) {
                    return;
                }
                Player burstTarget = findTarget(powerMob, maxDistance, onlyTarget);
                if (burstTarget == null) {
                    return;
                }
                Sound sound = Sound.ENTITY_VEX_CHARGE;
                powerMob.getEntity().getWorld().playSound(powerMob.getEntity().getLocation(), sound, 1.0f, 1.0f);

                activeBursts.add(mobUuid);

                int totalDurationTicks = durationSeconds * 20;
                int intervalTicks = Math.max(1, totalDurationTicks / vexCount);

                new BukkitRunnable() {
                    private int spawned = 0;

                    @Override
                    public void run() {
                        if (!powerMob.isValid()) {
                            finish();
                            return;
                        }

                        if (!burstTarget.isValid() || burstTarget.isDead()) {
                            finish();
                            return;
                        }

                        spawnChargingVex(powerMob, burstTarget, vexDamage, spawnDistance, velocityTickSpeed, hitboxSize);
                        spawned++;

                        if (spawned >= vexCount) {
                            finish();
                        }
                    }

                    private void finish() {
                        cancel();
                        activeBursts.remove(mobUuid);
                        cooldowns.put(mobUuid, System.currentTimeMillis());
                    }
                }.runTaskTimer(plugin, 0L, intervalTicks);
            }

            private void cleanup() {
                cancel();
                activeTasks.remove(mobUuid);
                activeBursts.remove(mobUuid);
            }
        };

        BukkitTask scheduledTask = task.runTaskTimer(this.plugin, 0L, 20L);
        this.activeTasks.put(mobUuid, scheduledTask);

    }

    private Player findTarget(PowerMob powerMob, int maxDistance, boolean onlyTarget) {
        LivingEntity mobEntity = powerMob.getEntity();
        if (mobEntity == null || !mobEntity.isValid()) {
            return null;
        }

        Location mobLocation = mobEntity.getLocation();

        Player aggroTarget = getAggroTarget(mobEntity, mobLocation, maxDistance);
        if (onlyTarget) {
            return aggroTarget;
        }

        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canUsePlayerTarget(mobEntity, player, mobLocation, maxDistance)) {
                continue;
            }
            if (!MobTargetingUtil.shouldAllowTargeting(this.plugin, mobEntity, player)) {
                continue;
            }
            nearbyPlayers.add(player);
        }

        if (nearbyPlayers.isEmpty()) {
            return null;
        }

        return nearbyPlayers.get(ThreadLocalRandom.current().nextInt(nearbyPlayers.size()));
    }

    private Player getAggroTarget(LivingEntity mobEntity, Location mobLocation, int maxDistance) {
        if (!(mobEntity instanceof Mob mob)) {
            return null;
        }

        if (!(mob.getTarget() instanceof Player player)) {
            return null;
        }

        return canUsePlayerTarget(mobEntity, player, mobLocation, maxDistance) ? player : null;
    }

    private boolean canUsePlayerTarget(LivingEntity mobEntity, Player player, Location mobLocation, int maxDistance) {
        if (player == null || !player.isOnline() || !player.isValid() || player.isDead()) {
            return false;
        }

        if (!player.getWorld().equals(mobLocation.getWorld())) {
            return false;
        }

        if (player.getLocation().distanceSquared(mobLocation) > (double) maxDistance * maxDistance) {
            return false;
        }

        return MobTargetingUtil.shouldAllowTargeting(this.plugin, mobEntity, player);
    }

    private void spawnChargingVex(PowerMob powerMob, Player target, int vexDamage, int spawnDistance, double velocityTickSpeed, double hitRadius) {
        LivingEntity mobEntity = powerMob.getEntity();
        if (mobEntity == null || !mobEntity.isValid() || target == null || !target.isValid() || target.isDead()) {
            return;
        }

        Location targetPoint = target.getLocation().clone().add(0.0, 0.6, 0.0);

        Location spawnLocation = randomPointAround(targetPoint, spawnDistance);

        Vex vex = mobEntity.getWorld().spawn(spawnLocation, Vex.class, spawned -> {
            spawned.setAware(false);
            spawned.setTarget(null);
            spawned.setCharging(true);
            spawned.setSilent(true);
            spawned.setInvulnerable(true);
            spawned.setGravity(false);
            spawned.setCustomNameVisible(false);
            spawned.setRemoveWhenFarAway(true);
            spawned.setPersistent(false);
            spawned.setHealth(Math.max(1.0, spawned.getHealth()));

            if (spawned.getAttribute(Attribute.MAX_HEALTH) != null) {
                spawned.setHealth(Math.min(spawned.getHealth(), Objects.requireNonNull(spawned.getAttribute(Attribute.MAX_HEALTH)).getValue()));
            }
        });

        if (vexDamage > 0 && vex.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            Objects.requireNonNull(vex.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(vexDamage);
        }

        Vector velocity = targetPoint.toVector()
                .subtract(spawnLocation.toVector());
        Vector keepVelocity = velocity.normalize().multiply(velocityTickSpeed);

        if (velocity.lengthSquared() > 0.0001) {
            vex.setVelocity(keepVelocity);
        }

        mobEntity.getWorld().spawnParticle(
                Particle.SMOKE,
                spawnLocation,
                14,
                0.25,
                0.25,
                0.25,
                0.02
        );
        mobEntity.getWorld().playSound(spawnLocation, Sound.BLOCK_CANDLE_EXTINGUISH, 0.3f, 0.2f);

        trackVexFlight(vex, targetPoint, vexDamage, mobEntity, keepVelocity, hitRadius);
    }

    public static Location randomPointAround(Location center, double radius) {
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
        double x = center.getX() + Math.cos(angle) * radius;
        double z = center.getZ() + Math.sin(angle) * radius;
        return new Location(center.getWorld(), x, center.getY(), z, center.getYaw(), center.getPitch());
    }

    private void trackVexFlight(Vex vex, Location aimPoint, int damage, LivingEntity source, Vector keepVelocity, double hitRadius) {
        new BukkitRunnable() {
            private boolean reachedTarget = false;
            private int removeDelayTicks = 0;

            @Override
            public void run() {
                if (!vex.isValid() || vex.isDead()) {
                    cancel();
                    return;
                }

                vex.setVelocity(keepVelocity);
                faceVelocity(vex, keepVelocity);

                Player hitPlayer = findHitPlayerByBoundingBox(vex, hitRadius);
                if (hitPlayer != null) {
                    try {
                        hitPlayer.damage(damage <= 0 ? 1.0 : damage, source);
                    } catch (Exception ignored) {
                    }

                    vex.getWorld().spawnParticle(
                            Particle.LARGE_SMOKE,
                            vex.getLocation(),
                            20,
                            0.35,
                            0.35,
                            0.35,
                            0.03
                    );
                    vex.remove();
                    cancel();
                    return;
                }

                double arrivalThreshold = 0.3;
                if (!reachedTarget && vex.getLocation().distanceSquared(aimPoint) <= arrivalThreshold * arrivalThreshold) {
                    reachedTarget = true;
                    removeDelayTicks = 10;
                }

                if (reachedTarget) {
                    removeDelayTicks--;
                    if (removeDelayTicks <= 0) {
                        vex.getWorld().spawnParticle(
                                Particle.LARGE_SMOKE,
                                vex.getLocation(),
                                20,
                                0.35,
                                0.35,
                                0.35,
                                0.03
                        );
                        vex.remove();
                        cancel();
                        return;
                    }
                }

                if (vex.getTicksLived() > 120) {
                    vex.remove();
                    cancel();
                }
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
    }

    private void faceVelocity(Vex vex, Vector velocity) {
        if (velocity == null || velocity.lengthSquared() <= 0.0001) {
            return;
        }

        Location facing = vex.getLocation().clone();
        facing.setDirection(velocity);
        vex.setRotation(facing.getYaw(), facing.getPitch());
    }

    private Player findHitPlayerByBoundingBox(Vex vex, double hitRadius) {
        BoundingBox contactBox = vex.getBoundingBox().expand(hitRadius, hitRadius, hitRadius);

        for (Player player : vex.getWorld().getPlayers()) {
            if (!player.isValid() || player.isDead()) {
                continue;
            }

            if (!player.getWorld().equals(vex.getWorld())) {
                continue;
            }

            if (contactBox.overlaps(player.getBoundingBox())) {
                return player;
            }
        }

        return null;
    }

    @Override
    public void remove(PowerMob powerMob) {
        UUID mobUuid = powerMob.getEntityUuid();
        this.cooldowns.remove(mobUuid);

        BukkitTask task = this.activeTasks.remove(mobUuid);
        if (task != null) {
            task.cancel();
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
                "chance", AbilityConfigField.chance("chance", this.defaultChance, "Chance to trigger each second"),
                "only-target", AbilityConfigField.bool("only-target", this.defaultOnlyTarget,"Determines if it will focus a random target or only one the mob has an agro on."),
                "max-distance", AbilityConfigField.integer("max-distance", this.defaultMaxDistance, "Max distance the ability can trigger against players"),
                "vex-damage", AbilityConfigField.integer("vex-damage", this.defaultVexDamage, "Sets the vexes attack damage"),
                "vex-count", AbilityConfigField.integer("vex-count", this.defaultVexCount, "How many consecutive vexes will spawn within the defined duration"),
                "duration-seconds", AbilityConfigField.integer("duration-seconds", this.defaultDurationSeconds, "How long the ability will last for"),
                "velocity-tick-speed", AbilityConfigField.dbl("velocity-tick-speed", this.defaultVelocityTickSpeed, "How many blocks per tick the vexes will move towards the target"),
                "hitbox-size", AbilityConfigField.dbl("hitbox-size", this.defaultHitboxSize, "The block range to make damage contact"),
                "spawn-distance", AbilityConfigField.integer("spawn-distance", this.defaultSpawnDistance, "How far away from the target the vexes will spawn"),
                "cooldown", AbilityConfigField.integer("cooldown", this.defaultCooldown, "Cooldown until it can trigger again"));
    }
}

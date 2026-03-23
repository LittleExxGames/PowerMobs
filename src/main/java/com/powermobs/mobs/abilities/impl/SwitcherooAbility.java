package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.*;
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

public class SwitcherooAbility extends AbstractAbility implements Listener {

    private final String title = "Switcheroo";
    private final String description = "Switches the mod with a player that has attacked it.";
    private final Material material = Material.ENDER_EYE;

    private final double defaultChance = 0.3;
    private final int defaultMaxDistance = 10;
    private final int defaultCooldown = 5;
    private final boolean defaultOnlyIfDamaged = false;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final long attackerToRemoveMS = 60_000L;
    private static final int maxAttackerHistory = 10;
    private final Map<UUID, List<PlayerAttackerInfo>> lastAttackers = new HashMap<>();
    private final Map<UUID, BukkitRunnable> cycleTasks = new HashMap<>();
    private final Random random = new Random();


    /**
     * Creates a new teleport ability
     *
     * @param plugin The plugin instance
     */
    public SwitcherooAbility(PowerMobsPlugin plugin) {
        super(plugin, "switcheroo");
        Bukkit.getPluginManager().registerEvents(this, plugin);
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

        if (powerMob.getAbilities().stream().noneMatch(a -> a.getId().equals(this.id))) return;


        UUID mobUuid = powerMob.getEntityUuid();
        Player attacker = resolvePlayerAttacker(event);

        // Always record attacker history
        if (attacker != null) {
            recordAttacker(mobUuid, attacker);
        }

        boolean onlyIfDamaged = powerMob.getAbilityBoolean(this.id, "only-if-damaged", defaultOnlyIfDamaged);
        if (!onlyIfDamaged) return;

        if (!passesChanceAndCooldown(mobUuid, powerMob)) return;

        int maxDistance = powerMob.getAbilityInt(this.id, "max-distance", defaultMaxDistance);

        Player target = pickTarget(mobUuid, entity, attacker, maxDistance);
        if (target == null) return;

        performSwitch(entity, target, mobUuid);
    }

    private void startCycleTask(PowerMob powerMob) {
        UUID mobUuid = powerMob.getEntityUuid();
        cancelCycleTask(mobUuid);

        int cooldownSeconds = powerMob.getAbilityInt(this.id, "cooldown", defaultCooldown);
        long intervalTicks = Math.max(1L, cooldownSeconds) * 20L;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity entity = powerMob.getEntity();
                if (entity.isDead() || !entity.isValid()) {
                    cancelCycleTask(mobUuid);
                    return;
                }

                if (!passesChanceAndCooldown(mobUuid, powerMob)) return;

                int maxDistance = powerMob.getAbilityInt(id, "max-distance", defaultMaxDistance);

                Player target = pickTarget(mobUuid, entity, null, maxDistance);
                if (target == null) return;

                performSwitch(entity, target, mobUuid);
            }
        };

        task.runTaskTimer(this.plugin, intervalTicks, intervalTicks);
        cycleTasks.put(mobUuid, task);
    }

    private void cancelCycleTask(UUID mobUuid) {
        BukkitRunnable existing = cycleTasks.remove(mobUuid);
        if (existing != null) existing.cancel();
    }

    /**
     * Picks a valid swap target.
     * Priority: current attacker (if valid) → random eligible attacker from history → random nearby player.
     */
    private Player pickTarget(UUID mobUuid, LivingEntity mob, Player currentAttacker, int maxDistance) {
        if (isValidTarget(currentAttacker, mob, maxDistance)) {
            return currentAttacker;
        }

        List<PlayerAttackerInfo> history = lastAttackers.getOrDefault(mobUuid, Collections.emptyList());
        List<Player> pool = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (PlayerAttackerInfo info : history) {
            if (now - info.lastAttackTime() > attackerToRemoveMS) continue;
            Player p = Bukkit.getPlayer(info.playerUuid());
            if (isValidTarget(p, mob, maxDistance)) {
                pool.add(p);
            }
        }

        if (!pool.isEmpty()) {
            Collections.shuffle(pool, this.random);
            return pool.get(0);
        }

        return findNearbyPlayer(mob, maxDistance);
    }

    private Player findNearbyPlayer(LivingEntity mob, int maxDistance) {
        List<Player> nearby = new ArrayList<>();
        for (Player p : mob.getWorld().getPlayers()) {
            if (isValidTarget(p, mob, maxDistance)) {
                nearby.add(p);
            }
        }
        if (nearby.isEmpty()) return null;
        Collections.shuffle(nearby, this.random);
        return nearby.get(0);
    }

    private boolean isValidTarget(Player player, LivingEntity mob, int maxDistance) {
        if (player == null || !player.isOnline() || player.isDead()) return false;
        if (!player.getWorld().equals(mob.getWorld())) return false;
        return player.getLocation().distanceSquared(mob.getLocation()) <= (double) maxDistance * maxDistance;
    }

    private void performSwitch(LivingEntity mob, Player player, UUID mobUuid) {
        Location mobLoc = mob.getLocation().clone();
        Location playerLoc = player.getLocation().clone();

        spawnEffects(mobLoc);
        spawnEffects(playerLoc);

        mob.teleport(playerLoc);
        player.teleport(mobLoc);

        spawnEffects(mob.getLocation());
        spawnEffects(player.getLocation());

        cooldowns.put(mobUuid, System.currentTimeMillis());
    }

    private void spawnEffects(Location loc) {
        loc.getWorld().spawnParticle(Particle.PORTAL, loc, 30, 0.5, 1.0, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    private boolean passesChanceAndCooldown(UUID mobUuid, PowerMob powerMob) {
        long lastUse = cooldowns.getOrDefault(mobUuid, 0L);
        int cooldownSeconds = powerMob.getAbilityInt(this.id, "cooldown", defaultCooldown);
        if (System.currentTimeMillis() - lastUse < cooldownSeconds * 1000L) return false;

        double chance = powerMob.getAbilityDouble(this.id, "chance", defaultChance);
        return Math.random() <= chance;
    }

    private void recordAttacker(UUID mobUuid, Player attacker) {
        List<PlayerAttackerInfo> list = lastAttackers.computeIfAbsent(mobUuid, k -> new ArrayList<>());

        // Remove any existing entry for this player to refresh it
        list.removeIf(info -> info.playerUuid().equals(attacker.getUniqueId()));
        list.add(new PlayerAttackerInfo(attacker.getUniqueId(), System.currentTimeMillis()));

        // Cap the list size, removing the oldest entries first
        while (list.size() > maxAttackerHistory) {
            list.remove(0);
        }
    }

    private Player resolvePlayerAttacker(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return null;
        Entity damager = byEntity.getDamager();
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    @Override
    public void apply(PowerMob powerMob) {
        boolean onlyIfDamaged = powerMob.getAbilityBoolean(this.id, "only-if-damaged", defaultOnlyIfDamaged);
        if (!onlyIfDamaged) {
            startCycleTask(powerMob);
        }
    }

    @Override
    public void remove(PowerMob powerMob) {
        UUID mobUuid = powerMob.getEntityUuid();
        cooldowns.remove(mobUuid);
        lastAttackers.remove(mobUuid);
        cancelCycleTask(mobUuid);
    }

    @Override
    public String getTitle() { return this.title;}

    @Override
    public String getDescription() { return this.description;}

    @Override
    public Material getMaterial() { return this.material;}

    @Override
    public List<String> getStatus() {
        return List.of();
    }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        return Map.of(
                "chance", AbilityConfigField.chance("chance", this.defaultChance, "Chance to trigger teleport upon receiving damage or to simply happen"),
                "max-distance", AbilityConfigField.integer("max-distance", this.defaultMaxDistance, "Max away distance the teleport can go"),
                "cooldown", AbilityConfigField.integer("cooldown", this.defaultCooldown, "Cooldown for teleportation upon receiving damage"),
                "only-if-damaged", AbilityConfigField.bool("only-if-damaged", defaultOnlyIfDamaged, "Only switch when the mob takes damage"));
    }

    /**
     * Stores information about the last player attacker
     */
    private record PlayerAttackerInfo(UUID playerUuid, long lastAttackTime) { }
}

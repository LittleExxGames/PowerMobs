package com.powermobs.mobs.equipment;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.equipment.items.Shape;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import com.powermobs.mobs.equipment.items.EffectType;
import com.powermobs.mobs.equipment.items.TargetType;
import com.powermobs.mobs.equipment.items.TriggerType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes and applies item effects
 */
public class ItemEffectProcessor {

    private final PowerMobsPlugin plugin;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private enum StackWhen {
        ALWAYS,

        CHANCE_SUCCESS,
        CHANCE_FAIL,

        APPLY_SUCCESS,
        APPLY_FAIL,

        ANY_FAIL
    }


    public ItemEffectProcessor(PowerMobsPlugin plugin) {
        this.plugin = plugin;

        // Schedule cooldown cleanup every 5 minutes
        if (plugin.getServer().getScheduler() != null) {
            plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupExpiredCooldowns, 6000L, 6000L);
        }
    }

    public void processEffect(ItemEffect effect, LivingEntity triggerEntity,
                              LivingEntity targetEntity, Location location) {

        processEffectInternal(effect, triggerEntity, targetEntity, location, null, false, new HashSet<>(), 0);
    }

    private void processEffectInternal(
            ItemEffect effect,
            LivingEntity triggerEntity,
            LivingEntity targetEntity,
            Location location,
            Map<String, Object> stackOverrides,
            boolean fromStack,
            Set<String> chain,
            int depth
    ) {
        if (effect == null) return;

        if (depth > 12) {
            plugin.getLogger().warning("[ItemEffects] effect-stack exceeded max depth (possible loop).");
            return;
        }

        String chainKey = effect.getItemId() + ":" + effect.getEffectId();
        if (!chain.add(chainKey)) {
            plugin.getLogger().warning("[ItemEffects] effect-stack loop detected at " + chainKey + " (skipping).");
            return;
        }

        if (effect.getCooldown() > 0 && triggerEntity != null) {
            String cooldownKey = createCooldownKey(triggerEntity, effect);
            if (isOnCooldown(cooldownKey, effect.getCooldown())) {
                return;
            }
        }

        final double chance = resolveChance(effect, stackOverrides, fromStack);
        final boolean chancePassed = random.nextDouble() <= chance;

        boolean appliedAnything = false;

        if (chancePassed) {
            if (effect.usesCenter()) {
                Location center = resolveCenterLocation(effect, triggerEntity, targetEntity, location);
                if (center != null) {
                    appliedAnything = applyCenteredEffect(effect, center, triggerEntity);
                }
            } else {
                List<LivingEntity> targets = getTargetEntities(effect, triggerEntity, targetEntity, location);
                if (targets != null && !targets.isEmpty()) {
                    for (LivingEntity target : targets) {
                        applyTargetEffect(effect, target, location, triggerEntity);
                    }
                    appliedAnything = true;
                }
            }

            if (appliedAnything && effect.getCooldown() > 0 && triggerEntity != null) {
                String cooldownKey = createCooldownKey(triggerEntity, effect);
                cooldowns.put(cooldownKey, System.currentTimeMillis());
            }
        }

        if (effect.hasEffectStack()) {
            triggerEffectStack(effect, triggerEntity, targetEntity, location, chain, depth, chancePassed, appliedAnything);
        }
    }

    private boolean applyCenteredEffect(ItemEffect effect, Location center, LivingEntity caster) {
        return switch (effect.getEffectType()) {
            case AOE_POTION -> applyAoePotionEffect(effect, center, caster);
            case PARTICLES -> {
                spawnParticles(effect, center);
                yield true;
            }
            case SOUND -> {
                playSound(effect, center);
                yield true;
            }
            default -> false;
        };
    }

    private Location resolveCenterLocation(ItemEffect effect, LivingEntity triggerEntity, LivingEntity targetEntity, Location providedLocation) {
        return switch (effect.getCenterType()) {
            case SELF, ATTACKER -> (triggerEntity != null) ? triggerEntity.getLocation() : providedLocation;
            case VICTIM -> (targetEntity != null) ? targetEntity.getLocation() : ((triggerEntity != null) ? triggerEntity.getLocation() : providedLocation);
            case LOCATION -> (providedLocation != null) ? providedLocation : ((triggerEntity != null) ? triggerEntity.getLocation() : null);
        };
    }

    private void triggerEffectStack(
            ItemEffect origin,
            LivingEntity triggerEntity,
            LivingEntity targetEntity,
            Location location,
            Set<String> chain,
            int depth,
            boolean chancePassed,
            boolean appliedAnything
    ) {
        if (plugin.getItemEffectManager() == null) return;

        for (Map.Entry<String, Map<String, Object>> entry : origin.getEffectStack().entrySet()) {
            String stackedEffectId = entry.getKey();
            Map<String, Object> overrides = entry.getValue() != null ? entry.getValue() : Map.of();

            EnumSet<StackWhen> whenSet = parseStackWhenSet(
                    overrides.get("when"),
                    origin.getItemId(),
                    origin.getEffectId(),
                    stackedEffectId
            );

            if (!shouldRunStackEntry(whenSet, chancePassed, appliedAnything)) {
                continue;
            }

            ItemEffect stacked = findEffectById(origin.getItemId(), stackedEffectId);
            if (stacked == null) {
                plugin.getLogger().warning("[ItemEffects] effect-stack references missing effect '" + stackedEffectId +
                        "' on item '" + origin.getItemId() + "' (origin effect '" + origin.getEffectId() + "').");
                continue;
            }

            if (!isContextCompatible(stacked, triggerEntity, targetEntity, location)) {
                plugin.getLogger().warning("[ItemEffects] effect-stack effect '" + stackedEffectId +
                        "' is not compatible with the trigger context of '" + origin.getItemId() + ":" + origin.getEffectId() + "' (skipping).");
                continue;
            }

            // - chance defaults to 1.0 for stacks
            // - if overrides define "chance", it replaces the stacked effect's own chance
            processEffectInternal(stacked, triggerEntity, targetEntity, location, overrides, true, new HashSet<>(chain), depth + 1);
        }
    }

    private static boolean shouldRunStackEntry(EnumSet<StackWhen> whenSet, boolean chancePassed, boolean appliedAnything) {
        if (whenSet.contains(StackWhen.ALWAYS)) return true;

        boolean chanceFail = !chancePassed;

        boolean applySuccess = chancePassed && appliedAnything;
        boolean applyFail = chancePassed && !appliedAnything;

        boolean anyFail = chanceFail || applyFail;

        for (StackWhen w : whenSet) {
            switch (w) {
                case CHANCE_SUCCESS -> { if (chancePassed) return true; }
                case CHANCE_FAIL -> { if (chanceFail) return true; }
                case APPLY_SUCCESS -> { if (applySuccess) return true; }
                case APPLY_FAIL -> { if (applyFail) return true; }
                case ANY_FAIL -> { if (anyFail) return true; }
                default -> { /* handled above */ }
            }
        }
        return false;
    }

    private EnumSet<StackWhen> parseStackWhenSet(Object raw, String originItemId, String originEffectId, String stackedEffectId) {
        EnumSet<StackWhen> out = EnumSet.noneOf(StackWhen.class);

        if (raw == null) {
            out.add(StackWhen.APPLY_SUCCESS);
            return out;
        }

        if (!(raw instanceof Iterable<?> it)) {
            plugin.getLogger().warning("[ItemEffects] effect-stack.when must be a YAML list for '" +
                    originItemId + ":" + originEffectId + "' -> '" + stackedEffectId + "'. " +
                    "Example: when: [APPLY_FAIL, CHANCE_FAIL]. Value was: " + raw);
            out.add(StackWhen.APPLY_SUCCESS);
            return out;
        }

        for (Object o : it) {
            addWhenToken(out, o, originItemId, originEffectId, stackedEffectId);
        }

        if (out.isEmpty()) {
            plugin.getLogger().warning("[ItemEffects] Invalid/empty effect-stack.when list for '" + originItemId + ":" + originEffectId +
                    "' -> '" + stackedEffectId + "'. Using default APPLY_SUCCESS.");
            out.add(StackWhen.APPLY_SUCCESS);
        }

        return out;
    }

    private void addWhenToken(EnumSet<StackWhen> out, Object tokenObj, String originItemId, String originEffectId, String stackedEffectId) {
        if (tokenObj == null) return;

        String token = tokenObj.toString();
        if (token.isEmpty()) return;

        try {
            out.add(StackWhen.valueOf(token.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("[ItemEffects] Invalid effect-stack.when token '" + token + "' for '" +
                    originItemId + ":" + originEffectId + "' -> '" + stackedEffectId + "'. Valid: " +
                    Arrays.toString(StackWhen.values()));
        }
    }

    private ItemEffect findEffectById(String itemId, String effectId) {
        var all = plugin.getItemEffectManager().getItemEffects(itemId);
        if (all == null) return null;

        for (ItemEffect e : all) {
            if (e == null) continue;
            if (effectId.equalsIgnoreCase(e.getEffectId())) {
                return e;
            }
        }
        return null;
    }

    private double resolveChance(ItemEffect effect, Map<String, Object> stackOverrides, boolean fromStack) {
        if (fromStack) {
            Object v = (stackOverrides != null) ? stackOverrides.get("chance") : null;
            if (v instanceof Number n) return clamp01(n.doubleValue());
            if (v instanceof String s) {
                try {
                    return clamp01(Double.parseDouble(s));
                } catch (NumberFormatException ex) {
                    plugin.getLogger().warning("[ItemEffects] Invalid effect-stack chance value '" + s + "' for " +
                            effect.getItemId() + ":" + effect.getEffectId() + ". Using default 1.0");
                    return 1.0;
                }
            }
            return 1.0;
        }

        return clamp01(effect.getChance());
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private String createCooldownKey(LivingEntity entity, ItemEffect effect) {
        return entity.getUniqueId() + ":" + effect.getEffectType() + ":" + effect.getTrigger() + ":" + System.identityHashCode(effect);
    }

    private void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> {
            long timePassed = (currentTime - entry.getValue()) / 1000;
            return timePassed > 3600; // Remove cooldowns older than 1 hour
        });
    }

    private List<LivingEntity> getTargetEntities(ItemEffect effect, LivingEntity triggerEntity,
                                                 LivingEntity targetEntity, Location location) {
        TargetType targetType = effect.getTargetType();
        double radius = effect.getRadius();
        List<LivingEntity> targets = new ArrayList<>();

        switch (targetType) {
            case SELF:
                if (triggerEntity != null) targets.add(triggerEntity);
                break;
            case VICTIM:
                LivingEntity victim =
                        (effect.getTrigger() == TriggerType.ON_HIT) ? targetEntity :
                                (effect.getTrigger() == TriggerType.ON_HIT_TAKEN) ? triggerEntity :
                                        targetEntity; // fallback
                if (victim != null) targets.add(victim);
                break;
            case ATTACKER:
                LivingEntity attacker =
                        (effect.getTrigger() == TriggerType.ON_HIT) ? triggerEntity :
                                (effect.getTrigger() == TriggerType.ON_HIT_TAKEN) ? targetEntity :
                                        triggerEntity; // fallback
                if (attacker != null) targets.add(attacker);
                break;
        }

        return targets;
    }

    private void applyTargetEffect(ItemEffect effect, LivingEntity target, Location location, LivingEntity caster) {
        switch (effect.getEffectType()) {
            case POTION -> applyPotionEffect(effect, target);
            case IGNITE -> {
                if (!hasPersistentFireImmunity(target)) {
                    target.setFireTicks(effect.getFireTicks());
                }
            }
            case KNOCKBACK -> applyKnockback(effect, target, caster, location);
            case HEAL -> applyHealing(effect, target);
            case PURE_DAMAGE -> {
                target.setHealth(Math.max(0, target.getHealth() - effect.getDamage()));
                // Handle in damage events - store damage bonus
            }
            case IMMUNITY -> applyImmunity(effect, target);
        }
    }

    private boolean applyPotionEffect(ItemEffect effect, LivingEntity target) {
        try {
            PotionEffectType potionType = PotionEffectType.getByName(effect.getPotionType().toUpperCase());
            if (potionType == null) {
                plugin.getLogger().warning("Invalid potion type: " + effect.getPotionType());
                return false;
            }

            int levelOneBased = Math.max(1, effect.getPotionLevel());
            if (hasPersistentPotionImmunity(target, potionType, levelOneBased)) return false;

            PotionEffect potionEffect = new PotionEffect(
                    potionType,
                    toTick(effect.getPotionDuration()),
                    levelOneBased - 1
            );

            return target.addPotionEffect(potionEffect);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid potion type: " + effect.getPotionType());
            return false;
        }
    }

    private boolean applyAoePotionEffect(ItemEffect effect, Location location, LivingEntity caster) {
        if (location == null || location.getWorld() == null) return false;

        int applied = 0;

        for (Entity entity : location.getWorld().getNearbyEntities(location,
                effect.getRadius(), effect.getRadius(), effect.getRadius())) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!shouldIncludeInAoe(effect, living, caster)) continue;

            if (applyPotionEffect(effect, living)) {
                applied++;
            }
        }

        return applied > 0;
    }

    private void applyKnockback(ItemEffect effect, LivingEntity target, LivingEntity caster, Location hitLocation) {
        if (target == null) return;

        LivingEntity knockbackTarget = target;
        Location from; // origin of knockback (impact/victim)
        Location to;   // entity being pushed (knockbackTarget)
        Location targetLoc = target.getLocation();

        switch (effect.getTargetType()) {
            case VICTIM -> {
                // Push victim away from caster or impact point
                if (caster != null) {
                    from = caster.getLocation();
                } else if (hitLocation != null) {
                    from = hitLocation;
                } else {
                    // Fallback: push away from a point slightly behind the victim
                    from = targetLoc.clone().subtract(targetLoc.getDirection());
                }
                to = targetLoc;
            }
            case SELF -> {
                // We want to move the caster, away from the victim / hit point
                if (caster == null) return;
                knockbackTarget = caster;
                Location casterLoc = caster.getLocation();

                if (hitLocation != null) {
                    from = hitLocation;
                } else if (target != null && target != caster) {
                    // If we have a distinct victim, use them as origin
                    from = targetLoc;
                } else {
                    // Fallback: push player backwards from where they're looking
                    from = casterLoc.clone().add(casterLoc.getDirection());
                }
                to = casterLoc;
            }
            default -> {
                // Fallback: push 'target' away from provided source location or caster
                if (hitLocation != null) {
                    from = hitLocation;
                } else if (caster != null) {
                    from = caster.getLocation();
                } else {
                    // Push away from a point slightly behind the target
                    from = targetLoc.clone().subtract(targetLoc.getDirection());
                }
                to = targetLoc;
            }
        }

        if (from.getWorld() == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }

        // Direction = to - from
        Vector dir = to.toVector().subtract(from.toVector());

        // Avoid zero-length (no direction)
        if (dir.lengthSquared() == 0.0) {
            // For SELF fallback: push backwards from look direction
            if (effect.getTargetType() == TargetType.SELF && caster != null) {
                dir = caster.getLocation().getDirection().multiply(-1);
            } else {
                return;
            }
        }

        dir.normalize();

        double resistance = 0.0;
        if (knockbackTarget.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
            resistance = knockbackTarget.getAttribute(Attribute.KNOCKBACK_RESISTANCE).getValue();
        }
        resistance = Math.max(0, resistance - 1);
        resistance = Math.max(0, 1 - resistance);

        double strength = effect.getKnockbackStrength() * resistance;

        dir.multiply(strength);
        dir.setY(Math.max(0.3, dir.getY()));

        if (!Double.isFinite(dir.getX()) || !Double.isFinite(dir.getY()) || !Double.isFinite(dir.getZ())) {
            plugin.getLogger().warning("[ItemEffects] Knockback vector not finite; skipping.");
            return;
        }

        knockbackTarget.setVelocity(dir);
    }

    private void applyHealing(ItemEffect effect, LivingEntity target) {
        double healAmount = effect.getHealing();
        double newHealth = Math.min(target.getMaxHealth(), target.getHealth() + healAmount);
        target.setHealth(newHealth);
    }

    private void spawnParticles(ItemEffect effect, Location location) {
        if (location == null || location.getWorld() == null) return;

        final Particle particle;
        try {
            particle = Particle.valueOf(effect.getParticleType().toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().warning("[ItemEffects] Invalid particle type '" + effect.getParticleType() + "' for " +
                    effect.getItemId() + ":" + effect.getEffectId());
            return;
        }

        final int perTickCount = effect.getParticleCount();
        if (perTickCount <= 0) return;

        final double radius = effect.getParticleRadius();
        final Shape shape = effect.getParticleShape();

        Runnable burst = () -> spawnParticleBurst(location, particle, perTickCount, radius, shape);

        int durationSeconds = effect.getParticleDurationSeconds();
        if (durationSeconds <= 0) {
            burst.run();
            return;
        }

        int interval = effect.getParticleIntervalTicks();
        int runs = Math.max(1, (durationSeconds * 20) / interval);

        final int[] remaining = { runs };
        final BukkitTask[] task = { null };

        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (remaining[0]-- <= 0) {
                if (task[0] != null) task[0].cancel();
                return;
            }
            burst.run();
        }, 0L, interval);
    }

    private void spawnParticleBurst(Location base, Particle particle, int count, double radius, Shape shape) {
        if (base.getWorld() == null) return;

        Location origin = base.clone();

        for (int i = 0; i < count; i++) {
            Vector offset = switch (shape) {
                case SQUARE -> randomPointInSquare(radius);
                case CIRCLE ->  randomPointInCircle(radius);
                case CUBE -> randomPointInCube(radius);
                case ORB -> randomPointInSphere(radius);
            };

            Location at = origin.clone().add(offset);
            origin.getWorld().spawnParticle(particle, at, 1, 0, 0, 0, 0);
        }
    }

    private Vector randomPointInSquare(double radius){
        if (radius <= 0) return new Vector(0, 0, 0);
        return new Vector(random.nextDouble() * radius * 2 - radius, 0.0, random.nextDouble() * radius * 2 - radius);
    }

    private Vector randomPointInCube(double radius){
        if (radius <= 0) return new Vector(0, 0, 0);
        return new Vector(random.nextDouble() * radius * 2 - radius, random.nextDouble() * radius * 2 - radius, random.nextDouble() * radius * 2 - radius);
    }

    private Vector randomPointInCircle(double radius) {
        if (radius <= 0) return new Vector(0, 0, 0);

        double t = random.nextDouble() * (Math.PI * 2);
        double r = Math.sqrt(random.nextDouble()) * radius;
        double x = Math.cos(t) * r;
        double z = Math.sin(t) * r;
        return new Vector(x, 0.0, z);
    }

    private Vector randomPointInSphere(double radius) {
        if (radius <= 0) return new Vector(0, 0, 0);

        while (true) {
            double x = (random.nextDouble() * 2 - 1) * radius;
            double y = (random.nextDouble() * 2 - 1) * radius;
            double z = (random.nextDouble() * 2 - 1) * radius;
            if ((x * x + y * y + z * z) <= (radius * radius)) {
                return new Vector(x, y, z);
            }
        }
    }

    private void playSound(ItemEffect effect, Location location) {
        if (location.getWorld() == null) return;

        try {
            Sound sound = Sound.valueOf(effect.getSoundType().toUpperCase());
            location.getWorld().playSound(location, sound, effect.getSoundVolume(), effect.getSoundPitch());
        } catch (Exception e) {
            plugin.getLogger().warning("[ItemEffects] Invalid sound type '" + effect.getSoundType() + "' for " +
                    effect.getItemId() + ":" + effect.getEffectId());
        }
    }

    private void applyImmunity(ItemEffect effect, LivingEntity target) {
        if (target == null) return;

        // Clear fire
        if (effect.isClearFire()) {
            target.setFireTicks(0);
        }

        // Level-aware cleanse: remove only effects that are configured immune at that level or below
        if (effect.getImmunePotionMaxLevels() != null && !effect.getImmunePotionMaxLevels().isEmpty()) {
            for (PotionEffect pe : target.getActivePotionEffects()) {
                int levelOneBased = pe.getAmplifier() + 1; // amplifier is 0-based
                int maxImmune = effect.getImmuneMaxLevelFor(pe.getType());
                if (maxImmune >= 1 && levelOneBased <= maxImmune) {
                    target.removePotionEffect(pe.getType());
                }
            }
        }
    }

    /**
     * Determines if an entity should be included in AOE effects based on configuration flags
     */
    private boolean shouldIncludeInAoe(ItemEffect effect, LivingEntity target, LivingEntity caster) {
        if (target == null) return false;

        // Check if target is the caster
        if (target.equals(caster)) {
            return effect.isIncludeSelf();
        }

        // Check if target is a player (but not the caster)
        if (target instanceof Player) {
            return effect.isIncludePlayers();
        }

        // Check if target is an ally of the caster
        if (isAlly(caster, target)) {
            return effect.isIncludeAllies();
        }

        // Target is neither caster, player, nor ally - it's an "other" entity
        return effect.isIncludeOthers();
    }


    private boolean hasPersistentPotionImmunity(LivingEntity target, PotionEffectType type, int levelOneBased) {
        if (plugin.getItemEffectManager() == null || target == null || type == null) return false;

        for (ItemEffect immunity : collectPersistentImmunities(target)) {
            int maxImmune = immunity.getImmuneMaxLevelFor(type);
            if (maxImmune >= 1 && levelOneBased <= maxImmune) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPersistentFireImmunity(LivingEntity target) {
        if (plugin.getItemEffectManager() == null || target == null) return false;

        for (ItemEffect immunity : collectPersistentImmunities(target)) {
            if (immunity.isClearFire()) return true;
        }
        return false;
    }

    private List<ItemEffect> collectPersistentImmunities(LivingEntity entity) {
        List<ItemEffect> out = new ArrayList<>();
        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            // Hands -> HOLDING
            addItemImmunities(out, eq.getItemInMainHand(), TriggerType.HOLDING);
            addItemImmunities(out, eq.getItemInOffHand(), TriggerType.HOLDING);

            // Armor -> EQUIPPED
            addItemImmunities(out, eq.getHelmet(), TriggerType.EQUIPPED);
            addItemImmunities(out, eq.getChestplate(), TriggerType.EQUIPPED);
            addItemImmunities(out, eq.getLeggings(), TriggerType.EQUIPPED);
            addItemImmunities(out, eq.getBoots(), TriggerType.EQUIPPED);
        }

        // Inventory-based persistent immunities (HOTBAR/INVENTORY)
        if (entity instanceof HumanEntity human) {
            var pinv = human.getInventory();

            // HOTBAR immunities
            for (int i = 0; i < 9; i++) {
                addItemImmunities(out, pinv.getItem(i), TriggerType.HOTBAR);
            }

            // INVENTORY immunities
            var storage = pinv.getStorageContents();
            if (storage != null) {
                for (ItemStack stack : storage) {
                    addItemImmunities(out, stack, TriggerType.INVENTORY);
                }
            }
        }

        return out;
    }

    private void addItemImmunities(List<ItemEffect> out, ItemStack item, TriggerType expectedTrigger) {
        if (item == null || plugin.getItemEffectManager() == null) return;

        for (ItemEffect e : plugin.getItemEffectManager().getItemEffects(item)) {
            if (e.getEffectType() != EffectType.IMMUNITY) continue;
            if (e.getCooldown() != 0) continue;                 // persistent only
            if (e.getTargetType() != TargetType.SELF) continue; // applies to holder only
            if (e.getTrigger() != expectedTrigger) continue;    // holding/equipped scope
            out.add(e);
        }
    }

    private boolean isOnCooldown(String key, int cooldownSeconds) {
        if (!cooldowns.containsKey(key)) {
            return false;
        }

        long lastUse = cooldowns.get(key);
        long timePassed = (System.currentTimeMillis() - lastUse) / 1000;
        return timePassed < cooldownSeconds;
    }

    private boolean isAlly(LivingEntity entity1, LivingEntity entity2) {
        if (entity1 == null || entity2 == null) return false;

        // Check for tamed animals
        if (entity1 instanceof Player player && entity2 instanceof Tameable tameable) {
            return tameable.isTamed() && tameable.getOwner() != null && tameable.getOwner().equals(player);
        }

        if (entity2 instanceof Player player && entity1 instanceof Tameable tameable) {
            return tameable.isTamed() && tameable.getOwner() != null && tameable.getOwner().equals(player);
        }

        // Check if both are tamed by the same owner
        if (entity1 instanceof Tameable tame1 && entity2 instanceof Tameable tame2) {
            return tame1.isTamed() && tame2.isTamed() &&
                    tame1.getOwner() != null && tame1.getOwner().equals(tame2.getOwner());
        }


        return entity1.getType() == entity2.getType();
    }

    /**
     *  Verify that the effects triggered are able to use the same context as the origin effect
     * @param effect The new effect being applied
     * @param triggerEntity The entity that triggered the origin effect
     * @param targetEntity The entity that was targeted by the origin effect
     * @param location The location that was targeted by the origin effect
     * @return If the context is compatible
     */
    private boolean isContextCompatible(ItemEffect effect, LivingEntity triggerEntity, LivingEntity targetEntity, Location location) {
        // Center-based effects depend on center requirements, not target requirements
        if (effect.usesCenter()) {
            return switch (effect.getCenterType()) {
                case SELF, ATTACKER -> triggerEntity != null;
                case VICTIM -> targetEntity != null || triggerEntity != null; // allows fallback center
                case LOCATION -> location != null || triggerEntity != null;   // allows fallback center
            };
        }

        // Entity-targeted effects
        return switch (effect.getTargetType()) {
            case SELF -> triggerEntity != null;
            case VICTIM -> switch (effect.getTrigger()) {
                case ON_HIT -> targetEntity != null;
                case ON_HIT_TAKEN -> triggerEntity != null;
                default -> (triggerEntity != null || targetEntity != null);
            };
            case ATTACKER -> switch (effect.getTrigger()) {
                case ON_HIT -> triggerEntity != null;
                case ON_HIT_TAKEN -> targetEntity != null;
                default -> (triggerEntity != null || targetEntity != null);
            };
        };
    }

    private int toTick(int seconds) {
        return Math.max(0, seconds) * 20;
    }
}


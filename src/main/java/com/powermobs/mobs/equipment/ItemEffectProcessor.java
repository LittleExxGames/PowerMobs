package com.powermobs.mobs.equipment;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.equipment.items.EffectType;
import com.powermobs.mobs.equipment.items.TargetType;
import com.powermobs.mobs.equipment.items.TriggerType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes and applies item effects
 */
public class ItemEffectProcessor {

    private final PowerMobsPlugin plugin;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public ItemEffectProcessor(PowerMobsPlugin plugin) {
        this.plugin = plugin;

        // Schedule cooldown cleanup every 5 minutes
        if (plugin.getServer().getScheduler() != null) {
            plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupExpiredCooldowns, 6000L, 6000L);
        }
    }

    /**
     * Processes an item effect
     */
    public void processEffect(ItemEffect effect, LivingEntity triggerEntity,
                              LivingEntity targetEntity, Location location) {

        if (effect == null) return;

        if (random.nextDouble() > effect.getChance()) {
            return;
        }

        // Create unique cooldown key per entity + effect + item combination
        String cooldownKey = createCooldownKey(triggerEntity, effect);
        if (isOnCooldown(cooldownKey, effect.getCooldown())) {
            return;
        }

        // Get target entities based on effect configuration
        List<LivingEntity> targets = getTargetEntities(effect, triggerEntity, targetEntity, location);

        // Apply the effect to all targets
        for (LivingEntity target : targets) {
            applyEffect(effect, target, location, triggerEntity);
        }

        // Set cooldown after successful application
        if (effect.getCooldown() > 0) {
            cooldowns.put(cooldownKey, System.currentTimeMillis());
        }
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

    private void applyEffect(ItemEffect effect, LivingEntity target, Location location, LivingEntity caster) {
        switch (effect.getEffectType()) {
            case POTION -> applyPotionEffect(effect, target);
            case AOE_POTION -> applyAoePotionEffect(effect, location != null ? location : target.getLocation(), caster);
            case IGNITE -> {
                if (!hasPersistentFireImmunity(target)) {
                    target.setFireTicks(effect.getFireTicks());
                }
            }
            case KNOCKBACK -> applyKnockback(effect, target, caster, location);
            case HEAL -> applyHealing(effect, target);
            case PARTICLES -> spawnParticles(effect, location != null ? location : target.getLocation());
            case SOUND -> playSound(effect, location != null ? location : target.getLocation());
            case PURE_DAMAGE -> {
                target.setHealth(Math.max(0, target.getHealth() - effect.getDamage()));
                // Handle in damage events - store damage bonus
            }
            case IMMUNITY -> applyImmunity(effect, target);
        }
    }

    private void applyPotionEffect(ItemEffect effect, LivingEntity target) {
        try {
            PotionEffectType potionType = PotionEffectType.getByName(effect.getPotionType().toUpperCase());
            if (potionType != null) {
                int levelOneBased = Math.max(1, effect.getPotionLevel());
                if (hasPersistentPotionImmunity(target, potionType, levelOneBased)) return;
                PotionEffect potionEffect = new PotionEffect(potionType,
                        toTick(effect.getPotionDuration()), levelOneBased - 1);
                target.addPotionEffect(potionEffect);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid potion type: " + effect.getPotionType());
        }
    }

    private void applyAoePotionEffect(ItemEffect effect, Location location, LivingEntity caster) {
        if (location == null || location.getWorld() == null) return;

        for (Entity entity : location.getWorld().getNearbyEntities(location,
                effect.getRadius(), effect.getRadius(), effect.getRadius())) {
            if (entity instanceof LivingEntity living) {
                if (shouldIncludeInAoe(effect, living, caster)) {
                    applyPotionEffect(effect, living);
                }
            }
        }
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
        if (location.getWorld() == null) return;

        try {
            Particle particle = Particle.valueOf(effect.getParticleType().toUpperCase());
            location.getWorld().spawnParticle(particle, location, 20, 1, 1, 1, 0.1);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid particle type: " + effect.getParticleType());
        }
    }

    private void playSound(ItemEffect effect, Location location) {
        if (location.getWorld() == null) return;

        try {
            Sound sound = Sound.valueOf(effect.getSoundType().toUpperCase());
            location.getWorld().playSound(location, sound, effect.getSoundVolume(), effect.getSoundPitch());
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound type: " + effect.getSoundType());
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
        if (entity1 instanceof Player player && entity2 instanceof org.bukkit.entity.Tameable tameable) {
            return tameable.isTamed() && tameable.getOwner() != null && tameable.getOwner().equals(player);
        }

        if (entity2 instanceof Player player && entity1 instanceof org.bukkit.entity.Tameable tameable) {
            return tameable.isTamed() && tameable.getOwner() != null && tameable.getOwner().equals(player);
        }

        // Check if both are tamed by the same owner
        if (entity1 instanceof org.bukkit.entity.Tameable tame1 && entity2 instanceof org.bukkit.entity.Tameable tame2) {
            return tame1.isTamed() && tame2.isTamed() &&
                    tame1.getOwner() != null && tame1.getOwner().equals(tame2.getOwner());
        }


        return entity1.getType() == entity2.getType();
    }

    private int toTick(int seconds) {
        return Math.max(0, seconds) * 20;
    }
}


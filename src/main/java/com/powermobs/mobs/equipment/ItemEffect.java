package com.powermobs.mobs.equipment;

import com.powermobs.mobs.equipment.items.*;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a custom effect that can be applied to items
 */
@Getter
public class ItemEffect {
    private final String itemId;
    private final String effectId;
    private final Map<String, Map<String, Object>> effectStack;

    private final TriggerType trigger;
    private final EffectType effectType;
    private final double chance;
    private final int cooldown;

    private final boolean targetProvided;
    private final TargetType targetType; //Single target
    private final CenterType centerType; //AOE

    // Effect-specific properties
    private final String potionType;
    private final int potionLevel;
    private final int potionDuration;
    private final double damage;
    private final double healing;
    private final int fireTicks;
    private final double knockbackStrength;

    private final String particleType;
    private final Shape particleShape;
    private final double particleRadius;
    private final int particleCount;
    private final int particleDurationSeconds;
    private final int particleIntervalTicks;


    private final String soundType;
    private final float soundVolume;
    private final float soundPitch;

    // AOE options
    private final boolean includeSelf;     // caster
    private final boolean includeAllies;   // allied entities (e.g., tamed)
    private final boolean includePlayers;  // other players (not the caster)
    private final boolean includeOthers;   // non-ally, non-player entities
    private final double radius;

    // Immunity-specific
    private final List<String> immunePotionTypes;
    private final boolean clearFire;
    private final Map<String, Integer> immunePotionMaxLevels;
    private final boolean negateFallDamage;


    /**
     * Creates a new item effect from configuration
     */
    public ItemEffect(String itemId, ConfigurationSection section) {
        this.itemId = itemId;
        this.effectId = section.getName();

        this.trigger = parseEnum(TriggerType.class, section.getString("trigger", "HOLDING"));
        this.effectType = parseEnum(EffectType.class, section.getString("effect", "POTION"));

        this.targetProvided = section.contains("target");
        this.targetType = parseEnum(TargetType.class, section.getString("target", "SELF"));
        this.centerType = parseEnum(CenterType.class, section.getString("center", defaultCenter(this.trigger).name()));

        this.chance = Math.max(0.0, Math.min(1.0, section.getDouble("chance", 1.0)));
        this.cooldown = Math.max(0, section.getInt("cooldown", 0));

        this.potionType = section.getString("potion-type", "SPEED");
        this.potionLevel = Math.max(0, section.getInt("potion-level", 1));
        this.potionDuration = Math.max(0, section.getInt("potion-duration", 60));
        this.damage = Math.max(0.0, section.getDouble("damage", 1.0));
        this.healing = Math.max(0.0, section.getDouble("healing", 1.0));
        this.fireTicks = Math.max(0, section.getInt("fire-ticks", 60));
        this.knockbackStrength = Math.max(0.0, section.getDouble("knockback-strength", 1.0));
        this.radius = Math.max(0.0, section.getDouble("radius", 5.0));

        this.particleType = section.getString("particle-type", "HEART");
        this.particleShape = parseEnum(Shape.class, section.getString("particle-shape", "ORB"));
        this.particleRadius = Math.max(0.0, Math.min(10.0, section.getDouble("particle-radius", 1.0)));
        this.particleCount = Math.max(0, Math.min(200, section.getInt("particle-count", 20)));               // per interval
        this.particleDurationSeconds = Math.max(0, Math.min(30, section.getInt("particle-duration", 0)));     // 0 = single burst
        this.particleIntervalTicks = Math.max(1, Math.min(20, section.getInt("particle-interval-ticks", 5))); // 1–20


        this.soundType = section.getString("sound-type", "ENTITY_EXPERIENCE_ORB_PICKUP");
        this.soundVolume = (float) Math.max(0.0, section.getDouble("sound-volume", 1.0));
        this.soundPitch = (float) Math.max(0.0, section.getDouble("sound-pitch", 1.0));

        // AOE options
        this.includeSelf = section.getBoolean("aoe-include-caster", false);
        this.includeAllies = section.getBoolean("aoe-include-allies", true);
        this.includePlayers = section.getBoolean("aoe-include-players", true);
        this.includeOthers = section.getBoolean("aoe-include-others", true);

        // Immunity-specific
        this.immunePotionTypes = section.getStringList("immune-potion-types");
        this.clearFire = section.getBoolean("clear-fire", false);
        this.negateFallDamage = section.getBoolean("negate-fall-damage", false);

        // Parse TYPE[:LEVEL] entries into a lookup map (LEVEL is 1-based, absent = any level)
        Map<String, Integer> tmp = new HashMap<>();
        for (String entry : immunePotionTypes) {
            if (entry == null || entry.isBlank()) continue;
            String typeName = entry;
            int maxLevel = Integer.MAX_VALUE; // no level specified = any level
            int idx = entry.indexOf(':');
            if (idx > -1) {
                typeName = entry.substring(0, idx);
                String lvlStr = entry.substring(idx + 1).trim();
                try {
                    int parsed = Integer.parseInt(lvlStr);
                    maxLevel = Math.max(1, parsed); // clamp to 1+
                } catch (NumberFormatException ignored) {
                    // keep ANY if bad level
                }
            }
            if (!typeName.isBlank()) {
                tmp.put(typeName.trim().toUpperCase(), maxLevel);
            }
        }
        this.immunePotionMaxLevels = Collections.unmodifiableMap(tmp);

        ConfigurationSection stackSection = section.getConfigurationSection("effect-stack");
        Map<String, Map<String, Object>> tmpStack = new HashMap<>();
        if (stackSection != null) {
            for (String childId : stackSection.getKeys(false)) {
                ConfigurationSection childSection = stackSection.getConfigurationSection(childId);
                Map<String, Object> overrides = (childSection != null) ? childSection.getValues(true) : Collections.emptyMap();
                tmpStack.put(childId, overrides != null ? overrides : Collections.emptyMap());
            }
        }
        this.effectStack = Collections.unmodifiableMap(tmpStack);
    }

    public boolean hasEffectStack() {
        return effectStack != null && !effectStack.isEmpty();
    }

    private static CenterType defaultCenter(TriggerType trigger) {
        return switch (trigger) {
            case ON_HIT -> CenterType.VICTIM;
            case ON_HIT_TAKEN -> CenterType.SELF;
            case PROJECTILE_HIT -> CenterType.LOCATION;
            default -> CenterType.SELF;
        };
    }

    /**
     * Gets the max immune level configuration for the given effect type
     *
     * @param type The potion types max level to check for
     * @return The max level found for the potion effect
     */
    public int getImmuneMaxLevelFor(PotionEffectType type) {
        if (type == null) return -1;
        Integer lvl = immunePotionMaxLevels.get(type.getName().toUpperCase());
        return lvl == null ? -1 : lvl;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        if (value == null || value.trim().isEmpty()) {
            return enumClass.getEnumConstants()[0];
        }

        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return enumClass.getEnumConstants()[0];
        }
    }

    /** For 'at a place' or AOE effects */
    public boolean usesCenter() {
        return effectType == EffectType.AOE_POTION
                || effectType == EffectType.PARTICLES
                || effectType == EffectType.SOUND;
    }

    /** Effects that target a specific entity */
    public boolean usesTarget() {
        return !usesCenter();
    }

    public List<String> validateProblems() {
        List<String> problems = new java.util.ArrayList<>();

        if (usesTarget() && !targetProvided) {
            problems.add("Missing required 'target' (effect=" + effectType + ", trigger=" + trigger + ")");
        }
        if (usesCenter() && targetProvided) {
            problems.add("'target' is ignored for " + effectType + " (use 'center' instead)");
        }

        if (effectType == EffectType.POTION || effectType == EffectType.AOE_POTION) {
            PotionEffectType type = PotionEffectType.getByName(potionType.toUpperCase());
            if (type == null) {
                problems.add("Invalid potion-type '" + potionType + "'");
            }
        }

        return problems;
    }

    public boolean isValid() {
        return validateProblems().isEmpty();
    }
}

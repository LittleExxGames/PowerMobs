package com.powermobs.mobs.equipment;

import com.powermobs.mobs.equipment.items.EffectType;
import com.powermobs.mobs.equipment.items.TargetType;
import com.powermobs.mobs.equipment.items.TriggerType;
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

    private final TriggerType trigger;
    private final EffectType effectType;
    private final TargetType targetType;
    private final double chance;
    private final int cooldown;

    // Effect-specific properties
    private final String potionType;
    private final int potionLevel;
    private final int potionDuration;
    private final double damage;
    private final double healing;
    private final int fireTicks;
    private final double knockbackStrength;
    private final String particleType;
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
    public ItemEffect(ConfigurationSection section) {
        this.trigger = parseEnum(TriggerType.class, section.getString("trigger", "HOLDING"));
        this.effectType = parseEnum(EffectType.class, section.getString("effect", "POTION"));
        this.targetType = parseEnum(TargetType.class, section.getString("target", "SELF"));

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
            System.err.println("Invalid " + enumClass.getSimpleName() + ": " + value +
                    ". Using default: " + enumClass.getEnumConstants()[0]);
            return enumClass.getEnumConstants()[0];
        }
    }

    public boolean isValid() {
        if (effectType == EffectType.POTION || effectType == EffectType.AOE_POTION) {
            try {
                PotionEffectType.getByName(potionType.toUpperCase());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
}

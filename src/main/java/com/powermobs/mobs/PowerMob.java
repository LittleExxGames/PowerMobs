package com.powermobs.mobs;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.config.IPowerMobConfig;
import com.powermobs.mobs.abilities.Ability;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a power mob in the game
 */
@Getter
public class PowerMob {

    private static final String POWER_MOB_KEY = "powermob.id";
    private static final String POWER_MOB_UUID = "powermob.uuid";

    private final PowerMobsPlugin plugin;
    private final LivingEntity entity;
    private final String id;
    private final UUID powerMobUuid;
    private final boolean isRandom;
    private final List<Ability> abilities = new ArrayList<>();
    private final Map<String, Map<String, Object>> abilitySettings;

    /**
     * Creates a new power mob
     *
     * @param plugin The plugin instance
     * @param entity The entity to enhance
     * @param id     The power mob ID (config key or "random" for random mobs)
     */
    public PowerMob(PowerMobsPlugin plugin, LivingEntity entity, String id) {
        this(plugin, entity, id, Collections.emptyMap());
    }

    /**
     * Creates a new power mob with per-mob ability settings
     *
     * @param plugin          The plugin instance
     * @param entity          The entity to enhance
     * @param id              The power mob ID (config key or "random" for random mobs)
     * @param abilitySettings Per-mob ability settings (abilityId -> settings map)
     */
    public PowerMob(PowerMobsPlugin plugin, LivingEntity entity, String id, Map<String, Map<String, Object>> abilitySettings) {
        this.plugin = plugin;
        this.entity = entity;
        this.id = id;
        this.powerMobUuid = UUID.randomUUID();
        this.isRandom = "random".equals(id);
        this.abilitySettings = deepUnmodifiableCopy(abilitySettings);

        // Tag the entity with metadata
        this.entity.setMetadata(POWER_MOB_KEY, new FixedMetadataValue(plugin, id));

        // Store the UUID in the persistent data container
        PersistentDataContainer pdc = this.entity.getPersistentDataContainer();
        pdc.set(plugin.getPowerMobManager().getPowerMobKey(), PersistentDataType.STRING, this.powerMobUuid.toString());
    }

    /**
     * Gets per-mob settings for a specific ability.
     *
     * @param abilityId The ability ID
     * @return Unmodifiable settings map (empty if none)
     */
    public Map<String, Object> getAbilitySettings(String abilityId) {
        Map<String, Object> settings = this.abilitySettings.get(abilityId);
        return settings != null ? settings : Collections.emptyMap();
    }

    public String getAbilityString(String abilityId, String key, String defaultValue) {
        Object value = getAbilitySettings(abilityId).get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String s) {
            return s;
        }
        return String.valueOf(value);
    }

    public int getAbilityInt(String abilityId, String key, int defaultValue) {
        Number number = asNumber(getAbilitySettings(abilityId).get(key));
        return number != null ? number.intValue() : defaultValue;
    }

    public long getAbilityLong(String abilityId, String key, long defaultValue) {
        Number number = asNumber(getAbilitySettings(abilityId).get(key));
        return number != null ? number.longValue() : defaultValue;
    }

    public double getAbilityDouble(String abilityId, String key, double defaultValue) {
        Number number = asNumber(getAbilitySettings(abilityId).get(key));
        return number != null ? number.doubleValue() : defaultValue;
    }

    public boolean getAbilityBoolean(String abilityId, String key, boolean defaultValue) {
        Object value = getAbilitySettings(abilityId).get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            if (s.equalsIgnoreCase("true")) {
                return true;
            }
            if (s.equalsIgnoreCase("false")) {
                return false;
            }
        }
        return defaultValue;
    }

    private static Number asNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n;
        }
        if (value instanceof String s) {
            try {
                if (s.contains(".")) {
                    return Double.parseDouble(s);
                }
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Map<String, Map<String, Object>> deepUnmodifiableCopy(Map<String, Map<String, Object>> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Object>> outer = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : input.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Map<String, Object> inner = entry.getValue() != null
                    ? Collections.unmodifiableMap(new java.util.LinkedHashMap<>(entry.getValue()))
                    : Collections.emptyMap();
            outer.put(entry.getKey(), inner);
        }
        return Collections.unmodifiableMap(outer);
    }

    /**
     * Gets a power mob from an entity, if it exists
     *
     * @param plugin The plugin instance
     * @param entity The entity to check
     * @return The power mob, or null if the entity is not a power mob
     */
    public static PowerMob getFromEntity(PowerMobsPlugin plugin, LivingEntity entity) {
        if (entity == null) {
            return null;
        }

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(plugin.getPowerMobManager().getPowerMobKey(), PersistentDataType.STRING)) {
            String uuidString = pdc.get(plugin.getPowerMobManager().getPowerMobKey(), PersistentDataType.STRING);
            if (uuidString != null) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    return plugin.getPowerMobManager().getPowerMob(uuid);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Checks if an entity is a power mob
     *
     * @param plugin The plugin instance
     * @param entity The entity to check
     * @return True if the entity is a power mob
     */
    public static boolean isPowerMob(PowerMobsPlugin plugin, LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return entity.getPersistentDataContainer().has(
                plugin.getPowerMobManager().getPowerMobKey(),
                PersistentDataType.STRING
        );
    }

    /**
     * Applies the power mob properties to the entity
     *
     * @param config           The mob configuration
     * @param displayName      The display name
     * @param healthMultiplier The health multiplier
     * @param damageMultiplier The damage multiplier
     * @param speedMultiplier  The speed multiplier
     */
    public void applyProperties(IPowerMobConfig config, String displayName, double healthMultiplier,
                                double damageMultiplier, double speedMultiplier) {
        // Set display name
        if (this.plugin.getConfigManager().isShowNames()) {
            this.entity.setCustomName(ChatColor.translateAlternateColorCodes('&', displayName));
            this.entity.setCustomNameVisible(true);
        }

        // Apply health
        double baseMaxHealth = Objects.requireNonNull(
                this.entity.getAttribute(Attribute.MAX_HEALTH)
        ).getBaseValue();

        double newMaxHealth = config.getActualHealth() > 0
                ? config.getActualHealth()
                : baseMaxHealth * healthMultiplier;

        Objects.requireNonNull(
                this.entity.getAttribute(Attribute.MAX_HEALTH)
        ).setBaseValue(newMaxHealth);

        this.entity.setHealth(newMaxHealth);

        // Apply damage
        if (this.entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            double baseDamage = Objects.requireNonNull(
                    this.entity.getAttribute(Attribute.ATTACK_DAMAGE)
            ).getBaseValue();

            Objects.requireNonNull(
                    this.entity.getAttribute(Attribute.ATTACK_DAMAGE)
            ).setBaseValue(baseDamage * damageMultiplier);
        }

        // Apply speed
        if (this.entity.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            double baseSpeed = Objects.requireNonNull(
                    this.entity.getAttribute(Attribute.MOVEMENT_SPEED)
            ).getBaseValue();

            Objects.requireNonNull(
                    this.entity.getAttribute(Attribute.MOVEMENT_SPEED)
            ).setBaseValue(baseSpeed * speedMultiplier);
        }

        // Additional attributes
        if (config.getAttributes() != null) {
            for (String attrName : config.getAttributes().keySet()) {
                try {
                    Attribute attribute = Attribute.valueOf(attrName.toUpperCase().replace("-", "_"));
                    if (this.entity.getAttribute(attribute) != null) {
                        Objects.requireNonNull(
                                this.entity.getAttribute(attribute)
                        ).setBaseValue(config.getAttributes().get(attrName));
                    }
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Invalid attribute: " + attrName);
                }
            }
        }
    }

    /**
     * Adds an ability to the mob
     *
     * @param ability The ability to add
     */
    public void addAbility(Ability ability) {
        this.abilities.add(ability);
        ability.apply(this);
    }

    /**
     * Removes all abilities from the mob
     */
    public void removeAbilities() {
        for (Ability ability : this.abilities) {
            ability.remove(this);
        }
        this.abilities.clear();
    }

    /**
     * Checks if the entity is valid (not dead or removed)
     *
     * @return True if the entity is valid
     */
    public boolean isValid() {
        return this.entity != null && !this.entity.isDead() && this.entity.isValid();
    }

    /**
     * Removes this power mob
     */
    public void remove() {
        // Cancel any active despawn task
        plugin.getSpawnTimerManager().cancelDespawnTask(this);

        removeAbilities();
        if (isValid()) {
            this.entity.removeMetadata(POWER_MOB_KEY, this.plugin);
        }
    }

    /**
     * Gets the UUID of the entity
     *
     * @return The entity UUID
     */
    public UUID getEntityUuid() {
        return this.entity.getUniqueId();
    }

    /**
     * Checks if a player can see this mob
     *
     * @param player The player to check
     * @return True if the player can see the mob
     */
    public boolean isVisibleTo(Player player) {
        return player.getWorld().equals(this.entity.getWorld()) &&
                player.getLocation().distance(this.entity.getLocation()) <= 100;
    }

    /**
     * Gets the location description of the mob
     *
     * @return The location description
     */
    public String getLocationDescription() {
        return String.format("%s at [%d, %d, %d]",
                this.entity.getWorld().getName(),
                this.entity.getLocation().getBlockX(),
                this.entity.getLocation().getBlockY(),
                this.entity.getLocation().getBlockZ()
        );
    }
}
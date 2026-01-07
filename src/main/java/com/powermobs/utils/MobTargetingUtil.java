package com.powermobs.utils;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;

import java.util.HashSet;
import java.util.Set;


/**
 * Utility class for handling mob targeting logic
 */
public class MobTargetingUtil {
    private static final Set<EntityType> APPROVED_HUNTER_ENTITIES = new HashSet<>();
    private static boolean initialized = false;

    /**
     * Initializes the targeting system with configuration values
     *
     * @param plugin The plugin instance
     */
    public static void initialize(PowerMobsPlugin plugin) {
        if (initialized) {
            return;
        }

        APPROVED_HUNTER_ENTITIES.clear();

        // Default entities that can hunt power mobs
        APPROVED_HUNTER_ENTITIES.add(EntityType.IRON_GOLEM);
        APPROVED_HUNTER_ENTITIES.add(EntityType.WOLF);
        APPROVED_HUNTER_ENTITIES.add(EntityType.PLAYER);
        APPROVED_HUNTER_ENTITIES.add(EntityType.SNOW_GOLEM);

        // Load from config if available
        ConfigurationSection targetingConfig = plugin.getConfigManager().getConfig()
                .getConfigurationSection("targeting");

        if (targetingConfig != null) {
            if (targetingConfig.isList("approved-hunters")) {
                APPROVED_HUNTER_ENTITIES.clear();
                for (String entityName : targetingConfig.getStringList("approved-hunters")) {
                    try {
                        EntityType type = EntityType.valueOf(entityName.toUpperCase());
                        APPROVED_HUNTER_ENTITIES.add(type);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid entity type in approved-hunters: " + entityName);
                    }
                }
            }
        }

        initialized = true;
    }

    /**
     * Checks if the source entity should be allowed to target the target entity
     *
     * @param plugin The plugin instance
     * @param source The source entity (attacker)
     * @param target The target entity (potential victim)
     * @return True if targeting should be allowed, false if it should be prevented
     */
    public static boolean shouldAllowTargeting(PowerMobsPlugin plugin, Entity source, Entity target) {
        if (!initialized) {
            initialize(plugin);
        }

        // If either entity is not a living entity, allow targeting (shouldn't normally happen)
        if (!(source instanceof LivingEntity livingSource) || !(target instanceof LivingEntity livingTarget)) {
            return true;
        }

        // Check if the target is a player in creative or spectator mode
        if (livingTarget instanceof Player playerTarget) {
            GameMode gameMode = playerTarget.getGameMode();
            // Only allow targeting players in survival or adventure mode
            if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE) {
                return false;
            }
        }


        // Check if source is a power mob
        boolean sourceIsPowerMob = PowerMob.isPowerMob(plugin, livingSource);

        // Check if target is a power mob
        boolean targetIsPowerMob = PowerMob.isPowerMob(plugin, livingTarget);

        // Case 1: Power mobs should not target other power mobs
        if (sourceIsPowerMob && targetIsPowerMob) {
            return false;
        }

        // Case 2: Vanilla mobs targeting power mobs
        if (!sourceIsPowerMob && targetIsPowerMob) {
            // Check if this entity type is allowed to hunt power mobs
            if (APPROVED_HUNTER_ENTITIES.contains(source.getType())) {
                // Special case for wolves: only player-owned wolves can hunt
                if (source.getType() == EntityType.WOLF) {
                    Wolf wolf = (Wolf) source;
                    return wolf.isTamed() && wolf.getOwner() != null;
                }

                // Special case for iron golems: only player-created ones can hunt
                if (source.getType() == EntityType.IRON_GOLEM) {
                    IronGolem golem = (IronGolem) source;
                    return golem.isPlayerCreated();
                }

                // All other approved hunters can hunt
                return true;
            }

            // Not an approved hunter
            return false;
        }

        // Case 3: Power mobs targeting vanilla entities
        if (sourceIsPowerMob && !targetIsPowerMob) {
            // Power mobs can target players (game mode check happens elsewhere)
            if (target instanceof Player) {
                return true;
            }

            // Power mobs can target player's tamed animals
            if (target instanceof Tameable tameable) {
                return tameable.isTamed() && tameable.getOwner() != null;
            }

            // Power mobs can target player-created golems
            if (target instanceof IronGolem golem) {
                return golem.isPlayerCreated();
            }

            // Power mobs can target snowmen
            return target instanceof Snowman;

            // Power mobs should not target other mobs by default
        }

        // All other cases are allowed
        return true;
    }

    /**
     * Adds an entity type to the approved hunters list
     *
     * @param entityType The entity type to add
     */
    public static void addApprovedHunter(EntityType entityType) {
        APPROVED_HUNTER_ENTITIES.add(entityType);
    }

    /**
     * Removes an entity type from the approved hunters list
     *
     * @param entityType The entity type to remove
     */
    public static void removeApprovedHunter(EntityType entityType) {
        APPROVED_HUNTER_ENTITIES.remove(entityType);
    }

    /**
     * Clears the approved hunters list
     */
    public static void clearApprovedHunters() {
        APPROVED_HUNTER_ENTITIES.clear();
    }
}

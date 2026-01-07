package com.powermobs.mobs;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.config.EquipmentItemConfig;
import com.powermobs.config.PowerMobConfig;
import com.powermobs.config.RandomMobConfig;
import com.powermobs.config.SpawnCondition;
import com.powermobs.mobs.abilities.Ability;
import com.powermobs.utils.HostileEntityTypes;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Factory for creating power mobs
 */
@RequiredArgsConstructor
public class PowerMobFactory {

    private final PowerMobsPlugin plugin;
    private final Random random = new Random();
    private final Set<EntityType> randomRestrictedMobs = new HashSet<>(List.of(
            EntityType.WITHER,
            EntityType.WARDEN,
            EntityType.ENDER_DRAGON,
            EntityType.ELDER_GUARDIAN,
            EntityType.RAVAGER,
            EntityType.PHANTOM));
    private final Set<EntityType> randomNetherLimitedMobs = new HashSet<>(List.of(
            EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE,
            EntityType.HOGLIN,
            EntityType.GHAST));
    private final Set<EntityType> randomEndLimitedMobs = new HashSet<>(List.of(
            EntityType.SHULKER));
    private final Set<EntityType> allMobs = new HashSet<>(List.of(EntityType.values()));

    /**
     * Creates a power mob from a predefined configuration
     *
     * @param entity   The entity to enhance
     * @param configId The configuration ID
     * @return The power mob, or null if the configuration is invalid
     */
    public PowerMob createPowerMob(LivingEntity entity, String configId) {
        PowerMobConfig config = this.plugin.getConfigManager().getPowerMob(configId);
        if (config == null) {
            this.plugin.getLogger().warning("Invalid power mob configuration: " + configId);
            return null;
        }

        if (entity.getType() != config.getEntityType() && config.getSpawnCondition().isReplaceTypeOnly()) {
            this.plugin.getLogger().warning("Entity type mismatch for power mob " + configId +
                    ": expected " + config.getEntityType() + ", got " + entity.getType());
            return null;
        }
        entity = replaceEntity(entity, config.getEntityType());

        // Create the power mob
        PowerMob powerMob = new PowerMob(this.plugin, entity, configId);

        // Apply properties
        powerMob.applyProperties(
                config,
                config.getName(),
                config.getActualHealthMultiplier(), // Power Mob is default 1.0
                config.getActualDamageMultiplier(),
                config.getActualSpeedMultiplier()
        );

        // Apply abilities
        for (String abilityId : config.getPossibleAbilities()) {
            Ability ability = this.plugin.getAbilityManager().getAbility(abilityId);
            if (ability != null) {
                powerMob.addAbility(ability);
            } else {
                this.plugin.getLogger().warning("Invalid ability: " + abilityId);
            }
        }

        // Apply equipment
        for (String slot : config.getPossibleEquipment().keySet()) {
            if (!config.getPossibleEquipment().get(slot).isEmpty()) {
                EquipmentItemConfig equipmentConfig = config.getPossibleEquipment().get(slot).get(0);
                this.plugin.getEquipmentManager().applyEquipment(powerMob, slot, equipmentConfig);
            }
        }

        if (config.isGlowing()) {
            applyGlowingEffect(entity, config.getActualGlowTime());
        }

        // Schedule despawn timer using SpawnTimerManager
        int despawnTime = config.getSpawnCondition().getActualDespawnTime();
        if (despawnTime > 0) {
            this.plugin.getSpawnTimerManager().scheduleDespawnTask(powerMob, despawnTime);
        }

        return powerMob;
    }

    /**
     * Creates a random power mob
     *
     * @param entity The entity to enhance
     * @return The power mob
     */
    public PowerMob createRandomMob(LivingEntity entity) {
        RandomMobConfig config = this.plugin.getConfigManager().getRandomMobConfig();
        if (config == null || !config.isEnabled()) {
            this.plugin.getLogger().warning("Random mob config is null or disabled");
            return null;
        }

        EntityType t = entity.getType();
        World.Environment env = entity.getWorld().getEnvironment();
        if (config.getSpawnCondition().isReplaceTypeOnly())
        {
            entity = replaceEntity(entity, entity.getType());
        } else {
        if (randomRestrictedMobs.contains(t)) {
            entity = replaceEntity(entity, entity.getType());
        } else {
            Set<EntityType> options = new HashSet<>(allMobs);

            if (env != World.Environment.NETHER) {
                options.removeAll(randomNetherLimitedMobs);
            }
            if (env != World.Environment.THE_END) {
                options.removeAll(randomEndLimitedMobs);
            }
            options.removeAll(randomRestrictedMobs);
            // Pick a random type from the remaining options
            if (options.isEmpty()) {
                this.plugin.getLogger().warning("No valid random mob types available; keeping original type " + t);
            } else {
                List<EntityType> choiceList = new ArrayList<>(options);
                EntityType chosenType = choiceList.get(this.random.nextInt(choiceList.size()));
                entity = replaceEntity(entity, chosenType);
            }
        }
        }
        // Create the power mob
        PowerMob powerMob = new PowerMob(this.plugin, entity, "random");

        // Generate random name
        String name = generateRandomName(config, entity.getType());

        // Generate random stats
        double healthMultiplier = config.getActualHealthMultiplier();
        double damageMultiplier = config.getActualDamageMultiplier();
        double speedMultiplier = config.getActualSpeedMultiplier();

        // Apply properties
        powerMob.applyProperties(
                config,
                name,
                healthMultiplier,
                damageMultiplier,
                speedMultiplier
        );

        // Apply random abilities
        List<String> possibleAbilities = new ArrayList<>(config.getPossibleAbilities());
        Collections.shuffle(possibleAbilities);

        int abilityCount = config.getActualAbilityCount();
        for (int i = 0; i < Math.min(abilityCount, possibleAbilities.size()); i++) {
            Ability ability = this.plugin.getAbilityManager().getAbility(possibleAbilities.get(i));
            if (ability != null) {
                powerMob.addAbility(ability);
            }
        }

        // Apply random equipment
        applyRandomEquipment(powerMob, config);

        if (config.isGlowing()) {
            applyGlowingEffect(entity, config.getActualGlowTime());
        }

        // Schedule despawn timer for random mobs too
        int despawnTime = config.getSpawnCondition().getActualDespawnTime();
        if (despawnTime > 0) {
            this.plugin.getSpawnTimerManager().scheduleDespawnTask(powerMob, despawnTime);
        }

        return powerMob;
    }

    private void applyGlowingEffect(LivingEntity entity, int durationSeconds) {
        PotionEffect glowingEffect = new PotionEffect(
                PotionEffectType.GLOWING,     // Effect type
                durationSeconds * 20,         // Duration in ticks (seconds * 20)
                0,                           // Amplifier (0 = level 1)
                false,                       // Ambient (particles reduced)
                false                        // Show particles
        );

        entity.addPotionEffect(glowingEffect);
        this.plugin.debug("Applied glowing potion effect for " + durationSeconds + " seconds to " + entity.getType(), "mob_spawning");
    }

    /**
     * Replaces an entity with a new entity of the specified type
     *
     * @param original The entity to replace
     * @param newType  The new entity type
     * @return The new entity, or the original if replacement fails
     */
    public LivingEntity replaceEntity(LivingEntity original, EntityType newType) {
//        if (original.getType() == newType) {
//            return original;
//        }
        // Get the current location of the original entity
        Location loc = original.getLocation();
        // Remove the original entity
        original.remove();
        // Return the new replaced entity
        return (LivingEntity) loc.getWorld().spawnEntity(loc, newType, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    /**
     * Attempts to find and choose a valid power mob configuration for spawn replacement
     *
     * @param entity   The entity to enhance
     * @param location The spawn location
     * @return The configuration ID, "random" for random mobs, or null if no configuration is valid
     */
    public String findValidPowerMobConfig(LivingEntity entity, Location location, CreatureSpawnEvent.SpawnReason reason) {

        if (PowerMob.isPowerMob(this.plugin, entity) || !(entity instanceof Mob)) {
            return null;
        }

        this.plugin.debug("Evaluating configurations for " + entity.getType(), "mob_spawning");
        List<String> validConfigs = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        // Single iteration - find valid predefined mobs
        for (PowerMobConfig config : this.plugin.getConfigManager().getPowerMobs().values()) {
            SpawnCondition spawnCondition = config.getSpawnCondition();
            if (spawnCondition.isReplaceTypeOnly()) {
                if (config.getEntityType() != entity.getType()) {
                    continue;
                }
            } else {
                try {
                    HostileEntityTypes.valueOf(entity.getType().name());
                } catch (IllegalArgumentException e) {
                    continue;
                }
            }

            if (spawnCondition.isValidSpawn(location, this.plugin)) {
                boolean bypassTimer = true;
                switch (reason) {
                    case NATURAL -> bypassTimer = false;
                    case COMMAND -> bypassTimer = true;
                    case CUSTOM -> bypassTimer = this.plugin.getConfigManager().isItemBypass();
                    case SPAWNER -> bypassTimer = this.plugin.getConfigManager().isSpawnerBypass();
                }
                // Check if this mob can spawn based on timer restrictions
                if (this.plugin.getSpawnTimerManager().canSpawn(config.getId(), location, currentTime, bypassTimer)) {
                    validConfigs.add(config.getId());
                } else {
                    this.plugin.debug("Power mob id: " + config.getId() + " skipped due to spawn timer cooldown", "mob_spawning");
                }
            }
        }

        // Check if random mobs can spawn (both conditions and timers)
        boolean randomValid = false;

        RandomMobConfig randomConfig = this.plugin.getConfigManager().getRandomMobConfig();
        if (randomConfig != null &&
                randomConfig.isEnabled() &&
                randomConfig.isAllowedType(entity.getType()) &&
                randomConfig.getSpawnCondition().isValidSpawn(location, this.plugin)) {

            // Check if random mob can spawn based on timer restrictions
            if (this.plugin.getSpawnTimerManager().canSpawn(
                    "random", location, currentTime, false)) {
                randomValid = true;
            } else {
                this.plugin.debug("Random mob skipped due to spawn timer cooldown", "mob_spawning");
            }

        } else {
            this.plugin.debug("Random mob skipped due to invalid spawn condition", "mob_spawning");
        }

        // If nothing is valid, return null
        if (validConfigs.isEmpty() && !randomValid) {
            return null;
        }

        // Random selection among valid configs and random mobs
        double totalWeight = validConfigs.size() + (randomValid ? 1 : 0);
        double value = Math.random() * totalWeight;

        if (value < validConfigs.size()) {
            return validConfigs.get((int) value);
        } else {
            return "random";
        }
    }

    /**
     * Generates a random name for a mob
     *
     * @param config     The random mob configuration
     * @param entityType The entity type
     * @return The generated name
     */
    private String generateRandomName(RandomMobConfig config, EntityType entityType) {
        // Get the entity type name
        String baseName = entityType.toString().toLowerCase();
        baseName = baseName.substring(0, 1).toUpperCase() + baseName.substring(1);

        // Get a random prefix if available
        String prefix = "";
        if (!config.getNamePrefixes().isEmpty()) {
            prefix = config.getNamePrefixes().get(this.random.nextInt(config.getNamePrefixes().size()));
        }

        // Get a random suffix if available
        String suffix = "";
        if (!config.getNameSuffixes().isEmpty()) {
            suffix = config.getNameSuffixes().get(this.random.nextInt(config.getNameSuffixes().size()));
        }

        // Generate random color
        ChatColor color = ChatColor.values()[this.random.nextInt(ChatColor.values().length)];
        while (color == ChatColor.BLACK || color == ChatColor.DARK_GRAY || color == ChatColor.GRAY ||
                color == ChatColor.WHITE || color == ChatColor.MAGIC || color == ChatColor.BOLD ||
                color == ChatColor.ITALIC || color == ChatColor.UNDERLINE || color == ChatColor.STRIKETHROUGH ||
                color == ChatColor.RESET) {
            color = ChatColor.values()[this.random.nextInt(ChatColor.values().length)];
        }

        return color + prefix + (prefix.isEmpty() ? "" : " ") + baseName + (suffix.isEmpty() ? "" : " " + suffix);
    }

    /**
     * Applies random equipment to a mob
     *
     * @param powerMob The power mob
     * @param config   The random mob configuration
     */
    private void applyRandomEquipment(PowerMob powerMob, RandomMobConfig config) {
        // Weapon
        List<EquipmentItemConfig> weapons = config.getPossibleEquipment().getOrDefault("possible-weapons", new ArrayList<>());
        if (!weapons.isEmpty() && Math.random() < config.getWeaponChance()) {
            EquipmentItemConfig weaponItem = weapons.get(this.random.nextInt(weapons.size()));
            this.plugin.getEquipmentManager().applyEquipment(powerMob, "possible-weapons", weaponItem);
        }

        // Offhand
        List<EquipmentItemConfig> offhand = config.getPossibleEquipment().getOrDefault("possible-offhands", new ArrayList<>());
        if (!offhand.isEmpty() && Math.random() < config.getWeaponChance()) {
            EquipmentItemConfig weaponItem = offhand.get(this.random.nextInt(offhand.size()));
            this.plugin.getEquipmentManager().applyEquipment(powerMob, "possible-offhands", weaponItem);
        }

        // Helmet
        List<EquipmentItemConfig> helmets = config.getPossibleEquipment().getOrDefault("possible-helmets", new ArrayList<>());
        if (!helmets.isEmpty() && Math.random() < config.getHelmetChance()) {
            EquipmentItemConfig helmetItem = helmets.get(this.random.nextInt(helmets.size()));
            this.plugin.getEquipmentManager().applyEquipment(powerMob, "possible-helmets", helmetItem);
        }

        // Chestplate
        List<EquipmentItemConfig> chestplates = config.getPossibleEquipment().getOrDefault("possible-chestplates", new ArrayList<>());
        if (!chestplates.isEmpty() && Math.random() < config.getChestplateChance()) {
            EquipmentItemConfig chestplateItem = chestplates.get(this.random.nextInt(chestplates.size()));
            this.plugin.getEquipmentManager().applyEquipment(powerMob, "possible-chestplates", chestplateItem);
        }

        // Leggings
        List<EquipmentItemConfig> leggings = config.getPossibleEquipment().getOrDefault("possible-leggings", new ArrayList<>());
        if (!leggings.isEmpty() && Math.random() < config.getLeggingsChance()) {
            EquipmentItemConfig leggingsItem = leggings.get(this.random.nextInt(leggings.size()));
            this.plugin.getEquipmentManager().applyEquipment(powerMob, "possible-leggings", leggingsItem);
        }

        // Boots
        List<EquipmentItemConfig> boots = config.getPossibleEquipment().getOrDefault("possible-boots", new ArrayList<>());
        if (!boots.isEmpty() && Math.random() < config.getBootsChance()) {
            EquipmentItemConfig bootsItem = boots.get(this.random.nextInt(boots.size()));
            this.plugin.getEquipmentManager().applyEquipment(powerMob, "possible-boots", bootsItem);
        }
    }

    /**
     * Generates a random value in a range
     *
     * @param min The minimum value
     * @param max The maximum value
     * @return A random value between min and max
     */
    private double randomRange(double min, double max) {
        return min + (max - min) * this.random.nextDouble();
    }

    /**
     * Generates a random integer in a range
     *
     * @param min The minimum value
     * @param max The maximum value
     * @return A random value between min and max
     */
    private int randomRange(int min, int max) {
        return min + this.random.nextInt(max - min + 1);
    }

}
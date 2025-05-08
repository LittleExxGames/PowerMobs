package com.custommobs.mobs;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.config.CustomMobConfig;
import com.custommobs.config.ICustomMobConfig;
import com.custommobs.config.RandomMobConfig;
import com.custommobs.config.SpawnCondition;
import com.custommobs.mobs.abilities.Ability;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.*;

/**
 * Factory for creating custom mobs
 */
@RequiredArgsConstructor
public class CustomMobFactory {

    private final CustomMobsPlugin plugin;
    private final Random random = new Random();

    /**
     * Creates a custom mob from a predefined configuration
     *
     * @param entity   The entity to enhance
     * @param configId The configuration ID
     * @return The custom mob, or null if the configuration is invalid
     */
    public CustomMob createCustomMob(LivingEntity entity, String configId) {
        CustomMobConfig config = this.plugin.getConfigManager().getCustomMob(configId);
        if (config == null) {
            this.plugin.getLogger().warning("Invalid custom mob configuration: " + configId);
            return null;
        }

        if (entity.getType() != config.getEntityType()) {
            this.plugin.getLogger().warning("Entity type mismatch for custom mob " + configId +
                    ": expected " + config.getEntityType() + ", got " + entity.getType());
            return null;
        }

        // Create the custom mob
        CustomMob customMob = new CustomMob(this.plugin, entity, configId);

        // Apply properties
        customMob.applyProperties(
                config,
                config.getName(),
                1.0, // Using fixed health from config
                config.getDamageMultiplier(),
                config.getSpeedMultiplier()
        );

        // Apply abilities
        for (String abilityId : config.getAbilities()) {
            Ability ability = this.plugin.getAbilityManager().getAbility(abilityId);
            if (ability != null) {
                customMob.addAbility(ability);
            } else {
                this.plugin.getLogger().warning("Invalid ability: " + abilityId);
            }
        }

        // Apply equipment
        for (String slot : config.getEquipment().keySet()) {
            String equipmentId = config.getEquipment().get(slot);
            this.plugin.getEquipmentManager().applyEquipment(customMob, slot, equipmentId);
        }

        return customMob;
    }

    /**
     * Creates a random custom mob
     *
     * @param entity The entity to enhance
     * @return The custom mob
     */
    public CustomMob createRandomMob(LivingEntity entity) {
        RandomMobConfig config = this.plugin.getConfigManager().getRandomMobConfig();
        if (config == null || !config.isEnabled()) {
            return null;
        }

        // Create the custom mob
        CustomMob customMob = new CustomMob(this.plugin, entity, "random");

        // Generate random name
        String name = generateRandomName(config, entity.getType());

        // Generate random stats
        double healthMultiplier = randomRange(config.getMinHealthMultiplier(), config.getMaxHealthMultiplier());
        double damageMultiplier = randomRange(config.getMinDamageMultiplier(), config.getMaxDamageMultiplier());
        double speedMultiplier = randomRange(config.getMinSpeedMultiplier(), config.getMaxSpeedMultiplier());

        // Apply properties
        SimpleCustomMobConfig dummyConfig = new SimpleCustomMobConfig.Builder()
                .entityType(entity.getType())
                .build();

        customMob.applyProperties(
                dummyConfig,
                name,
                healthMultiplier,
                damageMultiplier,
                speedMultiplier
        );

        // Apply random abilities
        List<String> possibleAbilities = new ArrayList<>(config.getPossibleAbilities());
        Collections.shuffle(possibleAbilities);

        int abilityCount = randomRange(config.getMinAbilities(), config.getMaxAbilities());
        for (int i = 0; i < Math.min(abilityCount, possibleAbilities.size()); i++) {
            Ability ability = this.plugin.getAbilityManager().getAbility(possibleAbilities.get(i));
            if (ability != null) {
                customMob.addAbility(ability);
            }
        }

        // Apply random equipment
        applyRandomEquipment(customMob, config);

        return customMob;
    }

    /**
     * Checks if a mob can be replaced with a custom mob
     *
     * @param entity   The entity to check
     * @param location The spawn location
     * @return True if the entity can be replaced
     */
    public boolean canReplaceWithCustomMob(LivingEntity entity, Location location) {
        // Don't replace already custom mobs
        if (CustomMob.isCustomMob(this.plugin, entity)) {
            return false;
        }

        // Only replace mobs, not players or other living entities
        if (!(entity instanceof Mob)) {
            return false;
        }

        // Check if any custom mob can spawn here
        for (CustomMobConfig config : this.plugin.getConfigManager().getCustomMobs().values()) {
            if (config.getEntityType() == entity.getType() &&
                    config.getSpawnCondition().isValidSpawn(location, this.plugin)) {
                return true;
            }
        }

        // Check if random mobs are enabled and can spawn here
        RandomMobConfig randomConfig = this.plugin.getConfigManager().getRandomMobConfig();
        return randomConfig != null &&
                randomConfig.isEnabled() &&
                randomConfig.isAllowedType(entity.getType()) &&
                randomConfig.getSpawnCondition().isValidSpawn(location, this.plugin);
    }

    /**
     * Chooses a custom mob configuration for a specific entity and location
     *
     * @param entity   The entity to enhance
     * @param location The spawn location
     * @return The configuration ID, "random" for random mobs, or null if no configuration is valid
     */
    public String chooseCustomMobConfig(LivingEntity entity, Location location) {
        List<String> validConfigs = new ArrayList<>();

        // Find valid predefined mobs
        for (String configId : this.plugin.getConfigManager().getCustomMobs().keySet()) {
            CustomMobConfig config = this.plugin.getConfigManager().getCustomMob(configId);
            if (config.getEntityType() == entity.getType() &&
                    config.getSpawnCondition().isValidSpawn(location, this.plugin)) {
                validConfigs.add(configId);
            }
        }

        // Check if random mobs can spawn
        RandomMobConfig randomConfig = this.plugin.getConfigManager().getRandomMobConfig();
        boolean randomValid = randomConfig != null &&
                randomConfig.isEnabled() &&
                randomConfig.isAllowedType(entity.getType()) &&
                randomConfig.getSpawnCondition().isValidSpawn(location, this.plugin);

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
     * @param customMob The custom mob
     * @param config    The random mob configuration
     */
    private void applyRandomEquipment(CustomMob customMob, RandomMobConfig config) {
        // Weapon
        if (!config.getPossibleWeapons().isEmpty() && Math.random() < config.getWeaponChance()) {
            String weaponId = config.getPossibleWeapons().get(this.random.nextInt(config.getPossibleWeapons().size()));
            this.plugin.getEquipmentManager().applyEquipment(customMob, "mainhand", weaponId);
        }

        // Helmet
        if (!config.getPossibleHelmets().isEmpty() && Math.random() < config.getHelmetChance()) {
            String helmetId = config.getPossibleHelmets().get(this.random.nextInt(config.getPossibleHelmets().size()));
            this.plugin.getEquipmentManager().applyEquipment(customMob, "helmet", helmetId);
        }

        // Chestplate
        if (!config.getPossibleChestplates().isEmpty() && Math.random() < config.getChestplateChance()) {
            String chestplateId = config.getPossibleChestplates().get(this.random.nextInt(config.getPossibleChestplates().size()));
            this.plugin.getEquipmentManager().applyEquipment(customMob, "chestplate", chestplateId);
        }

        // Leggings
        if (!config.getPossibleLeggings().isEmpty() && Math.random() < config.getLeggingsChance()) {
            String leggingsId = config.getPossibleLeggings().get(this.random.nextInt(config.getPossibleLeggings().size()));
            this.plugin.getEquipmentManager().applyEquipment(customMob, "leggings", leggingsId);
        }

        // Boots
        if (!config.getPossibleBoots().isEmpty() && Math.random() < config.getBootsChance()) {
            String bootsId = config.getPossibleBoots().get(this.random.nextInt(config.getPossibleBoots().size()));
            this.plugin.getEquipmentManager().applyEquipment(customMob, "boots", bootsId);
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

    /**
     * Builder for creating a dummy CustomMobConfig for random mobs
     */
    private static class SimpleCustomMobConfig implements ICustomMobConfig {
        private final EntityType entityType;

        private SimpleCustomMobConfig(Builder builder) {
            this.entityType = builder.entityType;
        }

        @Override
        public EntityType getEntityType() {
            return this.entityType;
        }

        @Override
        public String getName() {
            return "Random Mob";
        }

        @Override
        public double getHealth() {
            return -1; // Use multiplier
        }

        @Override
        public double getDamageMultiplier() {
            return 1.0; // Using custom value
        }

        @Override
        public double getSpeedMultiplier() {
            return 1.0; // Using custom value
        }

        @Override
        public SpawnCondition getSpawnCondition() {
            return new SpawnCondition();
        }

        @Override
        public Map<String, Double> getAttributes() {
            return new HashMap<>();  // Return empty map for simple config
        }

        public static class Builder {
            private EntityType entityType;

            public Builder entityType(EntityType entityType) {
                this.entityType = entityType;
                return this;
            }

            public SimpleCustomMobConfig build() {
                return new SimpleCustomMobConfig(this);
            }
        }
    }
}
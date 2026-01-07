package com.powermobs.config;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.equipment.CustomDropConfig;
import com.powermobs.utils.WeightedRandom;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * Configuration for a power mob
 */
@Getter
@Setter
public class PowerMobConfig implements IPowerMobConfig {

    private final String id;
    private final Map<String, Double> attributes;
    private final List<String> possibleAbilities;
    private final Map<String, List<EquipmentItemConfig>> possibleEquipment;
    private final List<CustomDropConfig> drops;
    private EntityType entityType;
    private String name;
    private int minHealth;
    private int maxHealth;
    private int healthWeight;
    private double minDamageMultiplier;
    private double maxDamageMultiplier;
    private int damageWeight;
    private double minSpeedMultiplier;
    private double maxSpeedMultiplier;
    private int speedWeight;
    private boolean glowing;
    private int minGlowTime;
    private int maxGlowTime;
    private int minDrops;
    private int maxDrops;
    private int dropWeight;
    private double experienceChance;
    private int experienceMinAmount;
    private int experienceMaxAmount;
    private int experienceWeight;
    private SpawnCondition spawnCondition;

    /**
     * Creates a new power mob configuration
     *
     * @param id      The mob ID
     * @param section The configuration section
     * @throws IllegalArgumentException If the configuration is invalid
     */
    public PowerMobConfig(String id, ConfigurationSection section) {
        this.id = id;

        // Basic properties
        String typeString = section.getString("type");
        if (typeString == null) {
            throw new IllegalArgumentException("Mob type is required");
        }

        try {
            this.entityType = EntityType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid entity type: " + typeString);
        }

        this.name = section.getString("name", "Custom " + entityType);

        // Health range
        String healthObj = section.getString("health");
        if (healthObj != null) {
            String[] parts = (healthObj).split("-");
            this.minHealth = Integer.parseInt(parts[0]);
            this.maxHealth = parts.length > 1 ? Integer.parseInt(parts[1]) : this.minHealth;
        } else {
            this.minHealth = -1;
            this.maxHealth = -1;
        }
        this.healthWeight = section.getInt("health-weight", 100);

        // Damage multiplier
        String damageObj = section.getString("damage-multiplier");
        if (damageObj != null) {
            String[] parts = (damageObj).split("-");
            this.minDamageMultiplier = Double.parseDouble(parts[0]);
            this.maxDamageMultiplier = parts.length > 1 ? Double.parseDouble(parts[1]) : this.minDamageMultiplier;
        } else {
            this.minDamageMultiplier = 1.0;
            this.maxDamageMultiplier = 1.0;
        }
        this.damageWeight = section.getInt("damage-weight", 100);

        // Speed multiplier
        String speedObj = section.getString("speed-multiplier");
        if (speedObj != null) {
            String[] parts = (speedObj).split("-");
            this.minSpeedMultiplier = Double.parseDouble(parts[0]);
            this.maxSpeedMultiplier = parts.length > 1 ? Double.parseDouble(parts[1]) : this.minSpeedMultiplier;
        } else {
            this.minSpeedMultiplier = 1.0;
            this.maxSpeedMultiplier = 1.0;
        }
        this.speedWeight = section.getInt("speed-weight", 100);

        // Attributes
        this.attributes = new LinkedHashMap<>();
        ConfigurationSection attrSection = section.getConfigurationSection("attributes");
        if (attrSection != null) {
            for (String key : attrSection.getKeys(false)) {
                this.attributes.put(key, attrSection.getDouble(key));
            }
        }

        // Abilities
        this.possibleAbilities = section.getStringList("abilities");

        // Equipment
        this.possibleEquipment = new LinkedHashMap<>();
        this.possibleEquipment.put("possible-weapons", new ArrayList<>());
        this.possibleEquipment.put("possible-offhands", new ArrayList<>());
        this.possibleEquipment.put("possible-helmets", new ArrayList<>());
        this.possibleEquipment.put("possible-chestplates", new ArrayList<>());
        this.possibleEquipment.put("possible-leggings", new ArrayList<>());
        this.possibleEquipment.put("possible-boots", new ArrayList<>());

        ConfigurationSection equipSection = section.getConfigurationSection("possible-equipment");
        if (equipSection != null) {
            for (String slot : equipSection.getKeys(false)) {
                switch (slot) {
                    case "possible-weapons":
                    case "possible-offhands":
                    case "possible-helmets":
                    case "possible-chestplates":
                    case "possible-leggings":
                    case "possible-boots":
                        List<Map<?, ?>> rawList = equipSection.getMapList(slot);
                        List<EquipmentItemConfig> slotItems = this.possibleEquipment.get(slot);
                        for (Map<?, ?> rawItem : rawList) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> itemMap = (Map<String, Object>) rawItem;
                            slotItems.add(new EquipmentItemConfig(itemMap));
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        // Glow settings
        this.glowing = section.getBoolean("glowing", false);

        String glowTimeObj = section.getString("glow-time");
        if (glowTimeObj != null) {
            String[] parts = (glowTimeObj).split("-");
            this.minGlowTime = Integer.parseInt(parts[0]);
            this.maxGlowTime = parts.length > 1 ? Integer.parseInt(parts[1]) : this.minGlowTime;
        } else {
            this.minGlowTime = 100;
            this.maxGlowTime = 100;
        }

        // Drop count range 
        String amountObj = section.getString("drop-count");
        if (amountObj != null) {
            String[] parts = (amountObj).split("-");
            this.minDrops = Integer.parseInt(parts[0]);
            this.maxDrops = parts.length > 1 ? Integer.parseInt(parts[1]) : this.minDrops;
        } else {
            this.minDrops = 0;
            this.maxDrops = 2;
        }

        // Drop weight
        this.dropWeight = section.getInt("drop-weight", 100);

        // Experience chance
        this.experienceChance = section.getDouble("exp-chance", 1.00);

        // Experience amount range 
        String expAmountObj = section.getString("exp-amount");
        if (expAmountObj != null) {
            String[] parts = (expAmountObj).split("-");
            this.experienceMinAmount = Integer.parseInt(parts[0]);
            this.experienceMaxAmount = parts.length > 1 ? Integer.parseInt(parts[1]) : this.experienceMinAmount;
        } else {
            this.experienceMinAmount = 50;
            this.experienceMaxAmount = 200;
        }

        // Experience weight
        this.experienceWeight = section.getInt("exp-weight", 100);

        // Drops
        this.drops = new ArrayList<>();
        List<Map<?, ?>> dropsList = section.getMapList("drops");
        for (Map<?, ?> dropMap : dropsList) {
            Map<String, Object> castedDropMap = (Map<String, Object>) dropMap;
            CustomDropConfig drop = new CustomDropConfig(castedDropMap);
            this.drops.add(drop);
        }

        // Spawn conditions
        ConfigurationSection conditionSection = section.getConfigurationSection("spawn-conditions");
        if (conditionSection != null) {
            this.spawnCondition = new SpawnCondition(conditionSection);
        } else {
            this.spawnCondition = new SpawnCondition(); // Default conditions
        }

    }

    public PowerMobConfig(PowerMobConfig powerMob) {
        this.id = powerMob.getId();
        this.entityType = powerMob.getEntityType();
        this.name = powerMob.getName();
        this.minHealth = powerMob.getMinHealth();
        this.maxHealth = powerMob.getMaxHealth();
        this.healthWeight = powerMob.getHealthWeight();
        this.minDamageMultiplier = powerMob.getMinDamageMultiplier();
        this.maxDamageMultiplier = powerMob.getMaxDamageMultiplier();
        this.damageWeight = powerMob.getDamageWeight();
        this.minSpeedMultiplier = powerMob.getMinSpeedMultiplier();
        this.maxSpeedMultiplier = powerMob.getMaxSpeedMultiplier();
        this.speedWeight = powerMob.getSpeedWeight();
        this.attributes = new LinkedHashMap<>(powerMob.getAttributes());
        this.possibleAbilities = new ArrayList<>(powerMob.getPossibleAbilities());
        this.possibleEquipment = new LinkedHashMap<>();
        for (Map.Entry<String, List<EquipmentItemConfig>> e : powerMob.getPossibleEquipment().entrySet()) {
            List<EquipmentItemConfig> copiedList = new ArrayList<>();
            for (EquipmentItemConfig cfg : e.getValue()) {
                copiedList.add(new EquipmentItemConfig(cfg));
            }
            this.possibleEquipment.put(e.getKey(), copiedList);
        }
        this.glowing = powerMob.isGlowing();
        this.minGlowTime = powerMob.getMinGlowTime();
        this.maxGlowTime = powerMob.getMaxGlowTime();
        this.minDrops = powerMob.getMinDrops();
        this.maxDrops = powerMob.getMaxDrops();
        this.dropWeight = powerMob.getDropWeight();
        this.experienceChance = powerMob.getExperienceChance();
        this.experienceMinAmount = powerMob.getExperienceMinAmount();
        this.experienceMaxAmount = powerMob.getExperienceMaxAmount();
        this.experienceWeight = powerMob.getExperienceWeight();
        this.drops = new ArrayList<>(powerMob.getDrops());
        this.spawnCondition = new SpawnCondition(powerMob.getSpawnCondition());
    }

    /**
     * Creates a new power mob configuration with default values
     *
     * @param id The mob ID
     */
    public PowerMobConfig(String id) {
        this.id = id;
        this.entityType = EntityType.ZOMBIE; // Default entity type
        this.name = "NEW POWER MOB";

        // Default health values
        this.minHealth = 20;
        this.maxHealth = 30;
        this.healthWeight = 100;

        // Default damage multiplier
        this.minDamageMultiplier = 1.0;
        this.maxDamageMultiplier = 1.0;
        this.damageWeight = 100;

        // Default speed multiplier
        this.minSpeedMultiplier = 1.0;
        this.maxSpeedMultiplier = 1.0;
        this.speedWeight = 100;

        // Initialize empty collections
        this.attributes = new LinkedHashMap<>();
        this.possibleAbilities = new ArrayList<>();
        this.possibleEquipment = new LinkedHashMap<>();
        this.possibleEquipment.put("possible-weapons", new ArrayList<>());
        this.possibleEquipment.put("possible-offhands", new ArrayList<>());
        this.possibleEquipment.put("possible-helmets", new ArrayList<>());
        this.possibleEquipment.put("possible-chestplates", new ArrayList<>());
        this.possibleEquipment.put("possible-leggings", new ArrayList<>());
        this.possibleEquipment.put("possible-boots", new ArrayList<>());

        // Default glow settings
        this.glowing = false;
        this.minGlowTime = 100;
        this.maxGlowTime = 100;

        // Default drop settings
        this.minDrops = 0;
        this.maxDrops = 2;
        this.dropWeight = 100;

        // Default experience settings
        this.experienceChance = 1.0;
        this.experienceMinAmount = 10;
        this.experienceMaxAmount = 15;
        this.experienceWeight = 100;

        // Initialize empty drops list
        this.drops = new ArrayList<>();

        // Default spawn conditions
        this.spawnCondition = new SpawnCondition(); // Uses default constructor
    }

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        // Store the entity type as a string
        map.put("type", this.entityType.name());

        // Basic properties
        map.put("name", this.name);
        String health;
        if (this.minHealth == this.maxHealth) {
            health = this.minHealth + "";
        } else {
            health = this.minHealth + "-" + this.maxHealth;
        }

        map.put("health", health);
        map.put("health-weight", this.healthWeight);

        String damageMultiplier;
        if (this.minDamageMultiplier == this.maxDamageMultiplier) {
            damageMultiplier = this.minDamageMultiplier + "";
        } else {
            damageMultiplier = this.minDamageMultiplier + "-" + this.maxDamageMultiplier;
        }
        map.put("damage-multiplier", damageMultiplier);
        map.put("damage-weight", this.damageWeight);

        String speedMultiplier;
        if (this.minSpeedMultiplier == this.maxSpeedMultiplier) {
            speedMultiplier = this.minSpeedMultiplier + "";
        } else {
            speedMultiplier = this.minSpeedMultiplier + "-" + this.maxSpeedMultiplier;
        }
        map.put("speed-multiplier", speedMultiplier);
        map.put("speed-weight", this.speedWeight);

        // Attributes
        map.put("attributes", this.attributes);

        // Abilities as a list of strings
        map.put("abilities", this.possibleAbilities);

        // Equipment
        Map<String, Object> equipOut = new LinkedHashMap<>();
        for (String slot : this.possibleEquipment.keySet()) {
            List<Map<String, Object>> slotList = new ArrayList<>();
            for (EquipmentItemConfig item : this.possibleEquipment.get(slot)) {
                slotList.add(item.toConfigMap());
            }
            equipOut.put(slot, slotList);
        }
        map.put("possible-equipment", equipOut);

        // Glow
        map.put("glowing", this.glowing);

        // Glow time
        String glowTime;
        if (this.minGlowTime == this.maxGlowTime) {
            glowTime = this.minGlowTime + "";
        } else {
            glowTime = this.minGlowTime + "-" + this.maxGlowTime;
        }
        map.put("glow-time", glowTime);

        // Drop count - store as "minDrops-maxDrops" if they differ, or as a single number if equal
        String dropCount;
        if (this.minDrops == this.maxDrops) {
            dropCount = this.minDrops + "";
        } else {
            dropCount = this.minDrops + "-" + this.maxDrops;
        }
        map.put("drop-count", dropCount);

        // Drop weight
        map.put("drop-weight", this.dropWeight);

        // Experience chance
        map.put("exp-chance", this.experienceChance);

        // Experience amount
        if (this.experienceMinAmount == this.experienceMaxAmount) {
            map.put("exp-amount", this.experienceMinAmount + "");
        } else {
            map.put("exp-amount", this.experienceMinAmount + "-" + this.experienceMaxAmount);
        }

        // Experience weight
        map.put("exp-weight", this.experienceWeight);

        // Drops: Convert each CustomDropConfig to its map representation.
        List<Map<String, Object>> dropsList = new ArrayList<>();
        List<String> safetyIdCheck = new ArrayList<>();
        for (CustomDropConfig drop : this.drops) {
            if (safetyIdCheck.contains(drop.getItem())) {
                PowerMobsPlugin.getInstance().getLogger().warning(ChatColor.RED + "Duplicate item ID found in mob " + this.id + " - Skipping item: " + drop.getItem());
                continue;
            }
            safetyIdCheck.add(drop.getItem());
            dropsList.add(drop.toConfigMap());
        }
        map.put("drops", dropsList);

        // Spawn conditions
        map.put("spawn-conditions", this.spawnCondition.toConfigMap());

        return map;
    }

    public Set<EntityType> getEntityTypes() {
        return Collections.singleton(this.entityType);
    }

    public int getActualHealth() {
        return WeightedRandom.getWeightedRandom(minHealth, maxHealth, healthWeight);
    }

    public double getActualHealthMultiplier() {
        return 1.0;
    }

    public double getActualDamageMultiplier() {
        return WeightedRandom.getWeightedRandom(minDamageMultiplier, maxDamageMultiplier, damageWeight);
    }

    public double getActualSpeedMultiplier() {
        return WeightedRandom.getWeightedRandom(minSpeedMultiplier, maxSpeedMultiplier, speedWeight);
    }

    public int getActualAbilityCount() {
        return possibleAbilities.size();
    }

    @Override
    public Map<String, List<EquipmentItemConfig>> getPossibleEquipment() {
        return possibleEquipment;
    }

    @Override
    public List<EquipmentItemConfig> getEquipment(String slot) {
        // slot is expected to be one of "possible-weapons", "possible-helmets", etc.
        return possibleEquipment.getOrDefault(slot, Collections.emptyList());
    }

    public List<String> getNamePrefixes() {
        return new ArrayList<>();
    }

    public List<String> getNameSuffixes() {
        return new ArrayList<>();
    }

    public int getActualGlowTime() {
        Random random = new Random();
        return random.nextInt(minGlowTime, maxGlowTime + 1);
    }

    /**
     * Gets the actual drop count using the weighted distribution
     *
     * @return The selected amount
     */
    public int getActualDropCount() {
        return WeightedRandom.getWeightedRandom(minDrops, maxDrops, dropWeight);
    }

    public int getActualExperienceAmount() {
        return WeightedRandom.getWeightedRandom(experienceMinAmount, experienceMaxAmount, experienceWeight);
    }

    /**
     * Gets a specific custom drop by item ID
     *
     * @param itemId The item ID to search for
     * @return The found item, or null if not found
     */
    public CustomDropConfig getDrop(String itemId) {
        for (CustomDropConfig drop : drops) {
            if (drop.getItem().equalsIgnoreCase(itemId)) {
                return drop;
            }
        }
        return null; // Not found
    }

}
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
 * Configuration for randomly generated mobs
 */
@Getter
@Setter
public class RandomMobConfig implements IPowerMobConfig {

    private final Set<EntityType> allowedTypes;
    private final Map<String, Map<String, Object>> possibleAbilities;
    private final Map<String, List<EquipmentItemConfig>> possibleEquipment;
    private final List<String> namePrefixes;
    private final List<String> nameSuffixes;
    private final List<CustomDropConfig> drops;
    private boolean enabled;
    private int minAbilities;
    private int maxAbilities;
    private int abilitiesWeight;
    private double minHealthMultiplier;
    private double maxHealthMultiplier;
    private int healthWeight;
    private double minDamageMultiplier;
    private double maxDamageMultiplier;
    private int damageWeight;
    private double minSpeedMultiplier;
    private double maxSpeedMultiplier;
    private int speedWeight;
    private double weaponChance;
    private double offhandChance;
    private double helmetChance;
    private double chestplateChance;
    private double leggingsChance;
    private double bootsChance;
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
     * Creates a default random mob configuration
     */
    public RandomMobConfig() {
        this.enabled = false;
        this.allowedTypes = EnumSet.noneOf(EntityType.class);
        this.minAbilities = 1;
        this.maxAbilities = 1;
        this.abilitiesWeight = 100;
        this.possibleAbilities = new LinkedHashMap<>();
        this.minHealthMultiplier = 1.5;
        this.maxHealthMultiplier = 2.0;
        this.healthWeight = 100;
        this.minDamageMultiplier = 1.2;
        this.maxDamageMultiplier = 1.5;
        this.damageWeight = 100;
        this.minSpeedMultiplier = 1.0;
        this.maxSpeedMultiplier = 1.2;
        this.speedWeight = 100;
        this.weaponChance = 0.5;
        this.offhandChance = 0.5;
        this.helmetChance = 0.5;
        this.chestplateChance = 0.5;
        this.leggingsChance = 0.5;
        this.bootsChance = 0.5;
        this.possibleEquipment = new LinkedHashMap<>();
        this.possibleEquipment.put("possible-weapons", new ArrayList<>());
        this.possibleEquipment.put("possible-offhands", new ArrayList<>());
        this.possibleEquipment.put("possible-helmets", new ArrayList<>());
        this.possibleEquipment.put("possible-chestplates", new ArrayList<>());
        this.possibleEquipment.put("possible-leggings", new ArrayList<>());
        this.possibleEquipment.put("possible-boots", new ArrayList<>());
        this.namePrefixes = new ArrayList<>();
        this.namePrefixes.add("Legendary");
        this.nameSuffixes = new ArrayList<>();
        this.nameSuffixes.add("Power Slayer");
        this.glowing = false;
        this.minGlowTime = 100;
        this.maxGlowTime = 100;
        this.minDrops = 0;
        this.maxDrops = 2;
        this.dropWeight = 100;
        this.experienceChance = 1.00;
        this.experienceMinAmount = 50;
        this.experienceMaxAmount = 200;
        this.experienceWeight = 100;
        this.drops = new ArrayList<>();
        this.spawnCondition = new SpawnCondition();

    }

    /**
     * Creates a non-linked random mob configuration copy
     *
     * @param copy The configuration to copy
     */
    public RandomMobConfig(RandomMobConfig copy) {
        this.enabled = copy.enabled;
        this.allowedTypes = EnumSet.copyOf(copy.allowedTypes);
        this.minAbilities = copy.minAbilities;
        this.maxAbilities = copy.maxAbilities;
        this.abilitiesWeight = copy.abilitiesWeight;
        this.possibleAbilities = new LinkedHashMap<>();
        if (copy.getPossibleAbilities() != null) {
            for (Map.Entry<String, Map<String, Object>> entry : copy.getPossibleAbilities().entrySet()) {
                Map<String, Object> inner = entry.getValue() != null
                        ? new LinkedHashMap<>(entry.getValue())
                        : Collections.emptyMap();
                this.possibleAbilities.put(entry.getKey(), inner);
            }
        }
        this.minHealthMultiplier = copy.minHealthMultiplier;
        this.maxHealthMultiplier = copy.maxHealthMultiplier;
        this.healthWeight = copy.healthWeight;
        this.minDamageMultiplier = copy.minDamageMultiplier;
        this.maxDamageMultiplier = copy.maxDamageMultiplier;
        this.damageWeight = copy.damageWeight;
        this.minSpeedMultiplier = copy.minSpeedMultiplier;
        this.maxSpeedMultiplier = copy.maxSpeedMultiplier;
        this.speedWeight = copy.speedWeight;
        this.weaponChance = copy.weaponChance;
        this.offhandChance = copy.offhandChance;
        this.helmetChance = copy.helmetChance;
        this.chestplateChance = copy.chestplateChance;
        this.leggingsChance = copy.leggingsChance;
        this.bootsChance = copy.bootsChance;
        this.possibleEquipment = new LinkedHashMap<>(copy.possibleEquipment);
        this.namePrefixes = new ArrayList<>(copy.namePrefixes);
        this.nameSuffixes = new ArrayList<>(copy.nameSuffixes);
        this.glowing = copy.glowing;
        this.minGlowTime = copy.minGlowTime;
        this.maxGlowTime = copy.maxGlowTime;
        this.minDrops = copy.minDrops;
        this.maxDrops = copy.maxDrops;
        this.dropWeight = copy.dropWeight;
        this.experienceChance = copy.experienceChance;
        this.experienceMinAmount = copy.experienceMinAmount;
        this.experienceMaxAmount = copy.experienceMaxAmount;
        this.experienceWeight = copy.experienceWeight;
        this.drops = new ArrayList<>(copy.drops);
        this.spawnCondition = new SpawnCondition(copy.spawnCondition);
    }

    /**
     * Creates a random mob configuration from a configuration section
     *
     * @param section The configuration section
     */
    public RandomMobConfig(ConfigurationSection section) {
        this.enabled = section.getBoolean("enabled", false);

        // Load allowed entity types
        this.allowedTypes = EnumSet.noneOf(EntityType.class);
        List<String> typeStrings = section.getStringList("allowed-types");
        for (String typeStr : typeStrings) {
            try {
                EntityType type = EntityType.valueOf(typeStr.toUpperCase());
                if (type.isAlive() && type.isSpawnable()) {
                    this.allowedTypes.add(type);
                }
            } catch (IllegalArgumentException e) {
                // Skip invalid entity type
            }
        }

        // Load ability configuration
        this.possibleAbilities = new LinkedHashMap<>();

        // For default and defined abilities:
        // - abilities: ["fire-aura", "teleport"]
        // - abilities: { fire-aura: { radius: 5 }, teleport: { chance: 0.2 } }
        ConfigurationSection abilitiesSection = section.getConfigurationSection("possible-abilities");
        if (abilitiesSection != null) {
            for (String abilityId : abilitiesSection.getKeys(false)) {
                ConfigurationSection abilitySettingsSection = abilitiesSection.getConfigurationSection(abilityId);
                if (abilitySettingsSection != null) {
                    this.possibleAbilities.put(abilityId, new LinkedHashMap<>(abilitySettingsSection.getValues(false)));
                } else {
                    this.possibleAbilities.put(abilityId, Collections.emptyMap());
                }
            }
        }

        String abilityRange = section.getString("ability-range");
        if (abilityRange != null) {
            String[] parts = abilityRange.split("-");
            this.minAbilities = Integer.parseInt(parts[0]);
            this.maxAbilities = parts.length > 1 ? Integer.parseInt(parts[1]) : this.minAbilities;
        } else {
            this.minAbilities = 1;
            this.maxAbilities = 1;
        }

        this.abilitiesWeight = section.getInt("abilities-weight", 100);

        // Load stats configuration
        String healthMultiplier = section.getString("health-multiplier", "1.5-3.0");
        String[] healthParts = healthMultiplier.split("-");
        this.minHealthMultiplier = Double.parseDouble(healthParts[0]);
        this.maxHealthMultiplier = healthParts.length > 1 ? Double.parseDouble(healthParts[1]) : this.minHealthMultiplier;
        this.healthWeight = section.getInt("health-weight", 100);

        String damageMultiplier = section.getString("damage-multiplier", "1.2-2.0");
        String[] damageParts = damageMultiplier.split("-");
        this.minDamageMultiplier = Double.parseDouble(damageParts[0]);
        this.maxDamageMultiplier = damageParts.length > 1 ? Double.parseDouble(damageParts[1]) : this.minDamageMultiplier;
        this.damageWeight = section.getInt("damage-weight", 100);

        String speedMultiplier = section.getString("speed-multiplier", "1.0-1.5");
        String[] speedParts = speedMultiplier.split("-");
        this.minSpeedMultiplier = Double.parseDouble(speedParts[0]);
        this.maxSpeedMultiplier = speedParts.length > 1 ? Double.parseDouble(speedParts[1]) : this.minSpeedMultiplier;
        this.speedWeight = section.getInt("speed-weight", 100);


        // Load equipment configuration
        ConfigurationSection equipmentSection = section.getConfigurationSection("equipment-chance");
        if (equipmentSection != null) {
            this.weaponChance = equipmentSection.getDouble("weapon-chance", 0.7);
            this.offhandChance = equipmentSection.getDouble("offhand-chance", 0.5);
            this.helmetChance = equipmentSection.getDouble("helmet-chance", 0.5);
            this.chestplateChance = equipmentSection.getDouble("chestplate-chance", 0.4);
            this.leggingsChance = equipmentSection.getDouble("leggings-chance", 0.4);
            this.bootsChance = equipmentSection.getDouble("boots-chance", 0.5);

        } else {
            this.weaponChance = 0.7;
            this.offhandChance = 0.5;
            this.helmetChance = 0.5;
            this.chestplateChance = 0.4;
            this.leggingsChance = 0.4;
            this.bootsChance = 0.5;

        }

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
                        // ignore unknown keys
                        break;
                }
            }
        }

        // Load name generation
        this.namePrefixes = section.getStringList("prefixes");
        this.nameSuffixes = section.getStringList("suffixes");

        // Get Glow settings
        this.glowing = section.getBoolean("glowing", false);
        String glowObj = section.getString("glow-time");
        if (glowObj != null) {
            String[] parts = (glowObj).split("-");
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

        // Experience amount range (can be a range like "0-2" or a single number)
        String expAmountObj = section.getString("exp-amount");
        if (expAmountObj != null) {
            String[] parts = (expAmountObj).split("-");
            this.experienceMinAmount = Integer.parseInt(parts[0]);
            this.experienceMaxAmount = parts.length > 1 ? Integer.parseInt(parts[1]) : this.experienceMinAmount;
        } else {
            this.experienceMinAmount = 10;
            this.experienceMaxAmount = 15;
        }

        // Experience weight
        this.experienceWeight = section.getInt("exp-weight", 100);

        // Drops
        this.drops = new ArrayList<>();
        List<Map<?, ?>> dropsList = section.getMapList("drops");
        for (Map<?, ?> dropMap : dropsList) {
            @SuppressWarnings("unchecked")
            Map<String, Object> castedDropMap = (Map<String, Object>) dropMap;
            CustomDropConfig drop = new CustomDropConfig(castedDropMap);
            this.drops.add(drop);
        }


        // Load spawn conditions
        ConfigurationSection conditionSection = section.getConfigurationSection("spawn-conditions");
        if (conditionSection != null) {
            this.spawnCondition = new SpawnCondition(conditionSection);
        } else {
            this.spawnCondition = new SpawnCondition();
        }

    }

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        // Enabled flag
        map.put("enabled", this.enabled);

        // Store the entity types
        List<String> allowedTypes = new ArrayList<>();
        for (EntityType type : this.allowedTypes) {
            allowedTypes.add(type.name());
        }
        map.put("allowed-types", allowedTypes);

        // Abilities
        if (this.possibleAbilities != null && !this.possibleAbilities.isEmpty()) {
            Map<String, Object> abilitiesMap = new LinkedHashMap<>();
            for (String abilityId : this.possibleAbilities.keySet()) {
                Map<String, Object> settings = this.possibleAbilities.get(abilityId);
                abilitiesMap.put(abilityId, settings != null ? settings : Collections.emptyMap());
            }
            map.put("possible-abilities", abilitiesMap);
        } else {
            map.put("possible-abilities", this.possibleAbilities);
        }
        String abilityRange;
        if (this.minAbilities == this.maxAbilities) {
            abilityRange = this.minAbilities + "";
        } else {
            abilityRange = this.minAbilities + "-" + this.maxAbilities;
        }
        map.put("ability-range", abilityRange);
        map.put("abilities-weight", this.abilitiesWeight);

        // Health multiplier range
        String healthMultiplier;
        if (this.minHealthMultiplier == this.maxHealthMultiplier) {
            healthMultiplier = this.minHealthMultiplier + "";
        } else {
            healthMultiplier = this.minHealthMultiplier + "-" + this.maxHealthMultiplier;
        }
        map.put("health-multiplier", healthMultiplier);
        map.put("health-weight", this.healthWeight);

        // Damage multiplier range
        String damageMultiplier;
        if (this.minDamageMultiplier == this.maxDamageMultiplier) {
            damageMultiplier = this.minDamageMultiplier + "";
        } else {
            damageMultiplier = this.minDamageMultiplier + "-" + this.maxDamageMultiplier;
        }
        map.put("damage-multiplier", damageMultiplier);
        map.put("damage-weight", this.damageWeight);

        // Speed multiplier range
        String speedMultiplier;
        if (this.minSpeedMultiplier == this.maxSpeedMultiplier) {
            speedMultiplier = this.minSpeedMultiplier + "";
        } else {
            speedMultiplier = this.minSpeedMultiplier + "-" + this.maxSpeedMultiplier;
        }
        map.put("speed-multiplier", speedMultiplier);
        map.put("speed-weight", this.speedWeight);


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

        // Equipment chance
        Map<String, Object> equipChanceMap = new LinkedHashMap<>();
        equipChanceMap.put("weapon-chance", this.weaponChance);
        equipChanceMap.put("offhand-chance", this.offhandChance);
        equipChanceMap.put("helmet-chance", this.helmetChance);
        equipChanceMap.put("chestplate-chance", this.chestplateChance);
        equipChanceMap.put("leggings-chance", this.leggingsChance);
        equipChanceMap.put("boots-chance", this.bootsChance);
        map.put("equipment-chance", equipChanceMap);

        // Prefixes
        map.put("prefixes", this.namePrefixes);

        // Suffixes
        map.put("suffixes", this.nameSuffixes);

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
                PowerMobsPlugin.getInstance().getLogger().warning(ChatColor.RED + "Duplicate item ID found in random mob - Skipping item: " + drop.getItem());
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

    public String getName() {
        return "random-mob";
    }

    public Set<EntityType> getEntityTypes() {
        return allowedTypes;
    }

    public Map<String, Double> getAttributes() {
        Map<String, Double> attributes = new HashMap<>();
        // TODO: Implement attributes
        return attributes;
    }

    public int getActualHealth() {
        return -1;
    }

    public double getActualHealthMultiplier() {
        return WeightedRandom.getWeightedRandom(minHealthMultiplier, maxHealthMultiplier, healthWeight);
    }

    public double getActualDamageMultiplier() {
        return WeightedRandom.getWeightedRandom(minDamageMultiplier, maxDamageMultiplier, damageWeight);
    }

    public double getActualSpeedMultiplier() {
        return WeightedRandom.getWeightedRandom(minSpeedMultiplier, maxSpeedMultiplier, speedWeight);
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
        return com.powermobs.utils.WeightedRandom.getWeightedRandom(minDrops, maxDrops, dropWeight);
    }

    public int getActualExperienceAmount() {
        return WeightedRandom.getWeightedRandom(experienceMinAmount, experienceMaxAmount, experienceWeight);
    }

    public int getActualAbilityCount() {
        return WeightedRandom.getWeightedRandom(minAbilities, maxAbilities, abilitiesWeight);
    }

    public CustomDropConfig getDrop(String item) {
        for (CustomDropConfig drop : drops) {
            if (drop.getItem().equalsIgnoreCase(item)) {
                return drop;
            }
        }
        return null; // Not found
    }

    @Override
    public List<EquipmentItemConfig> getEquipment(String slot) {
        return possibleEquipment.get(slot);
    }

    /**
     * Checks if a mob type is allowed for random enhancements
     *
     * @param type The entity type
     * @return True if the type is allowed
     */
    public boolean isAllowedType(EntityType type) {
        return this.allowedTypes.contains(type);
    }
}
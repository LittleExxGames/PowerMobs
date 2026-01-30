package com.powermobs.config;

import com.powermobs.mobs.equipment.CustomDropConfig;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IPowerMobConfig {
    Set<EntityType> getEntityTypes();

    String getName();

    int getActualHealth();

    double getActualHealthMultiplier();

    double getActualDamageMultiplier();

    double getActualSpeedMultiplier();

    int getActualAbilityCount();

    int getMinDrops();

    int getMaxDrops();

    int getActualDropCount();

    int getActualExperienceAmount();

    double getExperienceChance();

    List<CustomDropConfig> getDrops();

    CustomDropConfig getDrop(String item);

    Map<String, List<EquipmentItemConfig>> getPossibleEquipment();

    List<EquipmentItemConfig> getEquipment(String slot);

    List<String> getNamePrefixes();

    List<String> getNameSuffixes();

    Map<String, Map<String, Object>> getPossibleAbilities();

    SpawnCondition getSpawnCondition();

    void setSpawnCondition(SpawnCondition condition);

    Map<String, Double> getAttributes();

    Map<String, Object> toConfigMap();
}

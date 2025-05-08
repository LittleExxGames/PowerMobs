package com.custommobs.config;

import org.bukkit.entity.EntityType;
import java.util.Map;

public interface ICustomMobConfig {
    EntityType getEntityType();
    String getName();
    double getHealth();
    double getDamageMultiplier();
    double getSpeedMultiplier();
    SpawnCondition getSpawnCondition();
    Map<String, Double> getAttributes();
}

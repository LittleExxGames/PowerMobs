package com.powermobs.config;

import com.powermobs.mobs.equipment.items.Shape;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class ParticleEffectConfig {

    private String particleType;
    private Shape particleShape;
    private double particleRadius;
    private int particleCount;
    private int particleDurationSeconds;
    private int particleIntervalTicks;

    public ParticleEffectConfig() {
        this.particleType = "EXPLOSION";
        this.particleShape = parseEnum(Shape.class, "ORB");
        this.particleRadius = 1.0;
        this.particleCount = 20;
        this.particleDurationSeconds = 0;
        this.particleIntervalTicks = 5;
    }

    public ParticleEffectConfig(ConfigurationSection section) {
        this.particleType = section.getString("particle-type", "EXPLOSION");
        this.particleShape = parseEnum(Shape.class, section.getString("particle-shape", "ORB"));
        this.particleRadius = ((Number) section.getDouble("particle-radius", 1.0)).doubleValue();
        this.particleCount = ((Number) section.getInt("particle-count", 20)).intValue();
        this.particleDurationSeconds = ((Number) section.getInt("particle-duration", 0)).intValue();
        this.particleIntervalTicks = ((Number) section.getInt("particle-interval-ticks", 5)).intValue();
    }

    public ParticleEffectConfig(ParticleEffectConfig copy) {
        this.particleType = copy.particleType;
        this.particleShape = copy.particleShape;
        this.particleRadius = copy.particleRadius;
        this.particleCount = copy.particleCount;
        this.particleDurationSeconds = copy.particleDurationSeconds;
        this.particleIntervalTicks = copy.particleIntervalTicks;
    }

    public ParticleEffectConfig(String particleType, String particleShape, double particleRadius, int particleCount, int particleDurationSeconds, int particleIntervalTicks) {
        this.particleType = particleType;
        this.particleShape = parseEnum(Shape.class, particleShape);
        this.particleRadius = particleRadius;
        this.particleCount = particleCount;
        this.particleDurationSeconds = particleDurationSeconds;
        this.particleIntervalTicks = particleIntervalTicks;
    }

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("particle-type", particleType);
        map.put("particle-shape", particleShape.toString());
        map.put("particle-radius", particleRadius);
        map.put("particle-count", particleCount);
        map.put("particle-duration", particleDurationSeconds);
        map.put("particle-interval-ticks", particleIntervalTicks);
        return map;
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

}

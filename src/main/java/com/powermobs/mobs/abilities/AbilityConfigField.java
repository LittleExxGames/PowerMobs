package com.powermobs.mobs.abilities;

import java.util.List;

public record AbilityConfigField(
        String key,
        AbilityConfigValueType type,
        Object defaultValue,
        String description
) {
    public AbilityConfigField {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key");
    }

    public static AbilityConfigField string(String key, String def, String desc) {
        return new AbilityConfigField(key, AbilityConfigValueType.STRING, def, desc);
    }

    public static AbilityConfigField bool(String key, boolean def, String desc){
        return new AbilityConfigField(key, AbilityConfigValueType.BOOLEAN, def, desc);
    }

    public static AbilityConfigField integer(String key, int def, String desc) {
        return new AbilityConfigField(key, AbilityConfigValueType.INT, def, desc);
    }

    public static AbilityConfigField dbl(String key, double def, String desc) {
        return new AbilityConfigField(key, AbilityConfigValueType.DOUBLE, def, desc);
    }

    public static AbilityConfigField chance(String key, double def, String desc) {
        return new AbilityConfigField(key, AbilityConfigValueType.CHANCE, def, desc);
    }

    public static AbilityConfigField entityType(String key, String def, String desc) {
        return new AbilityConfigField(key, AbilityConfigValueType.ENTITY_TYPE, def, desc);
    }

    public static AbilityConfigField entityTypeList(String key, List<String> def, String desc) {
        return new AbilityConfigField(key, AbilityConfigValueType.ENTITY_TYPE_LIST, def, desc);
    }
}

package com.powermobs.config;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class SoundEffectConfig {

    private final String soundType;
    private final double soundVolume;
    private final double soundPitch;

    public SoundEffectConfig() {
        this.soundType = "ENTITY_EXPERIENCE_ORB_PICKUP";
        this.soundVolume = 1.0;
        this.soundPitch = 1.0;
    }

    public SoundEffectConfig(ConfigurationSection section) {
        this.soundType = section.getString("sound-type", "ENTITY_EXPERIENCE_ORB_PICKUP");
        this.soundVolume = Math.max(0.0, section.getDouble("sound-volume", 1.0));
        this.soundPitch = Math.max(0.0, section.getDouble("sound-pitch", 1.0));
    }

    public SoundEffectConfig(SoundEffectConfig copy) {
        this.soundType = copy.soundType;
        this.soundVolume = copy.soundVolume;
        this.soundPitch = copy.soundPitch;
    }

    public SoundEffectConfig(String soundType, double soundVolume, double soundPitch) {
        this.soundType = soundType;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
    }

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sound-type", soundType);
        map.put("sound-volume", soundVolume);
        map.put("sound-pitch", soundPitch);
        return map;
    }
}

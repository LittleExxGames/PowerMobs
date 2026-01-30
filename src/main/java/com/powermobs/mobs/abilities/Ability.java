package com.powermobs.mobs.abilities;

import com.powermobs.mobs.PowerMob;
import org.bukkit.Material;

import java.util.*;

/**
 * Interface for power mob abilities
 */
public interface Ability {

    /**
     * Gets the ID of the ability
     *
     * @return The ability ID
     */
    String getId();

    /**
     * Applies the ability to a power mob
     *
     * @param powerMob The power mob
     */
    void apply(PowerMob powerMob);

    /**
     * Removes the ability from a power mob
     *
     * @param powerMob The power mob
     */
    void remove(PowerMob powerMob);

    /**
     * The name to identify the ability by
     *
     * @return The title
     */
    String getTitle();

    /**
     * The brief description of the ability effects
     *
     * @return The brief description
     */
    String getDescription();

    /**
     * The Material icon to display
     *
     * @return The display material
     */
    Material getMaterial();

    /**
     * Returns the list of abilities with the configuration attached
     *
     * @return Customizable status rows
     */
    List<String> getStatus();

    /**
     * Schema for GUI + validation (key/type/default/description).
     * If empty, GUI can fall back to inferring from abilitiesconfig.yml.
     */
    Map<String, AbilityConfigField> getConfigSchema();

    /**
     * Returns per-mob status/config info for display.
     * Defaults to rendering the mob's resolved settings for this ability.
     *
     * @param powerMob The power mob
     * @return Status rows
     */
    default List<String> getStatus(PowerMob powerMob) {
        if (powerMob == null) {
            return getStatus();
        }

        Map<String, Object> settings = powerMob.getAbilitySettings(getId());
        if (settings == null || settings.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        settings.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> lines.add(entry.getKey() + ": " + Objects.toString(entry.getValue())));
        return lines;
    }
}
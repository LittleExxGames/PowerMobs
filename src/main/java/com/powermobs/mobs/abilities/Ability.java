package com.powermobs.mobs.abilities;

import com.powermobs.mobs.PowerMob;
import org.bukkit.Material;

import java.util.List;

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
     * Retries the abilities customizable info
     *
     * @return Customizable status rows
     */
    List<String> getStatus();
}
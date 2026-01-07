package com.powermobs.mobs.abilities;

import com.powermobs.PowerMobsPlugin;
import lombok.Getter;

/**
 * Base class for abilities
 */
public abstract class AbstractAbility implements Ability {

    @Getter
    protected final PowerMobsPlugin plugin;

    @Getter
    protected final String id;

    /**
     * Creates a new ability
     *
     * @param plugin The plugin instance
     * @param id     The ability ID
     */
    protected AbstractAbility(PowerMobsPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }
}
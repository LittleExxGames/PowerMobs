package com.custommobs.mobs.abilities;

import com.custommobs.CustomMobsPlugin;
import lombok.Getter;

/**
 * Base class for abilities
 */
public abstract class AbstractAbility implements Ability {

    @Getter
    protected final CustomMobsPlugin plugin;
    
    @Getter
    protected final String id;
    
    /**
     * Creates a new ability
     * 
     * @param plugin The plugin instance
     * @param id The ability ID
     */
    protected AbstractAbility(CustomMobsPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }
}
package com.custommobs.mobs.abilities;

import com.custommobs.mobs.CustomMob;

/**
 * Interface for custom mob abilities
 */
public interface Ability {

    /**
     * Gets the ID of the ability
     *
     * @return The ability ID
     */
    String getId();
    
    /**
     * Applies the ability to a custom mob
     *
     * @param customMob The custom mob
     */
    void apply(CustomMob customMob);
    
    /**
     * Removes the ability from a custom mob
     *
     * @param customMob The custom mob
     */
    void remove(CustomMob customMob);
}
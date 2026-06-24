package com.powermobs.mobs.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.abilities.impl.*;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all abilities for power mobs
 */
@RequiredArgsConstructor
public class AbilityManager {

    private final PowerMobsPlugin plugin;
    private final Map<String, Ability> abilities = new HashMap<>();

    /**
     * Loads all abilities from the configuration
     */
    public void loadAbilities() {
        unloadAbilities();
        // Register built-in abilities
        registerAbility(new FireAuraAbility(this.plugin));
        registerAbility(new LightningStrikeAbility(this.plugin));
        registerAbility(new SwitcherooAbility(this.plugin));
        registerAbility(new TeleportAbility(this.plugin));
        registerAbility(new LeapAbility(this.plugin));
        registerAbility(new SummonMinionsAbility(this.plugin));
        registerAbility(new WebShotAbility(this.plugin));
        registerAbility(new InvisibilityAbility(this.plugin));
        registerAbility(new RegenerationAbility(this.plugin));
        registerAbility(new LaunchpadAbility(this.plugin));
        registerAbility(new VexedAbility(this.plugin));

        this.plugin.debug("Loaded " + this.abilities.size() + " abilities", "save_and_load");
    }

    /**
     * Registers an ability
     *
     * @param ability The ability to register
     */
    public void registerAbility(Ability ability) {
        this.abilities.put(ability.getId(), ability);
    }

    /**
     * Unregisters an ability
     *
     * @param id The ability ID
     */
    public void unregisterAbility(String id) {
        Ability removed = this.abilities.remove(id);
        if (removed instanceof Listener listener) {
            HandlerList.unregisterAll(listener);
        }
    }

    /**
     * Unloads all abilities and unregisters any listeners they own.
     */
    public void unloadAbilities() {
        for (Ability ability : this.abilities.values()) {
            if (ability instanceof Listener listener) {
                HandlerList.unregisterAll(listener);
            }
        }
        this.abilities.clear();
    }

    /**
     * Gets an ability by ID
     *
     * @param id The ability ID
     * @return The ability, or null if not found
     */
    public Ability getAbility(String id) {
        return this.abilities.get(id);
    }

    /**
     * Gets all registered abilities
     *
     * @return An unmodifiable map of all abilities
     */
    public Map<String, Ability> getAbilities() {
        return Collections.unmodifiableMap(this.abilities);
    }
}
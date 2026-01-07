package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ability that summons lightning when the mob attacks
 */
public class LightningStrikeAbility extends AbstractAbility implements Listener {

    private final String title = "Lightning Strike";
    private final String description = "Strikes lightning upon the attacker.";
    private final Material material = Material.LIGHTNING_ROD;
    private final double chance;
    private final double damageMultiplier;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * Creates a new lightning strike ability
     *
     * @param plugin The plugin instance
     */
    public LightningStrikeAbility(PowerMobsPlugin plugin) {
        super(plugin, "lightning-strike");

        ConfigurationSection config = plugin.getConfigManager().getAbilitiesConfigManager().getConfig().getConfigurationSection("abilities.lightning-strike");

        if (config != null) {
            this.chance = config.getDouble("chance", 0.25);
            this.damageMultiplier = config.getDouble("damage-multiplier", 1.0);
        } else {
            this.chance = 0.25;
            this.damageMultiplier = 1.0;
        }

        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {
        // This ability is event-based, so we don't need to do anything here
    }

    @Override
    public void remove(PowerMob powerMob) {
        this.cooldowns.remove(powerMob.getEntityUuid());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity damager)) {
            return;
        }

        PowerMob powerMob = PowerMob.getFromEntity(this.plugin, damager);

        if (powerMob == null) {
            return;
        }

        // Check if the mob has this ability
        boolean hasAbility = powerMob.getAbilities().stream()
                .anyMatch(ability -> ability.getId().equals(this.id));

        if (!hasAbility) {
            return;
        }

        // Check cooldown
        if (this.cooldowns.containsKey(powerMob.getEntityUuid())) {
            long lastUse = this.cooldowns.get(powerMob.getEntityUuid());
            if (System.currentTimeMillis() - lastUse < 5000) {
                return;
            }
        }

        // Random chance to trigger
        if (Math.random() > this.chance) {
            return;
        }

        // Trigger lightning
        if (event.getEntity().getLocation().getWorld() != null) {
            event.getEntity().getLocation().getWorld().strikeLightning(event.getEntity().getLocation());

            // Increase damage
            event.setDamage(event.getDamage() * this.damageMultiplier);

            // Set cooldown
            this.cooldowns.put(powerMob.getEntityUuid(), System.currentTimeMillis());
        }
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public Material getMaterial() {
        return this.material;
    }

    @Override
    public List<String> getStatus() {
        return List.of();
    }
}
package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LaunchpadAbility extends AbstractAbility implements Listener {
    private final String title = "Launchpad";
    private final String description = "The an attack has a chance to launch the target into the air.";
    private final Material material = Material.PISTON;
    private final double chance;
    private final int cooldown;
    private final int power;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public LaunchpadAbility(PowerMobsPlugin plugin) {
        super(plugin, "launchpad");

        ConfigurationSection config = plugin.getConfigManager().getAbilitiesConfigManager().getConfig().getConfigurationSection("abilities.launchpad");

        if (config != null) {
            this.chance = config.getDouble("chance", 0.08);
            this.cooldown = config.getInt("cooldown", 4);
            this.power = config.getInt("power", 2);
        } else {
            this.chance = 0.2;
            this.cooldown = 4;
            this.power = 2;
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
    public void apply(PowerMob powerMob) {
        // Register this ability as an event listener when applied
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @Override
    public void remove(PowerMob powerMob) {
        UUID mobUuid = powerMob.getEntityUuid();
        this.cooldowns.remove(mobUuid);
    }

    @Override
    public List<String> getStatus() {
        return List.of();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Check if the damager is a LivingEntity (potential power mob)
        if (!(event.getDamager() instanceof LivingEntity damager)) {
            return;
        }

        // Check if the target is a LivingEntity that can be launched
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        // Check if the damager is a power mob
        PowerMob powerMob = PowerMob.getFromEntity(this.plugin, damager);
        if (powerMob == null || !powerMob.isValid()) {
            return;
        }

        // Check if the mob has this ability
        boolean hasAbility = powerMob.getAbilities().stream()
                .anyMatch(ability -> ability.getId().equals(this.id));
        if (!hasAbility) {
            return;
        }

        // Check cooldown
        UUID mobUuid = powerMob.getEntityUuid();
        if (this.cooldowns.containsKey(mobUuid)) {
            long lastUse = this.cooldowns.get(mobUuid);
            if (System.currentTimeMillis() - lastUse < this.cooldown * 1000L) {
                return;
            }
        }

        if (Math.random() > this.chance) {
            return;
        }

        if (event.getDamager() instanceof Player player) {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
                return;
            }
        }

        // Launch the entity into the air
        target.setVelocity(new Vector(0, this.power, 0));
        this.cooldowns.put(mobUuid, System.currentTimeMillis());

    }

}

package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

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
    private final NamespacedKey abilityLightningKey = new NamespacedKey(plugin, "pm_lightning_strike");
    private final NamespacedKey abilityLightningDamageKey = new NamespacedKey(plugin, "pm_lightning_strike_damage");
    private final double defaultChance = 0.25;
    private final double defaultDamageMultiplier = 1.0;
    private final boolean defaultRealLighting = false;
    private final int defaultCooldown = 7;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * Creates a new lightning strike ability
     *
     * @param plugin The plugin instance
     */
    public LightningStrikeAbility(PowerMobsPlugin plugin) {
        super(plugin, "lightning-strike");

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
        if (event.getDamager() instanceof LightningStrike bolt) {
            if (bolt.getPersistentDataContainer().has(abilityLightningKey, PersistentDataType.STRING)) {
                Double scaled = bolt.getPersistentDataContainer().get(abilityLightningDamageKey, PersistentDataType.DOUBLE);
                if (scaled != null) {
                    event.setDamage(scaled);
                }
            }
            return;
        }
        lightingAttack(event);

    }

    public void lightingAttack(EntityDamageByEntityEvent event){
        LivingEntity source = null;
        if (event.getDamager() instanceof LivingEntity livingDamager) {
            source = livingDamager;
        } else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity livingShooter) {
                source = livingShooter;
            }
        }

        if (source == null) {
            return;
        }

        PowerMob powerMob = PowerMob.getFromEntity(this.plugin, source);

        if (powerMob == null) {
            return;
        }

        // Check if the mob has this ability
        boolean hasAbility = powerMob.getAbilities().stream()
                .anyMatch(ability -> ability.getId().equals(this.id));

        if (!hasAbility) {
            return;
        }

        final double chance = powerMob.getAbilityDouble(this.id, "chance", this.defaultChance);
        final double damageMultiplier = powerMob.getAbilityDouble(this.id, "damage-multiplier", this.defaultDamageMultiplier);
        final boolean realLighting = powerMob.getAbilityBoolean(this.id, "real-lighting", this.defaultRealLighting);
        final int cooldownSeconds = powerMob.getAbilityInt(this.id, "cooldown", this.defaultCooldown);

        // Check cooldown
        if (this.cooldowns.containsKey(powerMob.getEntityUuid())) {
            long lastUse = this.cooldowns.get(powerMob.getEntityUuid());
            if (System.currentTimeMillis() - lastUse < cooldownSeconds * 1000L) {
                return;
            }
        }

        // Random chance to trigger
        if (Math.random() > chance) {
            return;
        }

        World world = event.getEntity().getWorld();
        if (world == null) return;

        // Trigger lightning
        LightningStrike bolt = realLighting
                ? world.strikeLightning(event.getEntity().getLocation())
                : world.strikeLightningEffect(event.getEntity().getLocation());

        double scaledLightningDamage = 5 * damageMultiplier;
        bolt.getPersistentDataContainer().set(abilityLightningKey, PersistentDataType.STRING, this.id);
        bolt.getPersistentDataContainer().set(abilityLightningDamageKey, PersistentDataType.DOUBLE, scaledLightningDamage);

        // Increase damage
        if (!realLighting && event.getEntity() instanceof LivingEntity target) {
            target.damage(scaledLightningDamage, bolt);
        }

        // Set cooldown
        this.cooldowns.put(powerMob.getEntityUuid(), System.currentTimeMillis());
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

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        return Map.of(
                "chance", AbilityConfigField.chance("chance", this.defaultChance, "Chance to strike lightning upon hitting"),
                "damage-multiplier", AbilityConfigField.dbl("damage-multiplier", this.defaultDamageMultiplier, "Multiplier for the damage dealt"),
                "real-lighting", AbilityConfigField.bool("real-lighting", this.defaultRealLighting, "If true, uses real lightning (may cause world effects); if false, uses a visual-based strike"),
                "cooldown", AbilityConfigField.integer("cooldown", this.defaultCooldown, "Cooldown until lighting can trigger again")
        );
    }
}
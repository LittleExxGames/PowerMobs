package com.custommobs.mobs;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.config.CustomMobConfig;
import com.custommobs.config.ICustomMobConfig;
import com.custommobs.mobs.abilities.Ability;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a custom mob in the game
 */
@Getter
public class CustomMob {

    private static final String CUSTOM_MOB_KEY = "custommob.id";
    private static final String CUSTOM_MOB_UUID = "custommob.uuid";

    private final CustomMobsPlugin plugin;
    private final LivingEntity entity;
    private final String id;
    private final UUID customMobUuid;
    private final boolean isRandom;
    private final List<Ability> abilities = new ArrayList<>();
    
    /**
     * Creates a new custom mob
     * 
     * @param plugin The plugin instance
     * @param entity The entity to enhance
     * @param id The custom mob ID (config key or "random" for random mobs)
     */
    public CustomMob(CustomMobsPlugin plugin, LivingEntity entity, String id) {
        this.plugin = plugin;
        this.entity = entity;
        this.id = id;
        this.customMobUuid = UUID.randomUUID();
        this.isRandom = "random".equals(id);
        
        // Tag the entity with metadata
        this.entity.setMetadata(CUSTOM_MOB_KEY, new FixedMetadataValue(plugin, id));
        
        // Store the UUID in the persistent data container
        PersistentDataContainer pdc = this.entity.getPersistentDataContainer();
        pdc.set(plugin.getCustomMobManager().getCustomMobKey(), PersistentDataType.STRING, this.customMobUuid.toString());
    }
    
    /**
     * Applies the custom mob properties to the entity
     * 
     * @param config The mob configuration
     * @param displayName The display name
     * @param healthMultiplier The health multiplier
     * @param damageMultiplier The damage multiplier
     * @param speedMultiplier The speed multiplier
     */
    public void applyProperties(ICustomMobConfig config, String displayName, double healthMultiplier,
                                double damageMultiplier, double speedMultiplier) {
        // Set display name
        if (this.plugin.getConfigManager().isShowNames()) {
            this.entity.setCustomName(ChatColor.translateAlternateColorCodes('&', displayName));
            this.entity.setCustomNameVisible(true);
        }
        
        // Apply health
        double baseMaxHealth = Objects.requireNonNull(
            this.entity.getAttribute(Attribute.MAX_HEALTH)
        ).getBaseValue();
        
        double newMaxHealth = config.getHealth() > 0 
            ? config.getHealth() 
            : baseMaxHealth * healthMultiplier;
            
        Objects.requireNonNull(
            this.entity.getAttribute(Attribute.MAX_HEALTH)
        ).setBaseValue(newMaxHealth);
        
        this.entity.setHealth(newMaxHealth);
        
        // Apply damage
        if (this.entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            double baseDamage = Objects.requireNonNull(
                this.entity.getAttribute(Attribute.ATTACK_DAMAGE)
            ).getBaseValue();
            
            Objects.requireNonNull(
                this.entity.getAttribute(Attribute.ATTACK_DAMAGE)
            ).setBaseValue(baseDamage * damageMultiplier);
        }
        
        // Apply speed
        if (this.entity.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            double baseSpeed = Objects.requireNonNull(
                this.entity.getAttribute(Attribute.MOVEMENT_SPEED)
            ).getBaseValue();
            
            Objects.requireNonNull(
                this.entity.getAttribute(Attribute.MOVEMENT_SPEED)
            ).setBaseValue(baseSpeed * speedMultiplier);
        }
        
        // Additional attributes
        if (config.getAttributes() != null) {
            for (String attrName : config.getAttributes().keySet()) {
                try {
                    Attribute attribute = Attribute.valueOf(attrName.toUpperCase().replace("-", "_"));
                    if (this.entity.getAttribute(attribute) != null) {
                        Objects.requireNonNull(
                            this.entity.getAttribute(attribute)
                        ).setBaseValue(config.getAttributes().get(attrName));
                    }
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Invalid attribute: " + attrName);
                }
            }
        }
    }
    
    /**
     * Adds an ability to the mob
     * 
     * @param ability The ability to add
     */
    public void addAbility(Ability ability) {
        this.abilities.add(ability);
        ability.apply(this);
    }
    
    /**
     * Removes all abilities from the mob
     */
    public void removeAbilities() {
        for (Ability ability : this.abilities) {
            ability.remove(this);
        }
        this.abilities.clear();
    }
    
    /**
     * Checks if the entity is valid (not dead or removed)
     * 
     * @return True if the entity is valid
     */
    public boolean isValid() {
        return this.entity != null && !this.entity.isDead() && this.entity.isValid();
    }
    
    /**
     * Removes this custom mob
     */
    public void remove() {
        removeAbilities();
        if (isValid()) {
            this.entity.removeMetadata(CUSTOM_MOB_KEY, this.plugin);
        }
    }
    
    /**
     * Gets the UUID of the entity
     * 
     * @return The entity UUID
     */
    public UUID getEntityUuid() {
        return this.entity.getUniqueId();
    }
    
    /**
     * Checks if a player can see this mob
     * 
     * @param player The player to check
     * @return True if the player can see the mob
     */
    public boolean isVisibleTo(Player player) {
        return player.getWorld().equals(this.entity.getWorld()) && 
               player.getLocation().distance(this.entity.getLocation()) <= 100;
    }
    
    /**
     * Gets the location description of the mob
     * 
     * @return The location description
     */
    public String getLocationDescription() {
        return String.format("%s at [%d, %d, %d]", 
            this.entity.getWorld().getName(),
            this.entity.getLocation().getBlockX(),
            this.entity.getLocation().getBlockY(),
            this.entity.getLocation().getBlockZ()
        );
    }
    
    /**
     * Gets a custom mob from an entity, if it exists
     * 
     * @param plugin The plugin instance
     * @param entity The entity to check
     * @return The custom mob, or null if the entity is not a custom mob
     */
    public static CustomMob getFromEntity(CustomMobsPlugin plugin, LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(plugin.getCustomMobManager().getCustomMobKey(), PersistentDataType.STRING)) {
            String uuidString = pdc.get(plugin.getCustomMobManager().getCustomMobKey(), PersistentDataType.STRING);
            if (uuidString != null) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    return plugin.getCustomMobManager().getCustomMob(uuid);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Checks if an entity is a custom mob
     * 
     * @param plugin The plugin instance
     * @param entity The entity to check
     * @return True if the entity is a custom mob
     */
    public static boolean isCustomMob(CustomMobsPlugin plugin, LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        
        return entity.getPersistentDataContainer().has(
            plugin.getCustomMobManager().getCustomMobKey(), 
            PersistentDataType.STRING
        );
    }
}
package com.custommobs.events;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.mobs.CustomMob;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * Listens for mob spawns and replaces them with custom mobs
 */
@RequiredArgsConstructor
public class MobSpawnListener implements Listener {

    private final CustomMobsPlugin plugin;
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntitySpawn(EntitySpawnEvent event) {
        // Skip non-living entities
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        // Skip if the entity is already a custom mob
        if (CustomMob.isCustomMob(this.plugin, entity)) {
            return;
        }
        
        // Skip if the spawn is from another plugin or command
        if (entity.hasMetadata("NPC") || entity.hasMetadata("shopkeeper")) {
            return;
        }
        
        // Check if we should replace this mob
        Location location = entity.getLocation();
        
        if (!this.plugin.getCustomMobManager().getFactory().canReplaceWithCustomMob(entity, location)) {
            this.plugin.debug("Cannot replace with custom mob - conditions not met for " + entity.getType());
            return;
        }
        
        // Random chance to become a custom mob
        if (Math.random() > this.plugin.getConfigManager().getSpawnChance()) {
            return;
        }
        
        // Choose a configuration
        String configId = this.plugin.getCustomMobManager().getFactory().chooseCustomMobConfig(entity, location);
        if (configId == null) {
            this.plugin.debug("No suitable mob config found");
            return;
        }
        
        this.plugin.debug("Replacing " + entity.getType() + " with custom mob " + configId);
        
        // Create the custom mob
        CustomMob customMob = this.plugin.getCustomMobManager().createAndRegisterCustomMob(entity, configId);
        if (customMob == null) {
            return;
        }
        
        // Show spawn effect
        if (this.plugin.getConfigManager().isSpawnEffect()) {
            location.getWorld().spawnParticle(
                Particle.EXPLOSION_EMITTER,
                location.clone().add(0, 1, 0), 
                1, 
                0, 
                0, 
                0, 
                0
            );
        }
        
        // Announce spawn
        if (this.plugin.getConfigManager().isSpawnAnnouncements()) {
            for (Player player : location.getWorld().getPlayers()) {
                if (player.hasPermission("custommobs.announce") && 
                    player.getLocation().distance(location) <= 50) {
                    player.sendMessage(ChatColor.RED + "[CustomMobs] " + ChatColor.GOLD + 
                        "A " + entity.getCustomName() + ChatColor.GOLD + " has spawned nearby!");
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // This handler exists to catch spawns that might have been missed by the EntitySpawnEvent
        if (event.isCancelled()) {
            return;
        }
        
        LivingEntity entity = event.getEntity();

        // Skip if the entity is already a custom mob
        if (CustomMob.isCustomMob(this.plugin, entity)) {
            this.plugin.debug("Skipping - already a custom mob");
            return;
        }
        
        // Skip natural spawns (handled by the EntitySpawnEvent)
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }
        
        // Skip if the spawn is from another plugin or command
        if (entity.hasMetadata("NPC") || entity.hasMetadata("shopkeeper")) {
            return;
        }
        
        // Only handle specific spawn types
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER && 
            event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.EGG) {
            return;
        }
        
        // Check if we should replace this mob
        Location location = entity.getLocation();
        
        if (!this.plugin.getCustomMobManager().getFactory().canReplaceWithCustomMob(entity, location)) {
            return;
        }
        
        // 50% reduced chance for spawner mobs
        double chance = this.plugin.getConfigManager().getSpawnChance();
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            chance *= 0.5;
        }
        
        if (Math.random() > chance) {
            return;
        }
        
        // Choose a configuration
        String configId = this.plugin.getCustomMobManager().getFactory().chooseCustomMobConfig(entity, location);
        if (configId == null) {
            return;
        }
        
        this.plugin.debug("Replacing " + entity.getType() + " with custom mob " + configId + 
            " (from " + event.getSpawnReason().name() + ")");
        
        // Create the custom mob
        this.plugin.getCustomMobManager().createAndRegisterCustomMob(entity, configId);
    }


}
package com.powermobs.mobs.abilities.impl;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Ability that allows mobs to shoot webs that slow players
 */
public class WebShotAbility extends AbstractAbility {

    private final String title = "Web Shot";
    private final String description = "Shoots a cobweb to trap the player.";
    private final Material material = Material.COBWEB;
    private final int range;
    private final int slownessLevel;
    private final int duration;
    private final int cooldown;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<Location, Long> webLocations = new HashMap<>();

    /**
     * Creates a new web shot ability
     *
     * @param plugin The plugin instance
     */
    public WebShotAbility(PowerMobsPlugin plugin) {
        super(plugin, "web-shot");

        ConfigurationSection config = plugin.getConfigManager().getAbilitiesConfigManager().getConfig().getConfigurationSection("abilities.web-shot");

        if (config != null) {
            this.range = config.getInt("range", 15);
            this.slownessLevel = config.getInt("slowness-level", 1);
            this.duration = config.getInt("duration", 5);
            this.cooldown = config.getInt("cooldown", 10);
        } else {
            this.range = 15;
            this.slownessLevel = 1;
            this.duration = 5;
            this.cooldown = 10;
        }

        // Schedule a task to clean up web blocks
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Iterator<Map.Entry<Location, Long>> it = this.webLocations.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Location, Long> entry = it.next();
                if (now - entry.getValue() > 10000) { // 10 seconds
                    Location loc = entry.getKey();
                    if (loc.getBlock().getType() == Material.COBWEB) {
                        loc.getBlock().setType(Material.AIR);
                    }
                    it.remove();
                }
            }
        }, 20, 20);
    }

    @Override
    public void apply(PowerMob powerMob) {
        UUID entityUuid = powerMob.getEntityUuid();

        // Cancel existing task if it exists
        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
        }

        // Create a new task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!powerMob.isValid()) {
                remove(powerMob);
                return;
            }

            // Check cooldown
            if (this.cooldowns.containsKey(entityUuid)) {
                long lastUse = this.cooldowns.get(entityUuid);
                if (System.currentTimeMillis() - lastUse < this.cooldown * 1000L) {
                    return;
                }
            }

            // Get the entity
            LivingEntity entity = powerMob.getEntity();

            // Look for nearby players
            List<Player> nearbyPlayers = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR || player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                    continue;
                }
                if (player.getWorld().equals(entity.getWorld()) &&
                        player.getLocation().distance(entity.getLocation()) <= this.range) {
                    nearbyPlayers.add(player);
                }
            }

            if (nearbyPlayers.isEmpty()) {
                return;
            }

            // Select a random player
            Player target = nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()));

            // Check line of sight
            if (!entity.hasLineOfSight(target)) {
                return;
            }

            // Calculate direction to the player
            Vector direction = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();

            // Shoot a web
            Location startLoc = entity.getLocation().add(0, 1, 0);
            Location current = startLoc.clone();

            for (int i = 0; i < this.range; i++) {
                current = current.add(direction);

                // Show particle trail
                if (current.getWorld() != null) {
                    current.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, current, 5, 0.2, 0.2, 0.2, 0, Material.COBWEB.createBlockData());
                }

                // Check for collision
                Block block = current.getBlock();
                if (block.getType().isSolid()) {
                    break;
                }

                // Check for player hit
                for (Player player : nearbyPlayers) {
                    if (player.getLocation().distance(current) < 1.5) {
                        // Hit a player
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS,
                                this.duration * 20,
                                this.slownessLevel,
                                false,
                                true,
                                true
                        ));

                        // Place a web at the player's feet if the block is air
                        Block playerBlock = player.getLocation().getBlock();
                        if (playerBlock.getType() == Material.AIR || playerBlock.getType() == Material.CAVE_AIR) {
                            playerBlock.setType(Material.COBWEB);
                            this.webLocations.put(playerBlock.getLocation(), System.currentTimeMillis());
                        }

                        // Set cooldown
                        this.cooldowns.put(entityUuid, System.currentTimeMillis());
                        return;
                    }
                }
            }

            // Set cooldown even if we didn't hit
            this.cooldowns.put(entityUuid, System.currentTimeMillis());

        }, 20, 20);

        this.tasks.put(entityUuid, task);
    }

    @Override
    public void remove(PowerMob powerMob) {
        UUID entityUuid = powerMob.getEntityUuid();

        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
            this.tasks.remove(entityUuid);
        }

        this.cooldowns.remove(entityUuid);
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
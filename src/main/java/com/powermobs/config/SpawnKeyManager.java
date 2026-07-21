package com.powermobs.config;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.SpawnContext;
import com.powermobs.mobs.equipment.items.Shape;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnKeyManager {

    @Getter
    private final FileConfigManager spawnKeyConfig;
    private final NamespacedKey spawnKeyKey;
    private final PowerMobsPlugin plugin;
    private final Map<String, SpawnKeyConfig> keyConfigs;
    private final Random random = new Random();
    private final Set<String> pendingSpawnKeys = ConcurrentHashMap.newKeySet();



    public SpawnKeyManager(PowerMobsPlugin plugin) {
        this.spawnKeyConfig = new FileConfigManager(plugin, "spawnkeyconfig.yml");
        this.spawnKeyKey = new NamespacedKey(plugin, "spawn-key");
        this.plugin = plugin;
        this.keyConfigs = new ConcurrentHashMap<>();
    }

    /**
     * Loads spawn key configurations from the config file
     */
    public void loadKeys() {
        FileConfiguration config = spawnKeyConfig.getConfig();
        ConfigurationSection keysSection = config.getConfigurationSection("spawn-keys");

        keyConfigs.clear();
        if (keysSection == null || keysSection.getKeys(false).isEmpty()) {
            plugin.debug("No spawn keys configured, creating default configuration", "spawn_keys");
            createDefaultConfig();
            return;
        }

        for (String keyId : keysSection.getKeys(false)) {
            ConfigurationSection keySection = keysSection.getConfigurationSection(keyId);
            if (keySection == null) continue;

            try {
                SpawnKeyManager.SpawnKeyConfig keyConfig = loadKeyConfig(keyId, keySection);
                keyConfigs.put(keyId, keyConfig);
                plugin.debug("Loaded spawn key: " + keyId, "spawn_keys");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load spawn key " + keyId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Reloads spawn key configurations from the config file
     */
    public void reloadKeys() {
        spawnKeyConfig.reloadConfig();
        loadKeys();
        this.plugin.debug("Spawn keys reloaded", "spawn_keys");
    }

    /**
     * Loads a single key configuration from a config section
     */
    private SpawnKeyManager.SpawnKeyConfig loadKeyConfig(String id, ConfigurationSection section) {
        boolean enabled = section.getBoolean("enabled", true);
        Material material = Material.valueOf(section.getString("material", "TRIPWIRE_HOOK").toUpperCase());
        String name = section.getString("name", "&cPower Mob Spawn Key");
        List<String> lore = section.getStringList("lore");
        List<String> spawnIds = section.getStringList("spawn-ids");
        if (spawnIds.isEmpty()) {spawnIds.add("random");}
        boolean requireContext = section.getBoolean("require-context", true);
        String contextFailureText = section.getString("contextFailureText", "&cThe key doesnt seem to react.");
        boolean involveTimers = section.getBoolean("involve-timers", false);
        String timerRestrictionText = section.getString("timerRestrictionText", "&cThe key doesnt respond. Maybe try at a different time?.");
        List<String> announcementText = section.getStringList("announcement-text");
        double announcementTextInterval = section.getDouble("announcement-text-interval", 2.5);
        SoundEffectConfig soundEffectConfig;
        ConfigurationSection soundSection = section.getConfigurationSection("sound-effect");
        soundEffectConfig = (soundSection == null) ? new SoundEffectConfig() : new SoundEffectConfig(soundSection);

        ParticleEffectConfig particleEffectConfig;
        ConfigurationSection particleSection = section.getConfigurationSection("particle-effect");
        particleEffectConfig = (particleSection == null) ? new ParticleEffectConfig() : new ParticleEffectConfig(particleSection);

        return new SpawnKeyManager.SpawnKeyConfig(enabled, id, material, name, lore, spawnIds, requireContext, contextFailureText, involveTimers, timerRestrictionText, announcementText, announcementTextInterval, soundEffectConfig, particleEffectConfig);
    }

    public void createDefaultConfig() {
        FileConfiguration config = spawnKeyConfig.getConfig();
        config.set("spawn-keys", null);

        config.set("spawn-keys.random.enabled", true);
        config.set("spawn-keys.random.material", "TRIPWIRE_HOOK");
        config.set("spawn-keys.random.name", "&cPower Mob Spawn Key");
        config.set("spawn-keys.random.lore", Arrays.asList(
                "&7Use this to spawn a Power Mob",
                "&7Default key"
        ));
        config.set("spawn-keys.random.spawn-ids", Collections.singletonList("random"));
        config.set("spawn-keys.random.require-context", true);
        config.set("spawn-keys.random.contextFailureText", "&cThe key doesnt seem to react.");
        config.set("spawn-keys.random.involve-timers", false);
        config.set("spawn-keys.random.timerRestrictionText", "&cThe key doesnt respond. Maybe try at a different time?");
        config.set("spawn-keys.random.announcement-text", Collections.emptyList());
        config.set("spawn-keys.random.announcement-text-interval", 2.5);
        config.set("spawn-keys.random.sound-effect.sound-type", "ENTITY_EXPERIENCE_ORB_PICKUP");
        config.set("spawn-keys.random.sound-effect.sound-volume", 1);
        config.set("spawn-keys.random.sound-effect.sound-pitch", 1);
        config.set("spawn-keys.random.particle-effect.particle-type", "EXPLOSION");
        config.set("spawn-keys.random.particle-effect.particle-shape", Shape.ORB.toString());
        config.set("spawn-keys.random.particle-effect.particle-radius", 2);
        config.set("spawn-keys.random.particle-effect.particle-count", 25);
        config.set("spawn-keys.random.particle-effect.particle-duration", 1);
        config.set("spawn-keys.random.particle-effect.particle-interval-ticks", 5);

        spawnKeyConfig.saveConfig();
        loadKeys();
    }

    /**
     * Gets all configured spawn key IDs
     */
    public Set<String> getSpawnKeyIds() {
        return new HashSet<>(keyConfigs.keySet());
    }

    /**
     * Gets a spawn key configuration by ID
     */
    public SpawnKeyConfig getSpawnKeyConfig(String keyId) {
        return keyConfigs.get(keyId);
    }

    public ItemStack createSpawnKeyItem(String keyId) {
        SpawnKeyConfig config = keyConfigs.get(keyId);
        if (config == null) {
            return null;
        }

        ItemStack item = new ItemStack(config.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Set display name
        if (!config.name().isEmpty()) {
            meta.setDisplayName(config.name().replace("&", "§"));
        }

        // Set lore
        if (!config.lore().isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : config.lore()) {
                coloredLore.add(line.replace("&", "§"));
            }
            meta.setLore(coloredLore);
        }

        // Set custom data for identification
        if (config.id() != null) {
            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            dataContainer.set(spawnKeyKey, PersistentDataType.STRING, keyId);

            // Set custom-id for integration with existing custom item system
            NamespacedKey customIdKey = new NamespacedKey(plugin, "custom-id");
            dataContainer.set(customIdKey, PersistentDataType.STRING, config.id());
        }

        item.setItemMeta(meta);
        return item;
    }

    public boolean triggerSpawnKey(String keyId, PlayerInteractEvent event) {
        SpawnKeyConfig config = keyConfigs.get(keyId);
        if (config == null || !config.enabled()) {
            return false;
        }
        Player player = event.getPlayer();
        String lockKey = player.getUniqueId() + ":" + keyId;
        if (!pendingSpawnKeys.add(lockKey)) {
            return false;
        }
        try {
            Location loc;
            if (event.getClickedBlock() != null) {
                loc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0.0, 0.5);
                //loc = event.getClickedBlock().getLocation();
            } else {
                loc = event.getPlayer().getLocation().clone().add(0.0, 0.0, 0.0);
            }
            String chosenMobId = pickUsableMobId(config, loc);
            if (chosenMobId == null) {
                String message = config.contextFailureText();
                message = ChatColor.translateAlternateColorCodes('&', message);
                player.sendMessage(message);
                pendingSpawnKeys.remove(lockKey);
                return false;
            }

            IPowerMobConfig mobConfig = (chosenMobId.equalsIgnoreCase("random")) ? plugin.getConfigManager().getRandomMobConfig() : plugin.getConfigManager().getPowerMob(chosenMobId);

            boolean involveTimers = config.involveTimers();
            long now = System.currentTimeMillis();

            if (involveTimers && plugin.getConfigManager().isSpawnTimersEnabled() && !plugin.getSpawnTimerManager().canSpawn(chosenMobId, loc, now, false)) {
                String message = config.timerRestrictionText();
                message = ChatColor.translateAlternateColorCodes('&', message);
                player.sendMessage(message);
                pendingSpawnKeys.remove(lockKey);
                return false;
            }
            List<EntityType> choiceList = new ArrayList<>(mobConfig.getEntityTypes());
            EntityType chosenType = choiceList.get(random.nextInt(choiceList.size()));

            // Announce, sound, spawn
            startAnnouncementSequence(loc, config, mobConfig.getName(), () -> {
                var powerMob = plugin.getPowerMobManager().spawnAndRegisterPowerMob(
                        loc,
                        chosenType,
                        chosenMobId,
                        org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM,
                        true
                );

                if (powerMob == null) {
                    pendingSpawnKeys.remove(lockKey);
                    return;
                }

                playSpawnSound(loc, config);

                if (config.involveTimers() && plugin.getConfigManager().isSpawnTimersEnabled()) {
                    long delay = mobConfig.getSpawnCondition().getActualSpawnDelay() * 1000L;
                    var context = SpawnContext.builder()
                            .mobConfigId(chosenMobId)
                            .location(loc)
                            .source(SpawnContext.SpawnSource.ITEM)
                            .forceBypass(false)
                            .delay(delay)
                            .spawnTime(now)
                            .spawnReason(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM)
                            .itemUsed(keyId)
                            .build();

                    plugin.getSpawnTimerManager().recordSpawn(chosenMobId, context, false);
                }
                pendingSpawnKeys.remove(lockKey);
            });
        } catch (Exception e) {
            pendingSpawnKeys.remove(lockKey);
            throw e;
        }
        return true;
    }

    private String pickUsableMobId(SpawnKeyConfig config, Location loc) {
        if (config.spawnIds().isEmpty()) {
            return null;
        }
        List<String> validIds = new ArrayList<>(config.spawnIds());
        Collections.shuffle(validIds);
        for (String mobId : validIds) {
            IPowerMobConfig mob = (mobId.equalsIgnoreCase("random")) ? this.plugin.getConfigManager().getRandomMobConfig() : this.plugin.getConfigManager().getPowerMob(mobId);
            if (mob != null){
                if (config.requireContext) {
                     if (mob.getSpawnCondition().isValidSpawn(loc, this.plugin)){
                         return mobId;
                     } else {
                         continue;
                     }
                }
                return mobId;
            }
        }
        this.plugin.debug("No valid Power Mob available for spawning through the spawn key.", "mob_spawning");
        return null;
    }

    private void startAnnouncementSequence(Location location, SpawnKeyConfig config, String mobName, Runnable onComplete) {
        List<String> lines = new ArrayList<>(config.announcementText());

        long periodTicks = Math.max(1L, (long) (config.announcementTextInterval() * 20.0));

        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                if (index >= lines.size()) {
                    cancel();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    return;
                }

                playParticleEffect(config, location);

                String line = lines.get(index++);
                if (!line.isBlank()) {
                    for (Player nearby : location.getWorld().getPlayers()) {
                        if (nearby.getLocation().distanceSquared(location) <= 30 * 30) {
                            String message = line.replace("%mob%", mobName);
                            message = ChatColor.translateAlternateColorCodes('&', message);
                            nearby.sendMessage(message);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, periodTicks);
    }

    private void playParticleEffect(SpawnKeyConfig key, Location location) {
        if (location == null || location.getWorld() == null) return;

        final Particle particle;
        if (key.particleEffectConfig() == null) {
            plugin.getLogger().warning("[SpawnKeys] Invalid particle config for " +
                    key.id());
            return;
        }
        ParticleEffectConfig config = key.particleEffectConfig();
        try {

            particle = Particle.valueOf(config.getParticleType().toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().warning("[SpawnKeys] Invalid particle type '" + config.getParticleType() + "' for " +
                    key.id());
            return;
        }

        final int perTickCount = config.getParticleCount();
        if (perTickCount <= 0) return;

        final double radius = config.getParticleRadius();
        final Shape shape = config.getParticleShape();

        Runnable burst = () -> spawnParticleBurst(location, particle, perTickCount, radius, shape);

        int durationSeconds = config.getParticleDurationSeconds();
        if (durationSeconds <= 0) {
            burst.run();
            return;
        }

        int interval = config.getParticleIntervalTicks();
        int runs = Math.max(1, (durationSeconds * 20) / interval);

        final int[] remaining = { runs };
        final BukkitTask[] task = { null };

        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (remaining[0]-- <= 0) {
                if (task[0] != null) task[0].cancel();
                return;
            }
            burst.run();
        }, 0L, interval);
    }

    private void spawnParticleBurst(Location base, Particle particle, int count, double radius, Shape shape) {
        if (base.getWorld() == null) return;

        Location origin = base.clone();

        for (int i = 0; i < count; i++) {
            Vector offset = switch (shape) {
                case SQUARE -> randomPointInSquare(radius);
                case CIRCLE ->  randomPointInCircle(radius);
                case CUBE -> randomPointInCube(radius);
                case ORB -> randomPointInSphere(radius);
            };

            Location at = origin.clone().add(offset);
            origin.getWorld().spawnParticle(particle, at, 1, 0, 0, 0, 0);
        }
    }

    private Vector randomPointInSquare(double radius){
        if (radius <= 0) return new Vector(0, 0, 0);
        return new Vector(random.nextDouble() * radius * 2 - radius, 0.0, random.nextDouble() * radius * 2 - radius);
    }

    private Vector randomPointInCube(double radius){
        if (radius <= 0) return new Vector(0, 0, 0);
        return new Vector(random.nextDouble() * radius * 2 - radius, random.nextDouble() * radius * 2 - radius, random.nextDouble() * radius * 2 - radius);
    }

    private Vector randomPointInCircle(double radius) {
        if (radius <= 0) return new Vector(0, 0, 0);

        double t = random.nextDouble() * (Math.PI * 2);
        double r = Math.sqrt(random.nextDouble()) * radius;
        double x = Math.cos(t) * r;
        double z = Math.sin(t) * r;
        return new Vector(x, 0.0, z);
    }

    private Vector randomPointInSphere(double radius) {
        if (radius <= 0) return new Vector(0, 0, 0);

        while (true) {
            double x = (random.nextDouble() * 2 - 1) * radius;
            double y = (random.nextDouble() * 2 - 1) * radius;
            double z = (random.nextDouble() * 2 - 1) * radius;
            if ((x * x + y * y + z * z) <= (radius * radius)) {
                return new Vector(x, y, z);
            }
        }
    }

    private void playSpawnSound(Location location, SpawnKeyConfig config) {
        if (config.soundEffectConfig() == null || location.getWorld() == null) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(config.soundEffectConfig().getSoundType().toUpperCase());
            location.getWorld().playSound(location, sound,
                    (float) config.soundEffectConfig().getSoundVolume(),
                    (float) config.soundEffectConfig().getSoundPitch());
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("[Spawn Keys] Invalid spawn key sound: " + config.soundEffectConfig().getSoundType());
        }
    }



    public String getSpawnKeyId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(spawnKeyKey, PersistentDataType.STRING);
    }

    /**
     * Configuration class for spawn keys
     */
    public record SpawnKeyConfig(boolean enabled, String id, Material material, String name, List<String> lore, List<String> spawnIds,
                                 boolean requireContext, String contextFailureText, boolean involveTimers, String timerRestrictionText, List<String> announcementText, double announcementTextInterval,
                                 SoundEffectConfig soundEffectConfig, ParticleEffectConfig particleEffectConfig) {
        public SpawnKeyConfig(boolean enabled, String id, Material material, String name, List<String> lore, List<String> spawnIds,
                              boolean requireContext, String contextFailureText, boolean involveTimers, String timerRestrictionText, List<String> announcementText, double announcementTextInterval,
                              SoundEffectConfig soundEffectConfig, ParticleEffectConfig particleEffectConfig) {
            this.enabled = enabled;
            this.id = id;
            this.material = material;
            this.name = name;
            this.lore = lore != null ? lore : new ArrayList<>();
            this.spawnIds = new ArrayList<>(spawnIds);
            this.requireContext = requireContext;
            this.contextFailureText = contextFailureText;
            this.involveTimers = involveTimers;
            this.timerRestrictionText = timerRestrictionText;
            this.announcementText = new ArrayList<>(announcementText);
            this.announcementTextInterval = announcementTextInterval;
            this.soundEffectConfig = new SoundEffectConfig(soundEffectConfig);
            this.particleEffectConfig = new ParticleEffectConfig(particleEffectConfig);
        }
    }
}

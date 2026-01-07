package com.powermobs.config;

import com.powermobs.PowerMobsPlugin;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages spawn blocking mechanics using vanilla blocks or custom items
 */
public class SpawnBlockerManager {

    private static final long CACHE_DURATION = 30000; // 30 seconds
    private static final long BLOCKER_EVALUATE_TIMER = 20L * 60; // 1 minute
    private final PowerMobsPlugin plugin;
    @Getter
    private final FileConfigManager spawnBlockersConfig;
    private final FileConfigManager spawnBlockersDataConfig;
    private final NamespacedKey chunkBlockersKey;
    // Cache for active spawn blockers - maps world name to chunk coordinates to blocker configs
    private final Map<String, Map<ChunkCoord, Set<SpawnBlockerData>>> activeBlockers;
    // Configuration cache
    private final Map<String, SpawnBlockerConfig> blockerConfigs;
    // Optimization: cache for recent chunk checks
    private final Map<String, Boolean> chunkCheckCache;
    private long lastCacheClear = 0;
    private int cleanupTaskId = -1;

    public SpawnBlockerManager(PowerMobsPlugin plugin) {
        this.plugin = plugin;
        this.spawnBlockersConfig = new FileConfigManager(plugin, "spawnblockerconfig.yml");
        this.spawnBlockersDataConfig = new FileConfigManager(plugin, "spawnblockersdata.yml");
        this.activeBlockers = new ConcurrentHashMap<>();
        this.blockerConfigs = new ConcurrentHashMap<>();
        this.chunkCheckCache = new ConcurrentHashMap<>();
        this.chunkBlockersKey = new NamespacedKey(plugin, "spawn-blockers");
    }

    /**
     * Starts the cleanup task
     */
    public void startCleanupTask() {
        if (cleanupTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(cleanupTaskId);
            cleanupTaskId = -1;
        }

        cleanupTaskId = plugin.getServer().getScheduler().runTaskTimer(plugin,
                this::cleanupInvalidBlockers,
                BLOCKER_EVALUATE_TIMER,
                BLOCKER_EVALUATE_TIMER).getTaskId();
    }

    /**
     * Periodically removes blockers whose source block is gone or changed.
     * Only validates locations in loaded chunks to avoid chunk loads.
     */
    private void cleanupInvalidBlockers() {
        plugin.debug("Starting cleanup task...", "spawn_blockers");

        int checked = 0;
        int removed = 0;

        Set<String> toRemoveKeys = new HashSet<>();
        List<Location> toRemoveLocations = new ArrayList<>();

        Iterator<Map.Entry<String, Map<ChunkCoord, Set<SpawnBlockerData>>>> worldIterator =
                activeBlockers.entrySet().iterator();

        while (worldIterator.hasNext()) {
            Map.Entry<String, Map<ChunkCoord, Set<SpawnBlockerData>>> worldEntry = worldIterator.next();
            String worldName = worldEntry.getKey();
            World world = plugin.getServer().getWorld(worldName);

            if (world == null) {
                worldIterator.remove();
                plugin.debug("Removed entries for unloaded world: " + worldName, "spawn_blockers");
                continue;
            }

            Map<ChunkCoord, Set<SpawnBlockerData>> worldBlockers = worldEntry.getValue();
            if (worldBlockers == null || worldBlockers.isEmpty()) {
                worldIterator.remove();
                continue;
            }

            Iterator<Map.Entry<ChunkCoord, Set<SpawnBlockerData>>> chunkIterator =
                    worldBlockers.entrySet().iterator();

            while (chunkIterator.hasNext()) {
                Map.Entry<ChunkCoord, Set<SpawnBlockerData>> chunkEntry = chunkIterator.next();
                Set<SpawnBlockerData> chunkSet = chunkEntry.getValue();

                if (chunkSet == null || chunkSet.isEmpty()) {
                    chunkIterator.remove();
                    continue;
                }

                Iterator<SpawnBlockerData> dataIterator = chunkSet.iterator();
                while (dataIterator.hasNext()) {
                    SpawnBlockerData data = dataIterator.next();
                    Location loc = data.location();
                    if (loc == null || loc.getWorld() == null) {
                        dataIterator.remove();
                        removed++;
                        continue;
                    }

                    // Skip if origin chunk not loaded
                    int cx = loc.getBlockX() >> 4;
                    int cz = loc.getBlockZ() >> 4;
                    if (!world.isChunkLoaded(cx, cz)) continue;

                    checked++;

                    Material current = world.getBlockAt(loc).getType();
                    Material expected = data.config().material();
                    if (current != expected) {
                        dataIterator.remove();
                        String key = worldName + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
                        if (toRemoveKeys.add(key)) {
                            toRemoveLocations.add(loc);
                            removed++;
                        }
                    }
                }
                if (chunkSet.isEmpty()) {
                    chunkIterator.remove();
                }
            }
            if (worldBlockers.isEmpty()) {
                worldIterator.remove();
            }
        }

        for (Location loc : toRemoveLocations) {
            removeSpawnBlocker(loc);
        }
        plugin.debug("Cleanup completed in " +
                "Chunks checked: " + checked + ", Blockers removed: " + removed, "spawn_blockers");
        chunkCheckCache.clear();
    }

    /**
     * Loads spawn blocker configurations from the config file
     */
    public void loadBlockers() {
        blockerConfigs.clear();
        activeBlockers.clear();
        chunkCheckCache.clear();

        FileConfiguration config = spawnBlockersConfig.getConfig();
        ConfigurationSection blockersSection = config.getConfigurationSection("spawn-blockers");

        if (blockersSection == null) {
            plugin.debug("No spawn blockers configured, creating default configuration", "spawn_blockers");
            createDefaultConfig();
            return;
        }

        for (String blockerId : blockersSection.getKeys(false)) {
            ConfigurationSection blockerSection = blockersSection.getConfigurationSection(blockerId);
            if (blockerSection == null) continue;

            try {
                SpawnBlockerConfig blockerConfig = loadBlockerConfig(blockerId, blockerSection);
                blockerConfigs.put(blockerId, blockerConfig);
                plugin.debug("Loaded spawn blocker: " + blockerId + " affecting " + blockerConfig.chunkRange() + " chunk(s)", "spawn_blockers");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load spawn blocker " + blockerId + ": " + e.getMessage());
            }
        }

        // Load persisted blockers placed previously
        plugin.getServer().getScheduler().runTask(plugin, this::loadPersistedBlockers);
    }

    /**
     * Load active blockers from spawnblockersdata.yml.
     * Validates against current block type; invalid ones are skipped and later caught by cleanup if needed.
     */
    public void loadPersistedBlockers() {
        int loaded = 0;

        FileConfiguration data = spawnBlockersDataConfig.getConfig();
        ConfigurationSection active = data.getConfigurationSection("active-blockers");
        if (active == null) {
            plugin.debug("No saved blockers found in spawnblockersdata.yml.", "spawn_blockers");
            return;
        }

        for (String worldName : active.getKeys(false)) {
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) continue;

            List<Map<?, ?>> list = active.getMapList(worldName);
            for (Map<?, ?> entry : list) {
                String id = String.valueOf(entry.get("id"));
                SpawnBlockerConfig cfg = blockerConfigs.get(id);
                if (cfg == null || !cfg.enabled()) continue;

                Object xObj = entry.get("x");
                Object yObj = entry.get("y");
                Object zObj = entry.get("z");
                if (!(xObj instanceof Number) || !(yObj instanceof Number) || !(zObj instanceof Number)) continue;

                int x = ((Number) xObj).intValue();
                int y = ((Number) yObj).intValue();
                int z = ((Number) zObj).intValue();

                Location loc = new Location(world, x, y, z);

                // Avoid chunk loads on startup: only validate if chunk is already loaded.
                int cx = x >> 4;
                int cz = z >> 4;
                if (world.isChunkLoaded(cx, cz)) {
                    if (world.getBlockAt(loc).getType() != cfg.material()) {
                        continue;
                    }
                }

                if (registerSpawnBlockerInternal(loc, id, false)) {
                    loaded++;
                }
            }
        }

        plugin.debug("Loaded " + loaded + " blockers from spawnblockersdata.yml.", "save_and_load");
    }

    /**
     * Loads and registers blockers stored in this chunk's PDC.
     * Returns number of valid blockers registered.
     */
    public int loadChunkBlockersFromPdc(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String raw = pdc.get(chunkBlockersKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return 0;

        World world = chunk.getWorld();
        int registered = 0;

        // Format: id;x,y,z|id;x,y,z|...
        String[] entries = raw.split("\\|");
        for (String entry : entries) {
            if (entry.isBlank()) continue;

            String[] parts = entry.split(";");
            if (parts.length != 2) continue;

            String id = parts[0];
            SpawnBlockerConfig cfg = blockerConfigs.get(id);
            if (cfg == null || !cfg.enabled()) continue;

            String[] coords = parts[1].split(",");
            if (coords.length != 3) continue;

            int x, y, z;
            try {
                x = Integer.parseInt(coords[0]);
                y = Integer.parseInt(coords[1]);
                z = Integer.parseInt(coords[2]);
            } catch (NumberFormatException ignored) {
                continue;
            }

            Location loc = new Location(world, x, y, z);

            // Validate the block still exists and matches; if not, remove stale PDC entry.
            if (world.getBlockAt(loc).getType() != cfg.material()) {
                removeBlockerFromChunkPdc(loc, id);
                continue;
            }

            // Register into memory (do NOT re-persist; it's already in PDC)
            if (registerSpawnBlockerInternal(loc, id, false)) {
                registered++;
            }
        }

        return registered;
    }

    /**
     * Saves active blockers to spawnblockersdata.yml.
     * Writes only unique "source" blocker locations (deduped across ranged chunk entries).
     */
    public void saveActiveBlockersToDisk() {
        FileConfiguration data = spawnBlockersDataConfig.getConfig();
        data.set("active-blockers", null);

        // world -> list of {id,x,y,z}
        Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();

        // Dedup because each blocker is stored in multiple chunk sets (range)
        Set<String> seen = new HashSet<>();

        for (Map.Entry<String, Map<ChunkCoord, Set<SpawnBlockerData>>> worldEntry : activeBlockers.entrySet()) {
            String worldName = worldEntry.getKey();
            Map<ChunkCoord, Set<SpawnBlockerData>> byChunk = worldEntry.getValue();
            if (byChunk == null || byChunk.isEmpty()) continue;

            for (Set<SpawnBlockerData> set : byChunk.values()) {
                if (set == null || set.isEmpty()) continue;

                for (SpawnBlockerData blocker : set) {
                    Location loc = blocker.location();
                    if (loc == null || loc.getWorld() == null) continue;

                    String key = worldName + ":" + blocker.blockerId() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
                    if (!seen.add(key)) continue;

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", blocker.blockerId());
                    row.put("x", loc.getBlockX());
                    row.put("y", loc.getBlockY());
                    row.put("z", loc.getBlockZ());

                    out.computeIfAbsent(worldName, k -> new ArrayList<>()).add(row);
                }
            }
        }

        for (Map.Entry<String, List<Map<String, Object>>> e : out.entrySet()) {
            data.set("active-blockers." + e.getKey(), e.getValue());
        }

        spawnBlockersDataConfig.saveConfig();
        plugin.debug("Saved " + seen.size() + " blockers to spawnblockersdata.yml.", "save_and_load");
    }

    private boolean registerSpawnBlockerInternal(Location location, String blockerId, boolean persistToChunkPdc) {
        SpawnBlockerConfig config = blockerConfigs.get(blockerId);
        if (config == null || !config.enabled()) {
            return false;
        }

        String worldName = location.getWorld().getName();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        SpawnBlockerData blockerData = new SpawnBlockerData(blockerId, location.clone(), config);

        Map<ChunkCoord, Set<SpawnBlockerData>> worldMap =
                activeBlockers.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>());
        ChunkCoord originChunk = new ChunkCoord(chunkX, chunkZ);
        Set<SpawnBlockerData> originSet = worldMap.get(originChunk);
        if (originSet != null && originSet.contains(blockerData)) {
            return false; // already registered
        }

        for (int dx = -config.chunkRange() + 1; dx < config.chunkRange(); dx++) {
            for (int dz = -config.chunkRange() + 1; dz < config.chunkRange(); dz++) {
                ChunkCoord chunkCoord = new ChunkCoord(chunkX + dx, chunkZ + dz);

                worldMap
                        .computeIfAbsent(chunkCoord, k -> ConcurrentHashMap.newKeySet())
                        .add(blockerData);
            }
        }

        if (persistToChunkPdc) {
            addBlockerToChunkPdc(location, blockerId);
        }

        chunkCheckCache.clear();
        plugin.debug("Registered spawn blocker '" + blockerId + "' at " + location +
                " affecting " + (config.chunkRange() * 2 - 1) + "x" + (config.chunkRange() * 2 - 1) + " chunks", "spawn_blockers");
        return true;
    }

    private void addBlockerToChunkPdc(Location location, String blockerId) {
        Chunk chunk = location.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        String entry = encodePdcEntry(location, blockerId);

        String raw = pdc.get(chunkBlockersKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            pdc.set(chunkBlockersKey, PersistentDataType.STRING, entry);
            return;
        }

        // Avoid duplicates
        List<String> entries = new ArrayList<>(Arrays.asList(raw.split("\\|")));
        if (!entries.contains(entry)) {
            entries.add(entry);
            pdc.set(chunkBlockersKey, PersistentDataType.STRING, String.join("|", entries));
        }
    }

    private void removeBlockerFromChunkPdc(Location location, String blockerId) {
        Chunk chunk = location.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        String raw = pdc.get(chunkBlockersKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return;

        String entry = encodePdcEntry(location, blockerId);

        List<String> entries = new ArrayList<>(Arrays.asList(raw.split("\\|")));
        boolean removed = entries.removeIf(s -> s.equals(entry));

        if (!removed) return;

        if (entries.isEmpty()) {
            pdc.remove(chunkBlockersKey);
        } else {
            pdc.set(chunkBlockersKey, PersistentDataType.STRING, String.join("|", entries));
        }
    }

    private String encodePdcEntry(Location location, String blockerId) {
        return blockerId + ";" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    /**
     * Creates default configuration file if it doesn't exist
     */
    private void createDefaultConfig() {
        FileConfiguration config = spawnBlockersConfig.getConfig();

        // Default vanilla block blocker
        config.set("spawn-blockers.beacon.enabled", true);
        config.set("spawn-blockers.beacon.type", "VANILLA");
        config.set("spawn-blockers.beacon.material", "BEACON");
        config.set("spawn-blockers.beacon.chunk-range", 1);
        config.set("spawn-blockers.beacon.name", "&eSpawn Blocker Beacon");
        config.set("spawn-blockers.beacon.description", "Prevents power mob spawning in this chunk");

        // Default custom item blocker
        config.set("spawn-blockers.soul-torch.enabled", true);
        config.set("spawn-blockers.soul-torch.type", "CUSTOM");
        config.set("spawn-blockers.soul-torch.material", "SOUL_TORCH");
        config.set("spawn-blockers.soul-torch.chunk-range", 2);
        config.set("spawn-blockers.soul-torch.name", "&bMob Repelling torch");
        config.set("spawn-blockers.soul-torch.description", "Prevents power mob spawning in a 3x3 chunk area");

        List<String> lore = Arrays.asList(
                "&7Prevents power mob spawning",
                "&7Range: 3x3 chunks",
                "&e&lPlace to activate"
        );
        config.set("spawn-blockers.soul-torch.lore", lore);

        spawnBlockersConfig.saveConfig();

        // Reload the config
        loadBlockers();
    }

    /**
     * Loads a single blocker configuration from a config section
     */
    private SpawnBlockerConfig loadBlockerConfig(String id, ConfigurationSection section) {
        boolean enabled = section.getBoolean("enabled", true);
        String type = section.getString("type", "VANILLA").toUpperCase();
        Material material = Material.valueOf(section.getString("material", "BEACON").toUpperCase());
        int range = section.getInt("chunk-range", 1);
        String name = section.getString("name", "");
        String description = section.getString("description", "");
        List<String> lore = section.getStringList("lore");

        return new SpawnBlockerConfig(enabled, id, type, material, range, name, description, lore);
    }

    /**
     * Gets all custom spawn blocker items that can be dropped
     *
     * @return Map of item IDs to ItemStacks for droppable spawn blockers
     */
    public Map<String, ItemStack> getDroppableSpawnBlockerItems() {
        Map<String, ItemStack> droppableItems = new HashMap<>();

        for (String blockerId : blockerConfigs.keySet()) {
            SpawnBlockerConfig config = blockerConfigs.get(blockerId);
            if (config != null && config.enabled() && config.type().equals("CUSTOM")) {
                ItemStack item = createSpawnBlockerItem(blockerId);
                if (item != null) {
                    droppableItems.put("spawn-blocker-" + blockerId, item);
                }
            }
        }

        return droppableItems;
    }

    /**
     * Gets a cache key for a chunk
     */
    private String getChunkCacheKey(World world, int chunkX, int chunkZ) {
        return world.getName() + ":" + chunkX + ":" + chunkZ;
    }


    /**
     * Checks if power mob spawning should be blocked at the given location
     */
    public boolean isSpawnBlocked(Location location) {
        // Clear cache periodically
        long now = System.currentTimeMillis();
        if (now - lastCacheClear > CACHE_DURATION) {
            chunkCheckCache.clear();
            lastCacheClear = now;
        }

        // Get chunk coordinates
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        // Check cache first
        String cacheKey = getChunkCacheKey(location.getWorld(), chunkX, chunkZ);
        if (chunkCheckCache.containsKey(cacheKey)) {
            return chunkCheckCache.get(cacheKey);
        }

        boolean blocked = checkSpawnBlocked(location.getWorld(), chunkX, chunkZ);
        chunkCheckCache.put(cacheKey, blocked);
        return blocked;
    }


    /**
     * Internal method to check if spawn is blocked in a specific chunk
     */
    private boolean checkSpawnBlocked(World world, int chunkX, int chunkZ) {
        String worldName = world.getName();
        Map<ChunkCoord, Set<SpawnBlockerData>> worldBlockers = activeBlockers.get(worldName);

        if (worldBlockers == null || worldBlockers.isEmpty()) {
            return false;
        }

        ChunkCoord targetChunk = new ChunkCoord(chunkX, chunkZ);

        // Check if this specific chunk has blockers
        Set<SpawnBlockerData> chunkBlockers = worldBlockers.get(targetChunk);
        if (chunkBlockers != null && !chunkBlockers.isEmpty()) {
            for (SpawnBlockerData blockerData : chunkBlockers) {
                if (blockerData.config().enabled()) {
                    plugin.debug("Spawn blocked in chunk (" + chunkX + ", " + chunkZ + ") by blocker at " +
                            blockerData.location() + " (type: " + blockerData.blockerId() + ")", "spawn_blockers");
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Registers a spawn blocker at the given location
     */
    public void registerSpawnBlocker(Location location, String blockerId) {
        // CHANGED: now also persists to chunk PDC
        registerSpawnBlockerInternal(location, blockerId, true);
    }


    /**
     * Removes a spawn blocker at the given location
     */
    public void removeSpawnBlocker(Location location) {
        String worldName = location.getWorld().getName();
        Map<ChunkCoord, Set<SpawnBlockerData>> worldBlockers = activeBlockers.get(worldName);

        if (worldBlockers != null) {
            String removedId = null;

            // Find and remove all instances of this blocker from all affected chunks
            Iterator<Map.Entry<ChunkCoord, Set<SpawnBlockerData>>> chunkIterator = worldBlockers.entrySet().iterator();
            while (chunkIterator.hasNext()) {
                Map.Entry<ChunkCoord, Set<SpawnBlockerData>> entry = chunkIterator.next();
                Set<SpawnBlockerData> blockers = entry.getValue();

                Iterator<SpawnBlockerData> it = blockers.iterator();
                while (it.hasNext()) {
                    SpawnBlockerData data = it.next();
                    if (data.location().equals(location)) {
                        removedId = data.blockerId();
                        it.remove();
                    }
                }

                // Remove empty chunk entries
                if (blockers.isEmpty()) {
                    chunkIterator.remove();
                }
            }

            if (removedId != null) {
                removeBlockerFromChunkPdc(location, removedId);
            }

            chunkCheckCache.clear(); // Clear cache when blockers change
            plugin.debug("Removed spawn blocker at " + location, "spawn_blockers");
        }
    }

    /**
     * Scans all loaded worlds for existing spawn blockers
     */
    public void scanAllWorldsForBlockers() {
        for (World world : plugin.getServer().getWorlds()) {
            scanWorldForBlockers(world);
        }
    }

    /**
     * Scans a specific world for spawn blockers
     */
    public void scanWorldForBlockers(World world) {
        int loaded = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            loaded += loadChunkBlockersFromPdc(chunk);
        }
        plugin.debug("Loaded " + loaded + " blockers from chunk PDC for world " + world.getName() + ".", "spawn_blockers");
    }

    /**
     * Scans a chunk for spawn blockers
     */
    public void scanChunkForBlockers(org.bukkit.Chunk chunk) {
        loadChunkBlockersFromPdc(chunk);
    }

    /**
     * Creates a custom spawn blocker item
     */
    public ItemStack createSpawnBlockerItem(String blockerId) {
        SpawnBlockerConfig config = blockerConfigs.get(blockerId);
        if (config == null || !config.enabled() || !config.type().equals("CUSTOM")) {
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
            NamespacedKey key = new NamespacedKey(plugin, "spawn-blocker-id");
            dataContainer.set(key, PersistentDataType.STRING, blockerId);

            // Also set custom-id for integration with existing custom item system
            NamespacedKey customIdKey = new NamespacedKey(plugin, "custom-id");
            dataContainer.set(customIdKey, PersistentDataType.STRING, config.id());
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Gets the spawn blocker ID from an ItemStack
     */
    public String getSpawnBlockerId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "spawn-blocker-id");

        return dataContainer.get(key, PersistentDataType.STRING);
    }

    /**
     * Checks if an ItemStack is a spawn blocker item
     */
    public boolean isSpawnBlockerItem(ItemStack item) {
        return getSpawnBlockerId(item) != null;
    }

    /**
     * Gives a player a spawn blocker item
     */
    public boolean giveSpawnBlockerItem(Player player, String blockerId) {
        ItemStack item = createSpawnBlockerItem(blockerId);
        if (item == null) {
            return false;
        }

        player.getInventory().addItem(item);
        return true;
    }

    /**
     * Gets all configured spawn blocker IDs
     */
    public Set<String> getSpawnBlockerIds() {
        return new HashSet<>(blockerConfigs.keySet());
    }

    /**
     * Gets a spawn blocker configuration by ID
     */
    public SpawnBlockerConfig getSpawnBlockerConfig(String blockerId) {
        return blockerConfigs.get(blockerId);
    }

    /**
     * Checks if a specific location has a spawn blocker (for compatibility)
     */
    public boolean hasSpawnBlockerAt(Location location) {
        String worldName = location.getWorld().getName();
        Map<ChunkCoord, Set<SpawnBlockerData>> worldBlockers = activeBlockers.get(worldName);

        if (worldBlockers == null) return false;

        // Only check the chunks that could contain this blocker
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        // Check all possible chunk ranges where this location could be a blocker source
        for (Map.Entry<ChunkCoord, Set<SpawnBlockerData>> entry : worldBlockers.entrySet()) {
            ChunkCoord coord = entry.getKey();
            Set<SpawnBlockerData> blockers = entry.getValue();

            // Quick distance check to avoid unnecessary iteration
            int dx = Math.abs(coord.x() - chunkX);
            int dz = Math.abs(coord.z() - chunkZ);

            // Skip chunks that are too far away to contain blockers affecting this location
            if (dx > 5 || dz > 5) continue; // Reasonable max chunk range

            for (SpawnBlockerData blocker : blockers) {
                if (blocker.location().equals(location)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if a specific location has a spawn blocker (for compatibility)
     */
    public SpawnBlockerData getSpawnBlockerAt(Location location) {
        String worldName = location.getWorld().getName();
        Map<ChunkCoord, Set<SpawnBlockerData>> worldBlockers = activeBlockers.get(worldName);

        if (worldBlockers == null) return null;

        // Only check the chunks that could contain this blocker
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        // Check all possible chunk ranges where this location could be a blocker source
        for (Map.Entry<ChunkCoord, Set<SpawnBlockerData>> entry : worldBlockers.entrySet()) {
            ChunkCoord coord = entry.getKey();
            Set<SpawnBlockerData> blockers = entry.getValue();

            // Quick distance check to avoid unnecessary iteration
            int dx = Math.abs(coord.x() - chunkX);
            int dz = Math.abs(coord.z() - chunkZ);

            // Skip chunks that are too far away to contain blockers affecting this location
            if (dx > 5 || dz > 5) continue; // Reasonable max chunk range

            for (SpawnBlockerData blocker : blockers) {
                if (blocker.location().equals(location)) {
                    return blocker;
                }
            }
        }

        return null;
    }

    /**
     * Gets all active spawn blockers in a world
     */
    public Map<ChunkCoord, Set<SpawnBlockerData>> getActiveBlockers(World world) {
        return activeBlockers.getOrDefault(world.getName(), Collections.emptyMap());
    }

    /**
     * Represents chunk coordinates
     */
    public record ChunkCoord(int x, int z) {

        @Override
        public String toString() {
            return "(" + x + ", " + z + ")";
        }
    }

    /**
     * Helper class for storing blocker registration information.
     */
    private record BlockerRegistration(Location location, String blockerId) {
    }

    /**
     * Data class for spawn blocker information
     */
    public record SpawnBlockerData(String blockerId, Location location, SpawnBlockerConfig config) {

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SpawnBlockerData that = (SpawnBlockerData) obj;
            return Objects.equals(blockerId, that.blockerId) &&
                    Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockerId, location);
        }
    }

    /**
     * Configuration class for spawn blockers
     *
     * @param type VANILLA or CUSTOM
     */
    public record SpawnBlockerConfig(boolean enabled, String id, String type, Material material, int chunkRange,
                                     String name, String description, List<String> lore) {
        public SpawnBlockerConfig(boolean enabled, String id, String type, Material material, int chunkRange,
                                  String name, String description, List<String> lore) {
            this.enabled = enabled;
            this.id = id;
            this.type = type;
            this.material = material;
            this.chunkRange = chunkRange;
            this.name = name;
            this.description = description;
            this.lore = lore != null ? lore : new ArrayList<>();
        }
    }
}
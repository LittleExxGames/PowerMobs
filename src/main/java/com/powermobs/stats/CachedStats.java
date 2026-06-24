package com.powermobs.stats;

import com.powermobs.PowerMobsPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CachedStats {

    // ONLY holds data for players currently logged into the server
    private static final Map<UUID, Map<String, MobStats>> playerMobStats = new ConcurrentHashMap<>();
    private static final Map<String, GlobalStats> globalStats = new ConcurrentHashMap<>();

    public static void addJoinedPlayer(UUID playerUuid, Map<String, MobStats> mobList) {
        if (playerMobStats.containsKey(playerUuid)) {
            PowerMobsPlugin.getInstance().getLogger().warning("Player " + playerUuid + " already has stats in the cache!");
        }
        playerMobStats.put(playerUuid, new ConcurrentHashMap<>(mobList));
    }

    public static void removePlayer(UUID playerUuid, boolean updateDatabase) {
        if (updateDatabase) {
            PowerMobsPlugin.getInstance().getStatsManager().flushSinglePlayerStats(playerUuid);
        }
        playerMobStats.remove(playerUuid);
    }

    public static void deletePlayer(UUID playerUuid) {
        playerMobStats.put(playerUuid, new ConcurrentHashMap<>());
        PowerMobsPlugin.getInstance().getStatsManager().clearAllPlayerData(playerUuid);
    }

    public static Map<String, MobStats> getPlayerMobStats(UUID playerUuid) {
        return playerMobStats.getOrDefault(playerUuid, new ConcurrentHashMap<>());
    }

    public static GlobalStats getGlobalStats(String mobId) {
        return globalStats.getOrDefault(mobId, new GlobalStats(0, 0, 0.0));
    }

    public static Map<UUID, Map<String, MobStats>> getAllPlayerMobStats() {
        return playerMobStats;
    }

    public static Map<String, GlobalStats> getAllGlobalStats() {
        return globalStats;
    }

    public static void updateGlobalStats(String mobId, GlobalStats stats) {
        globalStats.put(mobId, stats);
    }

    public static void updatePlayerStats(UUID playerUuid, String mobId, int addKills, int addDeaths, double damageDealt, double maxDamageDealt) {
        Map<String, MobStats> mobMap = playerMobStats.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        MobStats mobStats = mobMap.computeIfAbsent(mobId, k -> new MobStats(0, 0, 0.0, 0.0));

        mobStats.kills += addKills;
        mobStats.deaths += addDeaths;
        mobStats.totalDamage += damageDealt;
        mobStats.lastUpdated = System.currentTimeMillis();
        mobStats.maxDamage = Math.max(mobStats.maxDamage, maxDamageDealt);
        mobStats.markDirty();

        GlobalStats totalMobRecords = globalStats.computeIfAbsent(mobId, k -> new GlobalStats(0, 0, 0.0));

        totalMobRecords.totalKills += addKills;
        totalMobRecords.totalDeaths += addDeaths;
        totalMobRecords.totalDamage += damageDealt;
    }

    public static class MobStats {
        public int kills;
        public int deaths;
        public double maxDamage;
        public double totalDamage;
        public long lastUpdated;
        private boolean isDirty = false;

        public MobStats(int kills, int deaths, double maxDamage, double totalDamage) {
            this.kills = kills;
            this.deaths = deaths;
            this.maxDamage = maxDamage;
            this.totalDamage = totalDamage;
            this.lastUpdated = System.currentTimeMillis();
        }

        public synchronized void markClean() { this.isDirty = false; }
        public synchronized void markDirty() { this.isDirty = true; }
        public synchronized boolean isDirty() { return this.isDirty; }
    }

    public static class GlobalStats {
        public int totalKills;
        public int totalDeaths;
        public double totalDamage;

        public GlobalStats(int totalKills, int totalDeaths, double totalDamage) {
            this.totalKills = totalKills;
            this.totalDeaths = totalDeaths;
            this.totalDamage = totalDamage;
        }
    }
}

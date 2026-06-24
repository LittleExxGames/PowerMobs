package com.powermobs.stats;

import com.powermobs.PowerMobsPlugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StatsManager {
    private final PowerMobsPlugin plugin;
    private HikariDataSource dataSource;

    public StatsManager(PowerMobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        String type = plugin.getConfig().getString("database.type", "SQLite").toUpperCase();

        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("MYSQL")) {
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "minecraft");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10); // Allow up to 10 simultaneous connections

        } else {
            File dataFolder = new File(plugin.getDataFolder(), "stats.db");
            if (!plugin.getDataFolder().exists()) {
                if(plugin.getDataFolder().mkdirs()){
                    plugin.getLogger().info("Created data folder");
                }
            }

            config.setJdbcUrl("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            config.setMaximumPoolSize(1); // SQLite handles file locks best with 1 pool connection
        }

        // Tweak performance optimization settings common to both types
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection(); Statement s = conn.createStatement()) {
            if (type.equalsIgnoreCase("SQLITE")) {
                s.execute("PRAGMA journal_mode = WAL;");
                s.execute("PRAGMA synchronous = NORMAL;");
                s.execute("PRAGMA foreign_keys = ON;");
            }

            s.execute("CREATE TABLE IF NOT EXISTS player_mob_stats (" +
                    "player_uuid VARCHAR(36), " +
                    "mob_id VARCHAR(64), " +
                    "kills_count INT DEFAULT 0, " +
                    "deaths_count INT DEFAULT 0, " +
                    "max_damage_dealt DOUBLE DEFAULT 0.0, " +
                    "total_damage_dealt DOUBLE DEFAULT 0.0, " +
                    "PRIMARY KEY (player_uuid, mob_id));");

        } catch (SQLException e) {
            plugin.getLogger().severe("Database initialization failed!");
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /** Starts the background repeating scheduler task for active gameplay saves */
    public void startSaveTask() {
        // Runs asynchronously every 60 seconds (1200 ticks) while players are active
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveAllActiveCaches(true);
        }, 1200L, 1200L);
    }

    /**
     * Scans all active player memory profiles and forces a unified batch save to disk.
     * @param runAsynchronously Set to TRUE for live gameplay timers, FALSE for server shutdowns.
     */
    public void saveAllActiveCaches(boolean runAsynchronously) {
        var cachedData = CachedStats.getAllPlayerMobStats();
        if (cachedData.isEmpty()) return;

        String type = plugin.getConfig().getString("database.type", "SQLite");
        String sql = type.equalsIgnoreCase("SQLITE") ?
                "INSERT INTO player_mob_stats (player_uuid, mob_id, kills_count, deaths_count, max_damage_dealt, total_damage_dealt) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid, mob_id) DO UPDATE SET kills_count = EXCLUDED.kills_count, deaths_count = EXCLUDED.deaths_count, max_damage_dealt = MAX(max_damage_dealt, EXCLUDED.max_damage_dealt), total_damage_dealt = EXCLUDED.total_damage_dealt;" :
                "INSERT INTO player_mob_stats (player_uuid, mob_id, kills_count, deaths_count, max_damage_dealt, total_damage_dealt) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE kills_count = VALUES(kills_count), deaths_count = VALUES(deaths_count), max_damage_dealt = GREATEST(max_damage_dealt, VALUES(max_damage_dealt)), total_damage_dealt = VALUES(total_damage_dealt);";

        Runnable saveLogic = () -> {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    boolean hasUpdates = false;

                    for (Map.Entry<UUID, Map<String, CachedStats.MobStats>> playerEntry : cachedData.entrySet()) {
                        String uuidStr = playerEntry.getKey().toString();

                        for (Map.Entry<String, CachedStats.MobStats> mobEntry : playerEntry.getValue().entrySet()) {
                            CachedStats.MobStats stats = mobEntry.getValue();

                            if (stats.isDirty()) {
                                ps.setString(1, uuidStr);
                                ps.setString(2, mobEntry.getKey());
                                ps.setInt(3, stats.kills);
                                ps.setInt(4, stats.deaths);
                                ps.setDouble(5, stats.maxDamage);
                                ps.setDouble(6, stats.totalDamage);
                                ps.addBatch();

                                stats.markClean();
                                hasUpdates = true;
                            }
                        }
                    }

                    if (hasUpdates) {
                        ps.executeBatch();
                        conn.commit();
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error executing unified statistics database flush!");
                e.printStackTrace();
            }
        };

        if (runAsynchronously) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveLogic);
        } else {
            saveLogic.run(); // Runs instantly right here on the main thread (Crucial for shutdown)
        }
    }

    /**
     * Safely flushes any unsaved (dirty) data for a single disconnecting player
     * down to the database before their session cache is entirely removed.
     */
    public void flushSinglePlayerStats(UUID playerUuid) {
        Map<String, CachedStats.MobStats> playerMobMap = CachedStats.getPlayerMobStats(playerUuid);
        if (playerMobMap == null || playerMobMap.isEmpty()) return;

        String type = plugin.getConfig().getString("database.type", "SQLite");
        String sql = type.equalsIgnoreCase("SQLITE") ?
                "INSERT INTO player_mob_stats (player_uuid, mob_id, kills_count, deaths_count, max_damage_dealt, total_damage_dealt) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid, mob_id) DO UPDATE SET kills_count = EXCLUDED.kills_count, deaths_count = EXCLUDED.deaths_count, max_damage_dealt = MAX(max_damage_dealt, EXCLUDED.max_damage_dealt), total_damage_dealt = EXCLUDED.total_damage_dealt;" :
                "INSERT INTO player_mob_stats (player_uuid, mob_id, kills_count, deaths_count, max_damage_dealt, total_damage_dealt) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE kills_count = VALUES(kills_count), deaths_count = VALUES(deaths_count), max_damage_dealt = GREATEST(max_damage_dealt, VALUES(max_damage_dealt)), total_damage_dealt = VALUES(total_damage_dealt);";

        String uuidStr = playerUuid.toString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    boolean hasUpdates = false;
                    for (Map.Entry<String, CachedStats.MobStats> entry : playerMobMap.entrySet()) {
                        CachedStats.MobStats stats = entry.getValue();
                        if (stats.isDirty()) {
                            ps.setString(1, uuidStr);
                            ps.setString(2, entry.getKey());
                            ps.setInt(3, stats.kills);
                            ps.setInt(4, stats.deaths);
                            ps.setDouble(5, stats.maxDamage);
                            ps.setDouble(6, stats.totalDamage);
                            ps.addBatch();
                            hasUpdates = true;
                        }
                    }
                    if (hasUpdates) {
                        ps.executeBatch();
                        conn.commit();
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to execute data flush for disconnecting player: " + uuidStr);
                e.printStackTrace();
            }
        });
    }


    /**
     * Asynchronously retrieves the custom mob statistics breakdown for a specific player.
     *
     * @param playerUuid The UUID of the player profile to load.
     * @return A future promise containing a map of Mob ID strings to their respective stats.
     */
    public CompletableFuture<Map<String, CachedStats.MobStats>> loadSpecificPlayerStats(UUID playerUuid) {
        CompletableFuture<Map<String, CachedStats.MobStats>> future = new CompletableFuture<>();
        String sql = "SELECT mob_id, kills_count, deaths_count, max_damage_dealt, total_damage_dealt " +
                "FROM player_mob_stats WHERE player_uuid = ?;";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, CachedStats.MobStats> mobStatsMap = new ConcurrentHashMap<>();

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String mobId = rs.getString("mob_id");
                        CachedStats.MobStats stats = new CachedStats.MobStats(
                                rs.getInt("kills_count"),
                                rs.getInt("deaths_count"),
                                rs.getDouble("max_damage_dealt"),
                                rs.getDouble("total_damage_dealt")
                        );
                        mobStatsMap.put(mobId, stats);
                    }
                }
                future.complete(mobStatsMap);

            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Scans the database table to calculate server-wide grand totals per mob ID,
     * then safely initializes the CachedStats global map.
     */
    public void loadGlobalMobTotalsIntoCache() {
        String sql = "SELECT mob_id, " +
                "       SUM(kills_count) AS total_k, " +
                "       SUM(deaths_count) AS total_d, " +
                "       SUM(total_damage_dealt) AS total_dmg " +
                "FROM player_mob_stats GROUP BY mob_id;";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getLogger().info("Loading global power mob totals into memory...");

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String mobId = rs.getString("mob_id");

                    CachedStats.GlobalStats compiledTotals = new CachedStats.GlobalStats(
                            rs.getInt("total_k"),
                            rs.getInt("total_d"),
                            rs.getDouble("total_dmg")
                    );

                    CachedStats.updateGlobalStats(mobId, compiledTotals);
                }

                plugin.getLogger().info("Successfully cached global stats for all custom mobs!");

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to calculate global mob milestones!");
                e.printStackTrace();
            }
        });
    }

    /** Get how many times a specific player has killed a specific mob */
    public CompletableFuture<Integer> getPlayerKillsForMob(UUID playerUuid, String mobId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String sql = "SELECT kills_count FROM player_mob_stats WHERE player_uuid = ? AND mob_id = ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, playerUuid.toString());
                ps.setString(2, mobId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        future.complete(rs.getInt("kills_count"));
                    } else {
                        future.complete(0);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /** Get how damage a player has done to a specific mob in one fight */
    public CompletableFuture<Integer> getPlayerMaxDamageForMob(UUID playerUuid, String mobId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String sql = "SELECT max_damage_dealt FROM player_mob_stats WHERE player_uuid = ? AND mob_id = ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, playerUuid.toString());
                ps.setString(2, mobId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        future.complete(rs.getInt("max_damage_dealt"));
                    } else {
                        future.complete(0);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /** Get how damage a player has done to a specific mob */
    public CompletableFuture<Integer> getPlayerTotalDamageForMob(UUID playerUuid, String mobId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String sql = "SELECT total_damage_dealt FROM player_mob_stats WHERE player_uuid = ? AND mob_id = ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, playerUuid.toString());
                ps.setString(2, mobId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        future.complete(rs.getInt("total_damage_dealt"));
                    } else {
                        future.complete(0);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /** Get how many times a player has died to a specific mob */
    public CompletableFuture<Integer> getPlayerDeathsToMob(UUID playerUuid, String mobId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String sql = "SELECT deaths_count FROM player_mob_stats WHERE player_uuid = ? AND mob_id = ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, playerUuid.toString());
                ps.setString(2, mobId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        future.complete(rs.getInt("deaths_count"));
                    } else {
                        future.complete(0);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }


    public record LeaderboardEntry(UUID playerUuid, double score) {}

    /** Top players who have killed the most of a specific power mob */
    public CompletableFuture<List<LeaderboardEntry>> getTopKillerToMob(String mobId, int limit) {
        CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();
        String sql = "SELECT player_uuid, kills_count FROM player_mob_stats WHERE mob_id = ? ORDER BY kills_count DESC LIMIT ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> le = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, mobId);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        le.add(new LeaderboardEntry(
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getInt("kills_count")
                        ));
                    }
                }
                future.complete(le);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /** Top players who have killed the most power mobs */
    public CompletableFuture<List<LeaderboardEntry>> getTopKillerAcrossAllMobs(int limit) {
        CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();
        String sql = "SELECT player_uuid, SUM(kills_count) AS total_kills FROM player_mob_stats GROUP BY player_uuid ORDER BY total_kills DESC LIMIT ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> le = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        le.add(new LeaderboardEntry(
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getDouble("total_kills")
                        ));
                    }
                }
                future.complete(le);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /** Top players who have dealt the highest single-fight damage to a specific power mob */
    public CompletableFuture<List<LeaderboardEntry>> getTopMaxDamageToMob(String mobId, int limit) {
        CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();
        String sql = "SELECT player_uuid, max_damage_dealt FROM player_mob_stats WHERE mob_id = ? ORDER BY max_damage_dealt DESC LIMIT ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> le = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, mobId);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        le.add(new LeaderboardEntry(
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getDouble("max_damage_dealt")
                        ));
                    }
                }
                future.complete(le);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /** Top players who have killed the most power mobs */
    public CompletableFuture<List<LeaderboardEntry>> getTopMaxDamageAcrossAllMobs(int limit) {
        CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();
        String sql = "SELECT player_uuid, SUM(max_damage_dealt) AS max_damage FROM player_mob_stats GROUP BY player_uuid ORDER BY max_damage DESC LIMIT ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> le = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        le.add(new LeaderboardEntry(
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getDouble("max_damage")
                        ));
                    }
                }
                future.complete(le);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /** Top players who have dealt the most damage to a specific power mob */
    public CompletableFuture<List<LeaderboardEntry>> getTopDamageToMob(String mobId, int limit) {
        CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();
        String sql = "SELECT player_uuid, total_damage_dealt FROM player_mob_stats WHERE mob_id = ? ORDER BY total_damage_dealt DESC LIMIT ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> le = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, mobId);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        le.add(new LeaderboardEntry(
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getInt("total_damage_dealt")
                        ));
                    }
                }
                future.complete(le);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /** Top players who have dealt the most damage across ALL power mobs combined */
    public CompletableFuture<List<LeaderboardEntry>> getTopDamageAcrossAllMobs(int limit) {
        CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();
        String sql = "SELECT player_uuid, SUM(total_damage_dealt) AS total_damage FROM player_mob_stats GROUP BY player_uuid ORDER BY total_damage DESC LIMIT ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> le = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        le.add(new LeaderboardEntry(
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getDouble("total_damage")
                        ));
                    }
                }
                future.complete(le);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /** Top players who have died the most to a specific power mob */
    public CompletableFuture<List<LeaderboardEntry>> getTopDeathsToMob(String mobId, int limit) {
        CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();
        String sql = "SELECT player_uuid, deaths_count FROM player_mob_stats WHERE mob_id = ? ORDER BY deaths_count DESC LIMIT ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> le = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, mobId);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        le.add(new LeaderboardEntry(
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getInt("deaths_count")
                        ));
                    }
                }
                future.complete(le);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /** Top players who have died the least across all power mobs combined */
    public CompletableFuture<List<LeaderboardEntry>> getTopDeathsAcrossAllMobs(int limit) {
        CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();

        String sql = "SELECT player_uuid, SUM(deaths_count) AS total_deaths " +
                "FROM player_mob_stats GROUP BY player_uuid ORDER BY total_deaths DESC LIMIT ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> le = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement s = conn.prepareStatement(sql)) {

                s.setInt(1, limit);
                try (ResultSet rs = s.executeQuery()) {
                    while (rs.next()) {
                        le.add(new LeaderboardEntry(
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getInt("total_deaths")
                        ));
                    }
                }
                future.complete(le);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /** Completely wipes all statistical traces of a single player */
    public void clearAllPlayerData(UUID player) {
        String sql = "DELETE FROM player_mob_stats WHERE player_uuid = ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, player.toString());
                ps.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void recordKill(UUID playerUuid, String mobId) {
        String dbType = plugin.getConfig().getString("database.type", "SQLite");
        String mobQuery;

        if (dbType.equalsIgnoreCase("SQLITE")) {
            mobQuery = "INSERT INTO player_mob_stats (player_uuid, mob_id, kills_count) VALUES (?, ?, 1) " +
                    "ON CONFLICT(player_uuid, mob_id) DO UPDATE SET kills_count = kills_count + 1";
        } else {
            mobQuery = "INSERT INTO player_mob_stats (player_uuid, mob_id, kills_count) VALUES (?, ?, 1) " +
                    "ON DUPLICATE KEY UPDATE kills_count = kills_count + 1";
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(mobQuery)) {

                ps.setString(1, playerUuid.toString());
                ps.setString(2, mobId);
                ps.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void recordDamage(UUID playerUuid, String mobId, double damage) {
        String uuidStr = playerUuid.toString();
        String dbType = plugin.getConfig().getString("database.type", "SQLite");

        String mobQuery;

        if (dbType.equalsIgnoreCase("SQLITE")) {
            mobQuery = "INSERT INTO player_mob_stats (player_uuid, mob_id, max_damage_dealt, total_damage_dealt) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(player_uuid, mob_id) DO UPDATE SET max_damage_dealt = MAX(max_damage_dealt, EXCLUDED.max_damage_dealt), total_damage_dealt = total_damage_dealt + EXCLUDED.total_damage_dealt";
        } else {
            mobQuery = "INSERT INTO player_mob_stats (player_uuid, mob_id, max_damage_dealt, total_damage_dealt) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE max_damage_dealt = GREATEST(max_damage_dealt, VALUES(max_damage_dealt)), total_damage_dealt = total_damage_dealt + VALUES(total_damage_dealt)";
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection()) {
                PreparedStatement ps1 = conn.prepareStatement(mobQuery);
                    ps1.setString(1, uuidStr);
                    ps1.setString(2, mobId);
                    ps1.setDouble(3, damage);
                    ps1.setDouble(4, damage);
                    ps1.executeUpdate();

                    conn.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * Deletes a player's history for a specific power mob and recalculates their global totals.
     */
    public void clearSpecificMobData(UUID playerUuid, String mobId) {
        String uuidStr = playerUuid.toString();

        String deleteMobSql = "DELETE FROM player_mob_stats WHERE player_uuid = ? AND mob_id = ?;";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection()) {
                PreparedStatement psDelete = conn.prepareStatement(deleteMobSql);
                    psDelete.setString(1, uuidStr);
                    psDelete.setString(2, mobId);
                    psDelete.executeUpdate();
                    conn.commit();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }


    /** Deletes a specific power mob completely from the database **/
    public void clearAllSpecificMobData(String mobId) {
        String sql = "DELETE FROM player_mob_stats WHERE mob_id = ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, mobId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Calculates a player's exact server-wide total damage rank.
     * @param playerUuid The UUID of the player asking for their rank.
     */
    public CompletableFuture<Integer> getPlayerDamageRank(UUID playerUuid) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String sql = "SELECT COUNT(*) + 1 AS rank FROM (" +
                "  SELECT player_uuid, SUM(total_damage_dealt) AS total " +
                "  FROM player_mob_stats " +
                "  GROUP BY player_uuid" +
                ") t1 WHERE total > (" +
                "  SELECT COALESCE(SUM(total_damage_dealt), 0) " +
                "  FROM player_mob_stats WHERE player_uuid = ?" +
                ");";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        future.complete(rs.getInt("rank"));
                    } else {
                        future.complete(1);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Calculates a player's exact server-wide total damage rank.
     * @param playerUuid The UUID of the player asking for their rank.
     */
    public CompletableFuture<Integer> getPlayerMaxDamageRank(UUID playerUuid) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String sql = "SELECT COUNT(*) + 1 AS rank FROM (" +
                "  SELECT player_uuid, SUM(max_damage_dealt) AS total " +
                "  FROM player_mob_stats " +
                "  GROUP BY player_uuid" +
                ") t1 WHERE total > (" +
                "  SELECT COALESCE(SUM(max_damage_dealt), 0) " +
                "  FROM player_mob_stats WHERE player_uuid = ?" +
                ");";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        future.complete(rs.getInt("rank"));
                    } else {
                        future.complete(1);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Calculates a player's exact server-wide total kills rank.
     * @param playerUuid The UUID of the player asking for their rank.
     */
    public CompletableFuture<Integer> getPlayerKillsRank(UUID playerUuid) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String sql = "SELECT COUNT(*) + 1 AS rank FROM (" +
                "  SELECT player_uuid, SUM(kills_count) AS total " +
                "  FROM player_mob_stats " +
                "  GROUP BY player_uuid" +
                ") t1 WHERE total > (" +
                "  SELECT COALESCE(SUM(kills_count), 0) " +
                "  FROM player_mob_stats WHERE player_uuid = ?" +
                ");";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        future.complete(rs.getInt("rank"));
                    } else {
                        future.complete(1);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Calculates a player's exact server-wide total deaths rank.
     * Lower deaths rank better than higher deaths.
     *
     * @param playerUuid The UUID of the player asking for their rank.
     * @return rank future
     */
    public CompletableFuture<Integer> getPlayerDeathsRank(UUID playerUuid) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String sql = "SELECT COUNT(*) + 1 AS rank FROM (" +
                "  SELECT player_uuid, SUM(deaths_count) AS total " +
                "  FROM player_mob_stats " +
                "  GROUP BY player_uuid" +
                ") t1 WHERE total < (" +
                "  SELECT COALESCE(SUM(deaths_count), 0) " +
                "  FROM player_mob_stats WHERE player_uuid = ?" +
                ");";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        future.complete(rs.getInt("rank"));
                    } else {
                        future.complete(1);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}

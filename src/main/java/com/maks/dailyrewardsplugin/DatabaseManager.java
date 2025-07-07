package com.maks.dailyrewardsplugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    private DailyRewardsPlugin plugin;
    private HikariDataSource dataSource;
    private int debuggingFlag = 1; // Set to 0 in production

    public DatabaseManager(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "dailyrewards");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");

        HikariConfig config = new HikariConfig();
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?characterEncoding=utf8&useSSL=false&autoReconnect=true&useUnicode=true" +
                "&socketTimeout=3000&connectTimeout=3000"; // Add explicit socket timeouts

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        // Connection pool settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // Timeout settings - critical for preventing server freezes
        config.setConnectionTimeout(3000); // 3 seconds timeout
        config.setValidationTimeout(2000); // 2 seconds validation timeout
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(3);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(10)); // 10 minutes
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(30)); // 30 minutes
        config.setLeakDetectionThreshold(60000); // 1 minute

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");

        if (debuggingFlag == 1) {
            plugin.getLogger().info("[DailyRewards] Connecting to MySQL database: " + host + ":" + port + "/" + database);
        }

        try {
            dataSource = new HikariDataSource(config);
            Bukkit.getLogger().info("[DailyRewards] Connected to MySQL database using HikariCP!");

            // Initialize tables asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::initializeTables);
        } catch (Exception e) {
            Bukkit.getLogger().severe("[DailyRewards] Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initializes database tables
     */
    private void initializeTables() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            if (debuggingFlag == 1) {
                plugin.getLogger().info("[DailyRewards] Initializing database tables");
            }

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS daily_rewards (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "daily_streak INT DEFAULT 1, " +
                            "last_login_date DATE, " +
                            "claimed_rewards TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, " +
                            "rank_history TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" +
                            ")"
            );

            // Add rank_history column if it doesn't exist
            try {
                statement.executeUpdate(
                        "ALTER TABLE daily_rewards ADD COLUMN IF NOT EXISTS rank_history TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                );
            } catch (SQLException e) {
                // Some MySQL versions don't support IF NOT EXISTS for ADD COLUMN
                // Check if the error is about the column already existing
                if (!e.getMessage().contains("Duplicate column")) {
                    throw e;
                }
            }
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS daily_rewards_config (" +
                            "day INT, " +
                            "rank VARCHAR(20), " +
                            "items TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, " +
                            "money DOUBLE, " +
                            "PRIMARY KEY (day, rank)" +
                            ")"
            );

            if (debuggingFlag == 1) {
                plugin.getLogger().info("[DailyRewards] Database tables initialized successfully");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[DailyRewards] Failed to initialize database tables: " + e.getMessage());
            if (debuggingFlag == 1) {
                e.printStackTrace();
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            Bukkit.getLogger().severe("[DailyRewards] Attempted to get connection from closed datasource!");
            throw new SQLException("DataSource is closed");
        }
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[DailyRewards] Disconnected from MySQL database (HikariCP closed).");
            }
        }
    }

    public PlayerData getPlayerData(String uuid) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM daily_rewards WHERE uuid = ?")) {
            ps.setString(1, uuid);
            ps.setQueryTimeout(2); // Add timeout to prevent hanging
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int dailyStreak = rs.getInt("daily_streak");
                    Date lastLoginDate = rs.getDate("last_login_date");
                    String claimedRewards = rs.getString("claimed_rewards");
                    String rankHistory = rs.getString("rank_history");
                    return new PlayerData(uuid, dailyStreak, lastLoginDate != null ? lastLoginDate.toLocalDate() : LocalDate.now(), claimedRewards, rankHistory != null ? rankHistory : "{}");
                } else {
                    createPlayerData(uuid);
                    return new PlayerData(uuid, 1, LocalDate.now(), "{}");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[DailyRewards] Error getting player data: " + e.getMessage());
            if (debuggingFlag == 1) {
                e.printStackTrace();
            }
            return new PlayerData(uuid, 1, LocalDate.now(), "{}");
        }
    }

    public void createPlayerData(String uuid) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO daily_rewards (uuid, daily_streak, last_login_date, claimed_rewards, rank_history) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid);
            ps.setInt(2, 1);
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            ps.setString(4, "{}");
            ps.setString(5, "{}");
            ps.setQueryTimeout(2); // Add timeout to prevent hanging
            ps.executeUpdate();

            if (debuggingFlag == 1) {
                plugin.getLogger().info("[DailyRewards] Created new player data for UUID: " + uuid);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[DailyRewards] Error creating player data: " + e.getMessage());
            if (debuggingFlag == 1) {
                e.printStackTrace();
            }
        }
    }

    public void updatePlayerData(PlayerData data) {
        // Run asynchronously to avoid blocking the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "UPDATE daily_rewards SET daily_streak = ?, last_login_date = ?, claimed_rewards = ?, rank_history = ? WHERE uuid = ?")) {
                ps.setInt(1, data.getDailyStreak());
                ps.setDate(2, Date.valueOf(data.getLastLoginDate()));
                ps.setString(3, data.getClaimedRewards() != null ? data.getClaimedRewards() : "{}");
                ps.setString(4, data.getRankHistory() != null ? data.getRankHistory() : "{}");
                ps.setString(5, data.getUuid());
                ps.setQueryTimeout(2); // Add timeout to prevent hanging
                ps.executeUpdate();

                if (debuggingFlag == 1) {
                    plugin.getLogger().info("[DailyRewards] Updated player data for UUID: " + data.getUuid());
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("[DailyRewards] Error updating player data: " + e.getMessage());
                if (debuggingFlag == 1) {
                    e.printStackTrace();
                }
            }
        });
    }
}

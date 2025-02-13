package com.maks.dailyrewardsplugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.*;
import java.time.LocalDate;

public class DatabaseManager {
    private DailyRewardsPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        String host = plugin.getConfig().getString("database.host");
        int port = plugin.getConfig().getInt("database.port");
        String database = plugin.getConfig().getString("database.database");
        String username = plugin.getConfig().getString("database.username");
        String password = plugin.getConfig().getString("database.password");

        HikariConfig config = new HikariConfig();
        // Skonstruuj URL JDBC. Upewnij się, że używasz charsetu UTF8, a opcjonalnie możesz dodać dodatkowe parametry.
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?characterEncoding=utf8&useSSL=false&autoReconnect=true";
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(10);
        // Możesz dodać inne opcje konfiguracyjne HikariCP tutaj

        dataSource = new HikariDataSource(config);
        Bukkit.getLogger().info("Connected to MySQL database using HikariCP!");

        // Tworzymy tabele, jeśli nie istnieją. Używamy charsetu utf8mb4 dla kolumny items.
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS daily_rewards (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "daily_streak INT DEFAULT 1, " +
                            "last_login_date DATE, " +
                            "claimed_rewards TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" +
                            ")"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS daily_rewards_config (" +
                            "day INT, " +
                            "rank VARCHAR(20), " +
                            "items TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, " +
                            "money DOUBLE, " +
                            "PRIMARY KEY (day, rank)" +
                            ")"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Bukkit.getLogger().info("Disconnected from MySQL database (HikariCP closed).");
        }
    }

    public PlayerData getPlayerData(String uuid) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM daily_rewards WHERE uuid = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int dailyStreak = rs.getInt("daily_streak");
                    Date lastLoginDate = rs.getDate("last_login_date");
                    String claimedRewards = rs.getString("claimed_rewards");
                    return new PlayerData(uuid, dailyStreak, lastLoginDate != null ? lastLoginDate.toLocalDate() : LocalDate.now(), claimedRewards);
                } else {
                    createPlayerData(uuid);
                    return new PlayerData(uuid, 1, LocalDate.now(), "{}");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void createPlayerData(String uuid) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO daily_rewards (uuid, daily_streak, last_login_date, claimed_rewards) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid);
            ps.setInt(2, 1); // Początkowy daily streak = 1
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            ps.setString(4, "{}");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerData(PlayerData data) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE daily_rewards SET daily_streak = ?, last_login_date = ?, claimed_rewards = ? WHERE uuid = ?")) {
            ps.setInt(1, data.getDailyStreak());
            ps.setDate(2, Date.valueOf(data.getLastLoginDate()));
            ps.setString(3, data.getClaimedRewards() != null ? data.getClaimedRewards() : "{}");
            ps.setString(4, data.getUuid());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

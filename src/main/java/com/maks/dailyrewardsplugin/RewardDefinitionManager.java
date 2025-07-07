package com.maks.dailyrewardsplugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardDefinitionManager {
    private DailyRewardsPlugin plugin;
    private Gson gson = new Gson();
    private Map<String, RewardDefinition> cache = new HashMap<>();
    private int debuggingFlag = 1; // Set to 0 in production

    public RewardDefinitionManager(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    private String getCacheKey(int day, String rank) {
        return day + ":" + rank;
    }

    public RewardDefinition getRewardDefinition(int day, String rank) {
        // Check cache first
        String cacheKey = getCacheKey(day, rank);
        if (cache.containsKey(cacheKey)) {
            if (debuggingFlag == 1) {
                plugin.getLogger().info("[DailyRewards] Cache hit for day " + day + ", rank " + rank);
            }
            return cache.get(cacheKey);
        }

        if (debuggingFlag == 1) {
            plugin.getLogger().info("[DailyRewards] Cache miss, loading from DB: day " + day + ", rank " + rank);
        }

        RewardDefinition def = null;

        // Try to load from database in a non-blocking way
        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM daily_rewards_config WHERE day = ? AND rank = ?")) {
            ps.setInt(1, day);
            ps.setString(2, rank);

            // Set timeout to prevent hanging
            ps.setQueryTimeout(2); // 2 seconds timeout

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String itemsJson = rs.getString("items");
                    double money = rs.getDouble("money");
                    ItemStack[] items = deserializeItems(itemsJson);
                    def = new RewardDefinition(day, rank, items, money);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[DailyRewards] Error loading reward definition: " + e.getMessage());
            if (debuggingFlag == 1) {
                e.printStackTrace();
            }
            // Continue with default definition
        }

        // If not found in DB, create a default definition
        if (def == null) {
            def = new RewardDefinition(day, rank, new ItemStack[26], 0.0);

            // Schedule asynchronous save of default definition
            final RewardDefinition finalDef = def;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    if (debuggingFlag == 1) {
                        plugin.getLogger().info("[DailyRewards] Saving default definition for day " + day + ", rank " + rank);
                    }
                    saveRewardDefinitionDirect(finalDef);
                } catch (Exception e) {
                    plugin.getLogger().severe("[DailyRewards] Error saving default definition: " + e.getMessage());
                    if (debuggingFlag == 1) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // Add to cache regardless of whether it was loaded or created
        cache.put(cacheKey, def);
        return def;
    }

    public void saveRewardDefinition(RewardDefinition def) {
        // Update cache immediately
        String cacheKey = getCacheKey(def.getDay(), def.getRank());
        cache.put(cacheKey, def);

        // Save to database (this may be called from async thread already)
        if (Bukkit.isPrimaryThread()) {
            // If on main thread, save asynchronously
            saveRewardDefinitionAsync(def);
        } else {
            // If already on async thread, save directly
            saveRewardDefinitionDirect(def);
        }
    }

    private void saveRewardDefinitionAsync(RewardDefinition def) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            saveRewardDefinitionDirect(def);
        });
    }

    private void saveRewardDefinitionDirect(RewardDefinition def) {
        if (debuggingFlag == 1) {
            plugin.getLogger().info("[DailyRewards] Saving reward definition for day " +
                    def.getDay() + ", rank " + def.getRank());
        }

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "REPLACE INTO daily_rewards_config (day, rank, items, money) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, def.getDay());
            ps.setString(2, def.getRank());
            ps.setString(3, serializeItems(def.getItems()));
            ps.setDouble(4, def.getMoney());
            ps.executeUpdate();

            if (debuggingFlag == 1) {
                plugin.getLogger().info("[DailyRewards] Saved reward definition successfully");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[DailyRewards] Error saving reward definition: " + e.getMessage());
            if (debuggingFlag == 1) {
                e.printStackTrace();
            }
        }
    }

    private String serializeItems(ItemStack[] items) {
        List<String> serialized = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null) {
                    serialized.add(ItemStackSerializer.serialize(item));
                } else {
                    serialized.add(null); // Keep null items to maintain positions
                }
            }
        }
        return gson.toJson(serialized);
    }

    private ItemStack[] deserializeItems(String json) {
        try {
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> list = gson.fromJson(json, listType);
            ItemStack[] items = new ItemStack[26]; // Fixed size array

            if (list != null) {
                for (int i = 0; i < list.size() && i < 26; i++) {
                    String s = list.get(i);
                    if (s != null) {
                        items[i] = ItemStackSerializer.deserialize(s);
                    }
                }
            }
            return items;
        } catch (Exception e) {
            plugin.getLogger().severe("[DailyRewards] Error deserializing items: " + e.getMessage());
            if (debuggingFlag == 1) {
                e.printStackTrace();
            }
            return new ItemStack[26];
        }
    }

    // Clear the cache when needed
    public void clearCache() {
        cache.clear();
        if (debuggingFlag == 1) {
            plugin.getLogger().info("[DailyRewards] Reward definition cache cleared");
        }
    }
}
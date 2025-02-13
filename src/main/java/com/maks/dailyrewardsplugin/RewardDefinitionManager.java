package com.maks.dailyrewardsplugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RewardDefinitionManager {
    private DailyRewardsPlugin plugin;
    private Gson gson = new Gson();

    public RewardDefinitionManager(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    public RewardDefinition getRewardDefinition(int day, String rank) {
        try {
            Connection connection = plugin.getDatabaseManager().getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM daily_rewards_config WHERE day = ? AND rank = ?");
            ps.setInt(1, day);
            ps.setString(2, rank);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String itemsJson = rs.getString("items");
                double money = rs.getDouble("money");
                ItemStack[] items = deserializeItems(itemsJson);
                return new RewardDefinition(day, rank, items, money);
            } else {
                RewardDefinition def = new RewardDefinition(day, rank, new ItemStack[0], 0.0);
                saveRewardDefinition(def);
                return def;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveRewardDefinition(RewardDefinition def) {
        try {
            Connection connection = plugin.getDatabaseManager().getConnection();
            PreparedStatement ps = connection.prepareStatement("REPLACE INTO daily_rewards_config (day, rank, items, money) VALUES (?, ?, ?, ?)");
            ps.setInt(1, def.getDay());
            ps.setString(2, def.getRank());
            ps.setString(3, serializeItems(def.getItems()));
            ps.setDouble(4, def.getMoney());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String serializeItems(ItemStack[] items) {
        List<String> serialized = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null) {
                    serialized.add(ItemStackSerializer.serialize(item));
                }
            }
        }
        return gson.toJson(serialized);
    }

    private ItemStack[] deserializeItems(String json) {
        try {
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> list = gson.fromJson(json, listType);
            List<ItemStack> items = new ArrayList<>();
            if (list != null) {
                for (String s : list) {
                    ItemStack item = ItemStackSerializer.deserialize(s);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
            return items.toArray(new ItemStack[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ItemStack[0];
    }
}

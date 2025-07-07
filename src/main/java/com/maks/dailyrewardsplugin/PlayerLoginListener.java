package com.maks.dailyrewardsplugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import java.time.LocalDate;

public class PlayerLoginListener implements Listener {
    private DailyRewardsPlugin plugin;
    private Gson gson = new Gson();

    public PlayerLoginListener(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Updates the player's rank history JSON
     * @param rankHistoryJson The current rank history JSON
     * @param date The date to record the rank for
     * @param isPremium Whether the player has premium rank
     * @param isDeluxe Whether the player has deluxe rank
     * @return The updated rank history JSON
     */
    private String updateRankHistory(String rankHistoryJson, LocalDate date, boolean isPremium, boolean isDeluxe) {
        JsonObject jsonObject;

        if (rankHistoryJson == null || rankHistoryJson.isEmpty()) {
            jsonObject = new JsonObject();
        } else {
            try {
                jsonObject = JsonParser.parseString(rankHistoryJson).getAsJsonObject();
            } catch (Exception e) {
                e.printStackTrace();
                jsonObject = new JsonObject();
            }
        }

        String dateKey = date.toString();
        JsonObject rankObject = new JsonObject();
        rankObject.addProperty("premium", isPremium);
        rankObject.addProperty("deluxe", isDeluxe);

        jsonObject.add(dateKey, rankObject);
        return gson.toJson(jsonObject);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);
        LocalDate today = LocalDate.now();

        // Check player's permissions
        boolean isPremium = player.hasPermission("daily.premium");
        boolean isDeluxe = player.hasPermission("daily.deluxe");

        plugin.getLogger().info("[DailyRewards] Player " + player.getName() + " logged in with permissions - Premium: " + isPremium + ", Deluxe: " + isDeluxe);

        // Update rank history
        String updatedRankHistory = updateRankHistory(data.getRankHistory(), today, isPremium, isDeluxe);
        data.setRankHistory(updatedRankHistory);

        plugin.getLogger().info("[DailyRewards] Updated rank history for " + player.getName() + " on " + today);

        if(data.getLastLoginDate() == null || !data.getLastLoginDate().isEqual(today)) {
            if(data.getLastLoginDate() != null && data.getLastLoginDate().isEqual(today.minusDays(1))) {
                int newStreak = data.getDailyStreak() < 28 ? data.getDailyStreak() + 1 : 1;
                data.setDailyStreak(newStreak);
            } else {
                data.setDailyStreak(1);
            }
            data.setClaimedRewards("{}");
            data.setLastLoginDate(today);
            plugin.getDatabaseManager().updatePlayerData(data);
        } else {
            // Even if they've already logged in today, update their rank history
            plugin.getDatabaseManager().updatePlayerData(data);
        }

        player.sendMessage("§a§lWelcome! §7Your daily check-in has been registered. Your current streak is §e"
                + data.getDailyStreak() + "§7.");
    }
}

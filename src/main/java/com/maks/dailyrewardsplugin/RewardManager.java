package com.maks.dailyrewardsplugin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class RewardManager {
    private DailyRewardsPlugin plugin;
    private Economy economy;
    private Gson gson = new Gson();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Checks if a player had a specific rank on a specific day
     * @param rankHistoryJson The player's rank history JSON
     * @param date The date to check
     * @param rank The rank to check for ("premium" or "deluxe")
     * @return true if the player had the rank on that day, false otherwise
     */
    private boolean hadRankOnDay(String rankHistoryJson, LocalDate date, String rank) {
        if (rankHistoryJson == null || rankHistoryJson.isEmpty()) {
            plugin.getLogger().info("[DailyRewards] No rank history found for date " + date);
            return false;
        }

        try {
            JsonObject jsonObject = JsonParser.parseString(rankHistoryJson).getAsJsonObject();
            String dateKey = date.format(dateFormatter);

            plugin.getLogger().info("[DailyRewards] Checking rank '" + rank + "' for date " + dateKey);

            if (jsonObject.has(dateKey)) {
                JsonElement dateElement = jsonObject.get(dateKey);
                if (dateElement.isJsonObject()) {
                    JsonObject rankObject = dateElement.getAsJsonObject();
                    if (rankObject.has(rank)) {
                        boolean hasRank = rankObject.get(rank).getAsBoolean();
                        plugin.getLogger().info("[DailyRewards] Player had rank '" + rank + "' on " + dateKey + ": " + hasRank);
                        return hasRank;
                    } else {
                        plugin.getLogger().info("[DailyRewards] Rank '" + rank + "' not found in rank history for " + dateKey);
                    }
                } else {
                    plugin.getLogger().info("[DailyRewards] Invalid rank data format for " + dateKey);
                }
            } else {
                plugin.getLogger().info("[DailyRewards] No rank history entry for date " + dateKey);
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("[DailyRewards] Error checking rank history: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public RewardManager(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
        if(Bukkit.getPluginManager().getPlugin("Vault") != null) {
            this.economy = plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        }
    }

    public void giveRewards(Player player, int day) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId().toString());
        int currentDay = data.getDailyStreak();
        if(day > currentDay) {
            player.sendMessage("§cThis reward is not available yet! You are on day §e" + currentDay + "§c.");
            return;
        }

        // Check if reward is already claimed using proper JSON parsing
        if(hasClaimedReward(data.getClaimedRewards(), day)) {
            player.sendMessage("§cYou have already claimed the reward for day §e" + day + "§c!");
            return;
        }

        RewardDefinitionManager rdm = plugin.getRewardDefinitionManager();
        RewardDefinition normalDef = rdm.getRewardDefinition(day, "normal");
        RewardDefinition premiumDef = rdm.getRewardDefinition(day, "premium");
        RewardDefinition deluxeDef = rdm.getRewardDefinition(day, "deluxe");

        // Give items with inventory space check
        boolean allItemsGiven = true;

        if(normalDef != null && normalDef.getItems() != null) {
            allItemsGiven = giveItems(player, normalDef.getItems()) && allItemsGiven;
        }

        // Get the date for the day being claimed
        LocalDate claimDate = data.getLastLoginDate().minusDays(currentDay - day);

        // Check if player had premium/deluxe rank on the day they're claiming for
        boolean hadPremium = hadRankOnDay(data.getRankHistory(), claimDate, "premium");
        boolean hadDeluxe = hadRankOnDay(data.getRankHistory(), claimDate, "deluxe");

        plugin.getLogger().info("[DailyRewards] Player " + player.getName() + " claiming day " + day + " rewards");
        plugin.getLogger().info("[DailyRewards] Claim date: " + claimDate);
        plugin.getLogger().info("[DailyRewards] Had premium: " + hadPremium + ", Had deluxe: " + hadDeluxe);

        // Also log current permissions for comparison
        boolean currentPremium = player.hasPermission("daily.premium");
        boolean currentDeluxe = player.hasPermission("daily.deluxe");
        plugin.getLogger().info("[DailyRewards] Current premium: " + currentPremium + ", Current deluxe: " + currentDeluxe);

        if((hadPremium || hadDeluxe) && premiumDef != null && premiumDef.getItems() != null) {
            allItemsGiven = giveItems(player, premiumDef.getItems()) && allItemsGiven;
        }

        if(hadDeluxe && deluxeDef != null && deluxeDef.getItems() != null) {
            allItemsGiven = giveItems(player, deluxeDef.getItems()) && allItemsGiven;
        }

        if (!allItemsGiven) {
            player.sendMessage("§eWarning: Some items couldn't fit in your inventory and were dropped on the ground.");
        }

        double money = 0.0;
        if(normalDef != null) money += normalDef.getMoney();
        if((hadPremium || hadDeluxe) && premiumDef != null) money += premiumDef.getMoney();
        if(hadDeluxe && deluxeDef != null) money += deluxeDef.getMoney();

        if(economy != null && money > 0) {
            economy.depositPlayer(player, money);
            player.sendMessage("§aYou received §e$" + money + " §afrom your rewards!");
        }

        // Update claimed rewards using Gson
        String updatedClaimed = updateClaimedRewards(data.getClaimedRewards(), day);
        data.setClaimedRewards(updatedClaimed);
        plugin.getDatabaseManager().updatePlayerData(data);

        player.sendMessage("§aCongratulations! You have claimed the rewards for day §e" + day + "§a.");
    }

    /**
     * Gives items to a player, checking for inventory space
     * @return true if all items were added to inventory, false if some were dropped
     */
    private boolean giveItems(Player player, ItemStack[] items) {
        boolean allItemsAdded = true;
        for (ItemStack item : items) {
            if (item == null) continue;

            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                // Drop items that didn't fit
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                allItemsAdded = false;
            }
        }
        return allItemsAdded;
    }

    /**
     * Checks if the player has claimed a reward for a specific day
     */
    private boolean hasClaimedReward(String claimedRewardsJson, int day) {
        if (claimedRewardsJson == null || claimedRewardsJson.isEmpty()) {
            return false;
        }

        try {
            JsonObject jsonObject = JsonParser.parseString(claimedRewardsJson).getAsJsonObject();
            String dayKey = "day_" + day;
            return jsonObject.has(dayKey) && jsonObject.get(dayKey).getAsBoolean();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the claimed rewards JSON properly using Gson
     */
    private String updateClaimedRewards(String claimedRewardsJson, int day) {
        JsonObject jsonObject;

        if (claimedRewardsJson == null || claimedRewardsJson.isEmpty()) {
            jsonObject = new JsonObject();
        } else {
            try {
                jsonObject = JsonParser.parseString(claimedRewardsJson).getAsJsonObject();
            } catch (Exception e) {
                e.printStackTrace();
                jsonObject = new JsonObject();
            }
        }

        jsonObject.addProperty("day_" + day, true);
        return gson.toJson(jsonObject);
    }
}

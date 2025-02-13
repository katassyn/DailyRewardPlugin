package com.maks.dailyrewardsplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RewardManager {
    private DailyRewardsPlugin plugin;
    private Economy economy;

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
            player.sendMessage("This reward is not available yet!");
            return;
        }
        if(data.getClaimedRewards() != null && data.getClaimedRewards().contains("\"day_" + day + "\":true")) {
            player.sendMessage("You have already claimed the reward for day " + day + "!");
            return;
        }
        RewardDefinitionManager rdm = plugin.getRewardDefinitionManager();
        RewardDefinition normalDef = rdm.getRewardDefinition(day, "normal");
        RewardDefinition premiumDef = rdm.getRewardDefinition(day, "premium");
        RewardDefinition deluxeDef = rdm.getRewardDefinition(day, "deluxe");

        if(normalDef != null && normalDef.getItems() != null) {
            for(ItemStack item : normalDef.getItems()) {
                if(item != null)
                    player.getInventory().addItem(item);
            }
        }
        boolean isPremium = player.hasPermission("daily.premium");
        boolean isDeluxe = player.hasPermission("daily.deluxe");
        if((isPremium || isDeluxe) && premiumDef != null && premiumDef.getItems() != null) {
            for(ItemStack item : premiumDef.getItems()) {
                if(item != null)
                    player.getInventory().addItem(item);
            }
        }
        if(isDeluxe && deluxeDef != null && deluxeDef.getItems() != null) {
            for(ItemStack item : deluxeDef.getItems()) {
                if(item != null)
                    player.getInventory().addItem(item);
            }
        }

        double money = 0.0;
        if(normalDef != null) money += normalDef.getMoney();
        if((isPremium || isDeluxe) && premiumDef != null) money += premiumDef.getMoney();
        if(isDeluxe && deluxeDef != null) money += deluxeDef.getMoney();

        if(economy != null && money > 0) {
            economy.depositPlayer(player, money);
            player.sendMessage("You received $" + money + "!");
        }

        String updatedClaimed = data.getClaimedRewards();
        if(updatedClaimed == null || updatedClaimed.isEmpty()) {
            updatedClaimed = "{}";
        }
        updatedClaimed = updatedClaimed.substring(0, updatedClaimed.length() - 1) +
                (updatedClaimed.length() > 2 ? "," : "") +
                "\"day_" + day + "\":true}";
        data.setClaimedRewards(updatedClaimed);
        plugin.getDatabaseManager().updatePlayerData(data);

        player.sendMessage("You have received the rewards for day " + day + "!");
    }
}

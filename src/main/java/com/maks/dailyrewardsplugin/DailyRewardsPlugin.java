package com.maks.dailyrewardsplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DailyRewardsPlugin extends JavaPlugin {

    private static DailyRewardsPlugin instance;
    private DatabaseManager databaseManager;
    private RewardManager rewardManager;
    private RewardDefinitionManager rewardDefinitionManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("DailyRewardsPlugin enabled!");

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();
        rewardDefinitionManager = new RewardDefinitionManager(this);
        rewardManager = new RewardManager(this);
        guiManager = new GUIManager(this);

        getCommand("daily").setExecutor(new DailyCommand(this));
        getCommand("editdaily").setExecutor(new EditDailyCommand(this));

        Bukkit.getPluginManager().registerEvents(new PlayerLoginListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DailyGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AdminGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MoneyEditListener(), this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerData data = databaseManager.getPlayerData(p.getUniqueId().toString());
                int day = data.getDailyStreak();
                if (data.getClaimedRewards() == null || !data.getClaimedRewards().contains("\"day_" + day + "\":true")) {
                    p.sendMessage("Reminder: You have unclaimed daily rewards. Use /daily to claim them!");
                }
            }
        }, 12000L, 12000L); // co 1200 ticków (1 minuta)

    }

    @Override
    public void onDisable() {
        getLogger().info("DailyRewardsPlugin disabled!");
        databaseManager.disconnect();
    }

    public static DailyRewardsPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public RewardDefinitionManager getRewardDefinitionManager() {
        return rewardDefinitionManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }
}

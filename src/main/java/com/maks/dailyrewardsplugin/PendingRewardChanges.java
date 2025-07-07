package com.maks.dailyrewardsplugin;

import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class PendingRewardChanges {
    private static final Map<String, RewardDefinition> pendingChanges = new HashMap<>();
    private static int debuggingFlag = 1; // Set to 0 in production

    private static String getKey(int day, String rank) {
        return day + ":" + rank;
    }

    public static void addPendingChange(RewardDefinition def) {
        String key = getKey(def.getDay(), def.getRank());
        pendingChanges.put(key, def);

        if (debuggingFlag == 1) {
            DailyRewardsPlugin.getInstance().getLogger().info(
                    "[DailyRewards] Added pending change for day " + def.getDay() +
                            ", rank " + def.getRank() + ". Total pending changes: " + pendingChanges.size());
        }
    }

    public static RewardDefinition getPendingChange(int day, String rank) {
        return pendingChanges.get(getKey(day, rank));
    }

    public static boolean hasPendingChanges() {
        return !pendingChanges.isEmpty();
    }

    public static int getPendingChangesCount() {
        return pendingChanges.size();
    }

    public static void commitAllChanges() {
        if (pendingChanges.isEmpty()) return;

        DailyRewardsPlugin plugin = DailyRewardsPlugin.getInstance();
        RewardDefinitionManager manager = plugin.getRewardDefinitionManager();

        if (debuggingFlag == 1) {
            plugin.getLogger().info(
                    "[DailyRewards] Committing " + pendingChanges.size() + " pending changes to database");
        }

        // Save asynchronously to avoid freezing the server
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            for (RewardDefinition def : pendingChanges.values()) {
                manager.saveRewardDefinition(def);
            }

            // Clear pending changes after saving
            pendingChanges.clear();

            if (debuggingFlag == 1) {
                plugin.getLogger().info("[DailyRewards] All pending changes committed successfully");
            }
        });
    }

    public static void clearPendingChanges() {
        pendingChanges.clear();

        if (debuggingFlag == 1) {
            DailyRewardsPlugin.getInstance().getLogger().info(
                    "[DailyRewards] Cleared all pending changes");
        }
    }
}
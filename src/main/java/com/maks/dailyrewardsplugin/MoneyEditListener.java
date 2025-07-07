package com.maks.dailyrewardsplugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.concurrent.ConcurrentHashMap;

public class MoneyEditListener implements Listener {
    private static ConcurrentHashMap<String, MoneyEditContext> editContexts = new ConcurrentHashMap<>();
    private static final String CANCEL_KEYWORD = "cancel";
    private int debuggingFlag = 1; // Set to 0 in production

    public static void registerEditor(String playerUUID, MoneyEditContext context) {
        editContexts.put(playerUUID, context);
    }

    public static boolean isPlayerEditing(String playerUUID) {
        return editContexts.containsKey(playerUUID);
    }

    public static void cancelEditing(String playerUUID) {
        editContexts.remove(playerUUID);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        if(editContexts.containsKey(uuid)) {
            event.setCancelled(true);
            String message = event.getMessage().trim();

            // Allow canceling the edit
            if(message.equalsIgnoreCase(CANCEL_KEYWORD)) {
                editContexts.remove(uuid);
                event.getPlayer().sendMessage(ChatColor.YELLOW + "Money edit canceled.");

                // Reopen the reward config GUI
                Player player = event.getPlayer();
                MoneyEditContext context = editContexts.get(uuid);

                DailyRewardsPlugin plugin = DailyRewardsPlugin.getInstance();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // Get current definition, preferring pending changes
                    RewardDefinition def = PendingRewardChanges.getPendingChange(context.getDay(), context.getRank());
                    if (def == null) {
                        def = plugin.getRewardDefinitionManager().getRewardDefinition(context.getDay(), context.getRank());
                    }

                    player.openInventory(plugin.getGuiManager().getRewardConfigGUI(
                            context.getDay(), context.getRank(), def));
                });
                return;
            }

            double newMoney;
            try {
                // Validate the money value
                newMoney = Double.parseDouble(message);
                if(newMoney < 0) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Please enter a positive number or '" + CANCEL_KEYWORD + "' to cancel.");
                    return;
                }
            } catch(NumberFormatException ex) {
                event.getPlayer().sendMessage(ChatColor.RED + "Invalid number format. Please enter a valid number or '" + CANCEL_KEYWORD + "' to cancel.");
                return;
            }

            MoneyEditContext context = editContexts.remove(uuid);
            DailyRewardsPlugin plugin = DailyRewardsPlugin.getInstance();

            // Update on the main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Get current definition, preferring pending changes
                RewardDefinition def = PendingRewardChanges.getPendingChange(context.getDay(), context.getRank());
                if (def == null) {
                    def = plugin.getRewardDefinitionManager().getRewardDefinition(context.getDay(), context.getRank());
                }

                // Update money value
                def.setMoney(newMoney);

                // Store as pending change
                PendingRewardChanges.addPendingChange(def);

                Player player = event.getPlayer();
                player.sendMessage(ChatColor.GREEN + "Money value updated to $" + newMoney +
                        " for day " + context.getDay() + " and rank " + context.getRank() +
                        ". Changes are pending until you save.");

                // Reopen the reward config GUI
                player.openInventory(plugin.getGuiManager().getRewardConfigGUI(
                        context.getDay(), context.getRank(), def));
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up if a player leaves while in edit mode
        String uuid = event.getPlayer().getUniqueId().toString();
        if(editContexts.containsKey(uuid)) {
            if (debuggingFlag == 1) {
                DailyRewardsPlugin.getInstance().getLogger().info(
                        "[DailyRewards] Player " + event.getPlayer().getName() +
                                " quit while editing money, cleaning up edit context");
            }
            editContexts.remove(uuid);
        }
    }
}
package com.maks.dailyrewardsplugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.util.concurrent.ConcurrentHashMap;

public class MoneyEditListener implements Listener {
    private static ConcurrentHashMap<String, MoneyEditContext> editContexts = new ConcurrentHashMap<>();

    public static void registerEditor(String playerUUID, MoneyEditContext context) {
        editContexts.put(playerUUID, context);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        if(editContexts.containsKey(uuid)) {
            event.setCancelled(true);
            String message = event.getMessage();
            double newMoney;
            try {
                newMoney = Double.parseDouble(message);
            } catch(NumberFormatException ex) {
                event.getPlayer().sendMessage(ChatColor.RED + "Invalid number format. Please enter a valid number.");
                return;
            }
            MoneyEditContext context = editContexts.remove(uuid);
            DailyRewardsPlugin plugin = DailyRewardsPlugin.getInstance();
            RewardDefinition def = plugin.getRewardDefinitionManager().getRewardDefinition(context.getDay(), context.getRank());
            def.setMoney(newMoney);
            plugin.getRewardDefinitionManager().saveRewardDefinition(def);
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.GREEN + "Money value updated to $" + newMoney + " for day " + context.getDay() + " and rank " + context.getRank() + ".");
        }
    }
}

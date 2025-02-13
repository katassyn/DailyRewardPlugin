package com.maks.dailyrewardsplugin;

import com.maks.dailyrewardsplugin.DailyRewardsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class DailyGUIListener implements Listener {
    private DailyRewardsPlugin plugin;

    public DailyGUIListener(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if(title.equals("Daily Rewards")) {
            event.setCancelled(true);
            if(event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
            if(displayName.startsWith("Day ")) {
                String[] parts = displayName.split(" ");
                try {
                    int day = Integer.parseInt(parts[1]);
                    plugin.getRewardManager().giveRewards(player, day);
                    player.closeInventory();
                } catch(NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Error reading day number.");
                }
            }
        }
    }
}

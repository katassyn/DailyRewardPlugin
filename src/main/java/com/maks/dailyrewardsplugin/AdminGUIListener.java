package com.maks.dailyrewardsplugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AdminGUIListener implements Listener {
    private DailyRewardsPlugin plugin;

    public AdminGUIListener(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        // GUI wyboru dnia do edycji
        if(title.equals("Select Day to Edit")) {
            event.setCancelled(true);
            if(event.getCurrentItem() == null) return;
            String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
            if(displayName.startsWith("Day ")) {
                String[] parts = displayName.split(" ");
                try {
                    int day = Integer.parseInt(parts[1]);
                    player.closeInventory();
                    player.openInventory(plugin.getGuiManager().getEditDailyGUI(day));
                } catch(NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Error reading day number.");
                }
            }
            return;
        }

        // GUI wyboru rangi (Edit Daily - Day <number>)
        if(title.startsWith("Edit Daily - Day ")) {
            event.setCancelled(true);
            if(event.getCurrentItem() == null) return;
            String rank = event.getCurrentItem().getItemMeta().getDisplayName().toLowerCase();
            String[] tokens = title.split(" ");
            if(tokens.length < 5) {
                player.sendMessage(ChatColor.RED + "Invalid title format.");
                return;
            }
            try {
                int day = Integer.parseInt(tokens[4]);
                RewardDefinition def = plugin.getRewardDefinitionManager().getRewardDefinition(day, rank);
                player.closeInventory();
                player.openInventory(plugin.getGuiManager().getRewardConfigGUI(day, rank, def));
            } catch(NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "Error reading day number.");
            }
            return;
        }

        // GUI konfiguracji nagrody ("Configure <rank> Reward - Day <number>")
        if(title.startsWith("Configure") && title.contains("Reward - Day")) {
            int slot = event.getSlot();
            if(slot == 26) {
                event.setCancelled(true);
                String[] tokens = title.split(" ");
                if(tokens.length < 6) {
                    player.sendMessage(ChatColor.RED + "Invalid title format.");
                    return;
                }
                try {
                    int day = Integer.parseInt(tokens[5]);
                    String rank = tokens[1].toLowerCase();
                    MoneyEditListener.registerEditor(player.getUniqueId().toString(), new MoneyEditContext(day, rank));
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "Please enter the new money value in chat.");
                } catch(NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Error reading day number.");
                }
            }
            // Dla pozostałych slotów nie anulujemy zdarzenia – admin może przenosić przedmioty
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if(title.startsWith("Configure") && title.contains("Reward - Day")) {
            String[] parts = title.split(" ");
            if(parts.length < 6) return;
            String rank = parts[1].toLowerCase();
            int day;
            try {
                day = Integer.parseInt(parts[5]);
            } catch(NumberFormatException ex) {
                return;
            }
            Inventory inv = event.getInventory();
            ItemStack[] items = new ItemStack[26];
            for (int i = 0; i < 26; i++) {
                items[i] = inv.getItem(i);
            }
            double money = 0.0;
            ItemStack moneyItem = inv.getItem(26);
            if(moneyItem != null && moneyItem.hasItemMeta() &&
                    moneyItem.getItemMeta().getDisplayName().startsWith("Money: ")) {
                try {
                    money = Double.parseDouble(moneyItem.getItemMeta().getDisplayName().replace("Money: ", ""));
                } catch(NumberFormatException ex) {
                    money = 0.0;
                }
            }
            RewardDefinition def = new RewardDefinition(day, rank, items, money);
            plugin.getRewardDefinitionManager().saveRewardDefinition(def);
            Player player = (Player) event.getPlayer();
            player.sendMessage(ChatColor.GREEN + "Reward definition for day " + day + " and rank " + rank + " has been saved.");
        }
    }
}

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
    private int debuggingFlag = 1; // Set to 0 in production

    public AdminGUIListener(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        String uuid = player.getUniqueId().toString();

        // GUI wyboru dnia do edycji
        if(title.equals("Select Day to Edit")) {
            event.setCancelled(true);
            if(event.getCurrentItem() == null) return;
            if(!event.getCurrentItem().hasItemMeta() || !event.getCurrentItem().getItemMeta().hasDisplayName()) return;

            String displayName = event.getCurrentItem().getItemMeta().getDisplayName();

            // Check for action buttons
            if(displayName.contains("Save All Changes")) {
                // Save all pending changes to database
                PendingRewardChanges.commitAllChanges();
                player.sendMessage(ChatColor.GREEN + "All changes have been saved to the database!");

                // Close and reopen the inventory to update buttons
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(plugin.getGuiManager().getEditDaysGUI());
                }, 2L);
                return;
            }

            if(displayName.contains("Cancel All Changes")) {
                // Discard all pending changes
                int count = PendingRewardChanges.getPendingChangesCount();
                PendingRewardChanges.clearPendingChanges();
                player.sendMessage(ChatColor.YELLOW + "Discarded " + count + " pending changes.");

                // Close and reopen the inventory to update buttons
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(plugin.getGuiManager().getEditDaysGUI());
                }, 2L);
                return;
            }

            // Handle day selection
            if(displayName.startsWith("Day ")) {
                String[] parts = displayName.split(" ");
                try {
                    // Make this final so it can be used in lambda
                    final int day = Integer.parseInt(parts[1]);
                    player.closeInventory();

                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (debuggingFlag == 1) plugin.getLogger().info("[DailyRewards] Opening edit GUI for day " + day);
                        player.openInventory(plugin.getGuiManager().getEditDailyGUI(day));
                    }, 2L);

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
            if(!event.getCurrentItem().hasItemMeta() || !event.getCurrentItem().getItemMeta().hasDisplayName()) return;

            String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
            String[] tokens = title.split(" ");
            if(tokens.length < 5) {
                player.sendMessage(ChatColor.RED + "Invalid title format.");
                return;
            }

            // Handle back button
            if(displayName.contains("Back to Day Selection")) {
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(plugin.getGuiManager().getEditDaysGUI());
                }, 2L);
                return;
            }

            // Handle rank selection
            try {
                // Make these final so they can be used in lambda
                final int day = Integer.parseInt(tokens[4]);
                final String rank = displayName.toLowerCase();

                // Get definitions - prefer pending changes if available
                RewardDefinition def = PendingRewardChanges.getPendingChange(day, rank);
                if (def == null) {
                    def = plugin.getRewardDefinitionManager().getRewardDefinition(day, rank);
                }

                // Must be final for lambda
                final RewardDefinition finalDef = def;

                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(plugin.getGuiManager().getRewardConfigGUI(day, rank, finalDef));
                }, 2L);
            } catch(NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "Error reading day number.");
            }
            return;
        }

        // GUI konfiguracji nagrody ("Configure <rank> Reward - Day <number>")
        if(title.startsWith("Configure") && title.contains("Reward - Day")) {
            String[] tokens = title.split(" ");
            if(tokens.length < 6) {
                player.sendMessage(ChatColor.RED + "Invalid title format.");
                return;
            }

            int day;
            try {
                day = Integer.parseInt(tokens[5]);
            } catch(NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "Error reading day number.");
                return;
            }

            String rank = tokens[1].toLowerCase();
            int slot = event.getSlot();

            // Handle action buttons
            if(slot == 31 && event.getCurrentItem() != null &&
                    event.getCurrentItem().getItemMeta().getDisplayName().contains("Accept Changes")) {
                // Accept button clicked
                event.setCancelled(true);

                // Create the reward definition from the current inventory
                saveCurrentInventoryAsPendingChange(player, event.getInventory(), day, rank);

                // Return to day selection
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(plugin.getGuiManager().getEditDaysGUI());
                }, 2L);
                return;
            }

            if(slot == 30 && event.getCurrentItem() != null &&
                    event.getCurrentItem().getItemMeta().getDisplayName().contains("Cancel")) {
                // Cancel button clicked
                event.setCancelled(true);

                // Return to day selection without saving
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(plugin.getGuiManager().getEditDaysGUI());
                }, 2L);
                return;
            }

            if(slot == 32 && event.getCurrentItem() != null &&
                    event.getCurrentItem().getItemMeta().getDisplayName().contains("Back")) {
                // Back button clicked
                event.setCancelled(true);

                // Need final copy for lambda
                final int finalDay = day;

                // Return to rank selection
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(plugin.getGuiManager().getEditDailyGUI(finalDay));
                }, 2L);
                return;
            }

            if(slot == 26) {
                // Money edit button
                event.setCancelled(true);

                // Check if player is already editing
                if(MoneyEditListener.isPlayerEditing(uuid)) {
                    player.sendMessage(ChatColor.RED + "You are already editing a money value. Please finish that first.");
                    return;
                }

                MoneyEditListener.registerEditor(uuid, new MoneyEditContext(day, rank));
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Please enter the new money value in chat (or type 'cancel' to cancel).");
                return;
            }

            // Don't cancel for the inventory slots where items can be placed
            if(slot < 26) {
                return; // Allow editing items in the first 26 slots
            }

            // Cancel clicks on all other buttons
            event.setCancelled(true);
            return;
        }
    }

    private void saveCurrentInventoryAsPendingChange(Player player, Inventory inv, int day, String rank) {
        // Extract items from inventory
        ItemStack[] items = new ItemStack[26];
        for (int i = 0; i < 26; i++) {
            items[i] = inv.getItem(i);
        }

        // Extract money value
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

        // Create and store the definition
        RewardDefinition def = new RewardDefinition(day, rank, items, money);
        PendingRewardChanges.addPendingChange(def);

        if (debuggingFlag == 1) {
            plugin.getLogger().info("[DailyRewards] Added pending change for day " + day + ", rank " + rank);
        }

        player.sendMessage(ChatColor.GREEN + "Changes for day " + day + " and rank " +
                rank + " have been queued for saving.");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // We no longer need this since we're using buttons to save changes
        // All inventory closing logic is now handled by the button clicks
    }
}
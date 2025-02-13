package com.maks.dailyrewardsplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GUIManager {
    private DailyRewardsPlugin plugin;

    public GUIManager(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    public Inventory getDailyGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "Daily Rewards");
        // Uzupełnij wszystkie sloty czarnymi szybami
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }
        // Definiujemy sloty dla 28 dni (ułożone tak, aby wyglądało estetycznie)
        int[] daySlots = {10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43};
        int currentDay = getPlayerCurrentDay(player);
        for (int i = 0; i < daySlots.length; i++) {
            int day = i + 1;
            ItemStack item;
            if (day < currentDay) {
                if(hasClaimed(player, day)) {
                    item = createGuiItem(Material.GRAY_WOOL, "Day " + day, "Claimed");
                } else {
                    item = createGuiItem(Material.YELLOW_WOOL, "Day " + day, "Available to claim");
                }
            } else if (day == currentDay) {
                if(hasClaimed(player, day)) {
                    item = createGuiItem(Material.GRAY_WOOL, "Day " + day, "Claimed");
                } else {
                    item = createGuiItem(Material.GREEN_WOOL, "Day " + day, "Today!");
                }
            } else {
                item = createGuiItem(Material.RED_WOOL, "Day " + day, "Not available yet");
            }
            inv.setItem(daySlots[i], item);
        }
        return inv;
    }

    public Inventory getEditDaysGUI() {
        Inventory inv = Bukkit.createInventory(null, 54, "Select Day to Edit");
        for (int i = 1; i <= 28; i++) {
            inv.addItem(createGuiItem(Material.PAPER, "Day " + i, "Click to edit"));
        }
        return inv;
    }

    public Inventory getEditDailyGUI(int day) {
        Inventory inv = Bukkit.createInventory(null, 27, "Edit Daily - Day " + day);
        inv.setItem(10, createGuiItem(Material.DIAMOND, "Normal", "Edit normal reward"));
        inv.setItem(13, createGuiItem(Material.GOLD_INGOT, "Premium", "Edit premium reward"));
        inv.setItem(16, createGuiItem(Material.EMERALD, "Deluxe", "Edit deluxe reward"));
        return inv;
    }

    public Inventory getRewardConfigGUI(int day, String rank, RewardDefinition def) {
        Inventory inv = Bukkit.createInventory(null, 27, "Configure " + rank + " Reward - Day " + day);
        if(def != null && def.getItems() != null) {
            ItemStack[] items = def.getItems();
            for (int i = 0; i < items.length && i < 26; i++) {
                inv.setItem(i, items[i]);
            }
        }
        inv.setItem(26, createGuiItem(Material.PAPER, "Money: " + def.getMoney(), "Click to change money value"));
        return inv;
    }

    private ItemStack createGuiItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(java.util.Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private int getPlayerCurrentDay(Player player) {
        PlayerData data = DailyRewardsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId().toString());
        return data.getDailyStreak();
    }

    private boolean hasClaimed(Player player, int day) {
        PlayerData data = DailyRewardsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId().toString());
        String claimed = data.getClaimedRewards();
        return claimed != null && claimed.contains("\"day_" + day + "\":true");
    }
}

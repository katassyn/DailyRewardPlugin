package com.maks.dailyrewardsplugin;

import org.bukkit.inventory.ItemStack;

public class RewardDefinition {
    private int day;
    private String rank;
    private ItemStack[] items;
    private double money;

    public RewardDefinition(int day, String rank, ItemStack[] items, double money) {
        this.day = day;
        this.rank = rank;
        this.items = items;
        this.money = money;
    }

    public int getDay() {
        return day;
    }

    public String getRank() {
        return rank;
    }

    public ItemStack[] getItems() {
        return items;
    }

    public void setItems(ItemStack[] items) {
        this.items = items;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }
}

package com.maks.dailyrewardsplugin;

public class MoneyEditContext {
    private int day;
    private String rank;

    public MoneyEditContext(int day, String rank) {
        this.day = day;
        this.rank = rank;
    }

    public int getDay() {
        return day;
    }

    public String getRank() {
        return rank;
    }
}

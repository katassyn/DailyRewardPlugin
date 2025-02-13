package com.maks.dailyrewardsplugin;

import java.time.LocalDate;

public class PlayerData {
    private String uuid;
    private int dailyStreak;
    private LocalDate lastLoginDate;
    private String claimedRewards;

    public PlayerData(String uuid, int dailyStreak, LocalDate lastLoginDate, String claimedRewards) {
        this.uuid = uuid;
        this.dailyStreak = dailyStreak;
        this.lastLoginDate = lastLoginDate;
        this.claimedRewards = claimedRewards;
    }

    public String getUuid() {
        return uuid;
    }

    public int getDailyStreak() {
        return dailyStreak;
    }

    public void setDailyStreak(int dailyStreak) {
        this.dailyStreak = dailyStreak;
    }

    public LocalDate getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(LocalDate lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public String getClaimedRewards() {
        return claimedRewards;
    }

    public void setClaimedRewards(String claimedRewards) {
        this.claimedRewards = claimedRewards;
    }
}

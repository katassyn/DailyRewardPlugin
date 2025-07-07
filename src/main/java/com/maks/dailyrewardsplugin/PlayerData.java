package com.maks.dailyrewardsplugin;

import java.time.LocalDate;

public class PlayerData {
    private String uuid;
    private int dailyStreak;
    private LocalDate lastLoginDate;
    private String claimedRewards;
    private String rankHistory;

    public PlayerData(String uuid, int dailyStreak, LocalDate lastLoginDate, String claimedRewards) {
        this(uuid, dailyStreak, lastLoginDate, claimedRewards, "{}");
    }

    public PlayerData(String uuid, int dailyStreak, LocalDate lastLoginDate, String claimedRewards, String rankHistory) {
        this.uuid = uuid;
        this.dailyStreak = dailyStreak;
        this.lastLoginDate = lastLoginDate;
        this.claimedRewards = claimedRewards;
        this.rankHistory = rankHistory;
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

    public String getRankHistory() {
        return rankHistory;
    }

    public void setRankHistory(String rankHistory) {
        this.rankHistory = rankHistory;
    }
}

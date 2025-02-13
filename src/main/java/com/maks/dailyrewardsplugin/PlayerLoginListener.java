package com.maks.dailyrewardsplugin;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import java.time.LocalDate;

public class PlayerLoginListener implements Listener {
    private DailyRewardsPlugin plugin;

    public PlayerLoginListener(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);
        LocalDate today = LocalDate.now();

        if(data.getLastLoginDate() == null || !data.getLastLoginDate().isEqual(today)) {
            if(data.getLastLoginDate() != null && data.getLastLoginDate().isEqual(today.minusDays(1))) {
                int newStreak = data.getDailyStreak() < 28 ? data.getDailyStreak() + 1 : 1;
                data.setDailyStreak(newStreak);
            } else {
                data.setDailyStreak(1);
            }
            data.setClaimedRewards("{}");
            data.setLastLoginDate(today);
            plugin.getDatabaseManager().updatePlayerData(data);
        }
        player.sendMessage("Welcome! Your daily check-in has been registered.");
    }
}

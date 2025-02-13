package com.maks.dailyrewardsplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class EditDailyCommand implements CommandExecutor {
    private DailyRewardsPlugin plugin;

    public EditDailyCommand(DailyRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;
        if(!player.hasPermission("daily.edit")) {
            player.sendMessage(ChatColor.RED + "You do not have permission!");
            return true;
        }
        player.openInventory(plugin.getGuiManager().getEditDaysGUI());
        return true;
    }
}

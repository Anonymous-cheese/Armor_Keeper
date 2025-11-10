package com.toasthax.lapisportalsplus.commands;

import com.toasthax.lapisportalsplus.LapisPortalsPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LppCommand implements CommandExecutor {
    private final LapisPortalsPlus plugin;
    public LppCommand(LapisPortalsPlus plugin){ this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("lapisportalsplus.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§7Usage: /lpp debug [on|off]  (debug is currently " + (plugin.isDebug() ? "§aON" : "§cOFF") + "§7)");
            return true;
        }
        if (args[0].equalsIgnoreCase("debug")) {
            if (args.length == 1) {
                plugin.setDebug(!plugin.isDebug());
            } else {
                boolean on = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
                plugin.setDebug(on);
            }
            sender.sendMessage("§7[LPP] Debug is now " + (plugin.isDebug() ? "§aON" : "§cOFF"));
            return true;
        }
        sender.sendMessage("§7Unknown subcommand.");
        return true;
    }
}

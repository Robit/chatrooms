package io.github.rm2023.chatrooms;

import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;

public class Util {
    public static boolean sendMessage(CommandSender p, String message) {
        if (p == null) {
            return false;
        }
        p.sendMessage(ChatColor.AQUA + "[Chatrooms] " + message);
        return true;
    }

    public static boolean sendError(CommandSender p, String message) {
        if (p == null) {
            return false;
        }
        p.sendMessage(ChatColor.RED + "[Chatrooms] " + message);
        return true;
    }
}

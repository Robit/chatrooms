package io.github.rm2023.chatrooms;

import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;

public class Util {
    public static boolean sendMessage(CommandSender p, String message) {
        p.sendMessage(ChatColor.AQUA + "[Chatrooms] " + message);
        return true;
    }
}

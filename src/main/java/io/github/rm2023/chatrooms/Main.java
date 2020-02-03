package io.github.rm2023.chatrooms;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.PlayerArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class Main extends JavaPlugin {

    static public Main plugin;
    int MAX_CHATROOMS;

    @Override
    public void onLoad() {
        plugin = this;
        // Initialize ChatRooms from database
        ChatRoom.initialize();
        // Register permissions
        getServer().getPluginManager().addPermission(new Permission("chatrooms.user", "Allows users to join, leave, and chat in chatrooms"));
        getServer().getPluginManager().addPermission(new Permission("chatrooms.manage", " A manager can create, delete, and manage their own chatroom"));
        getServer().getPluginManager().addPermission(new Permission("chatrooms.unlimited", "Allows chatroom managers to make unlimited chatrooms"));
        getServer().getPluginManager().addPermission(new Permission("chatrooms.admin", "An admin can create, delete, and manage everyone's chatroom, and also has the ability to \"spy\" on any chatroom."));
        // Load config
        File configFile = new File(Main.plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().log(Level.SEVERE, "Error loading ChatRooms config!");
            e.printStackTrace();
            return;
        }
        MAX_CHATROOMS = config.getInt("max_chatrooms");

        // Register commands
        DynamicSuggestedStringArgument chatRoomName = new DynamicSuggestedStringArgument((sender) -> {
            List<ChatRoom> rooms;
            if (!(sender instanceof Player) || sender.hasPermission("chatrooms.admin")) {
                rooms = ChatRoom.getRooms();
            } else {
                rooms = ChatRoom.getRooms((Player) sender);
            }
            LinkedList<String> result = new LinkedList<String>();
            for (ChatRoom room : rooms) {
                result.add(room.getName());
            }
            return result.toArray(new String[] {});
        });

        LinkedHashMap<String, Argument> arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("create").withPermission(CommandPermission.fromString("chatrooms.manage")));
        arguments.put("name", new StringArgument());
        arguments.put("password", new StringArgument());
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            String name = (String) args[0];
            String password = (String) args[1];
            if(!(sender instanceof Player)) {
                Util.sendError(sender, "This command can only be executed by a player!");
                return;
            }
            if(sender.hasPermission(new Permission("chatrooms.unlimited")) || ChatRoom.getRooms((Player) sender).size() < MAX_CHATROOMS) {
                Util.sendError(sender, "You can only create up to " + MAX_CHATROOMS + " rooms!");
                return;
            }
            if (ChatRoom.getRoom(name) != null || name.equals("GLOBAL")) {
                Util.sendError(sender, "A chatroom already exists with this name!");
                return;
            }
            new ChatRoom(name, (Player) sender, password);
            Util.sendMessage(sender, "Chatroom created.");
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("delete").withPermission(CommandPermission.fromString("chatrooms.manage")));
        arguments.put("name", chatRoomName);
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            String name = (String) args[0];
            ChatRoom toDelete = ChatRoom.getRoom(name);
            if (toDelete == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            if (sender.hasPermission("chatrooms.admin") || toDelete.getOwner().equals((Player) sender)) {
                Util.sendMessage(sender, "Chatroom deleted.");
                toDelete.remove();
                return;
            }
            Util.sendError(sender, "Only the owner of the room can delete it!");
        });
        
        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("setPassword").withPermission(CommandPermission.fromString("chatrooms.manage")));
        arguments.put("name", chatRoomName);
        arguments.put("password", new StringArgument());
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            String name = (String) args[0];
            String password = (String) args[1];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            if (sender.hasPermission("chatrooms.admin") || toChange.getOwner().equals((Player) sender)) {
                Util.sendMessage(sender, "Password changed.");
                toChange.setPassword(password);
                return;
            }
            Util.sendError(sender, "Only the owner of the room can change the password!");
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("invite").withPermission(CommandPermission.fromString("chatrooms.manage")));
        arguments.put("name", chatRoomName);
        arguments.put("player", new PlayerArgument());
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            String name = (String) args[0];
            Player toInvite = (Player) args[1];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            if (sender.hasPermission("chatrooms.admin") || toChange.getOwner().equals((Player) sender)) {
                if (toInvite.hasPermission("chatrooms.user")) {
                    Util.sendError(sender, "This player doesn't have permission to join rooms!");
                    return;
                }
                if (!toChange.invitePlayer(toInvite)) {
                    Util.sendError(sender, "An error occured!");
                }
                Util.sendMessage(sender, "Player invited. They need to do /cr join " + toChange.getName() + " to join.");
                return;
            }
            Util.sendError(sender, "Only the owner of the room can invite players!");
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("kick").withPermission(CommandPermission.fromString("chatrooms.manage")));
        arguments.put("name", chatRoomName);
        arguments.put("player", new PlayerArgument());
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            String name = (String) args[0];
            Player toKick = (Player) args[1];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            if (sender.hasPermission("chatrooms.admin") || toChange.getOwner().equals((Player) sender)) {
                if (!toChange.removePlayer(toKick)) {
                    Util.sendError(sender, "An error occured!");
                }
                return;
            }
            Util.sendError(sender, "Only the owner of the room can kick players!");
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("kick").withPermission(CommandPermission.fromString("chatrooms.manage")));
        arguments.put("name", chatRoomName);
        arguments.put("player", new TextArgument());
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            String name = (String) args[0];
            String toKick = (String) args[1];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            if (sender.hasPermission("chatrooms.admin") || toChange.getOwner().equals((Player) sender)) {
                if (!toChange.removePlayer(toKick)) {
                    Util.sendError(sender, "An error occured!");
                }
                return;
            }
            Util.sendError(sender, "Only the owner of the room can kick players!");
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("join"));
        arguments.put("name", new StringArgument());
        arguments.put("password", new StringArgument());
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            if (!(sender instanceof Player)) {
                Util.sendError(sender, "This command can only be executed by a player!");
                return;
            }
            String name = (String) args[0];
            String password = (String) args[1];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            if (password.equals(toChange.getPassword())) {
                if (!toChange.addPlayer((Player) sender)) {
                    Util.sendError(sender, "An error occured!");
                    return;
                }
                return;
            }
            Util.sendError(sender, "Incorrect Password!");
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("join"));
        arguments.put("name", new StringArgument());
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            if (!(sender instanceof Player)) {
                Util.sendError(sender, "This command can only be executed by a player!");
                return;
            }
            String name = (String) args[0];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            if (((Player) sender).hasPermission("chatrooms.admin") || toChange.isInvited((Player) sender)) {
                if (!toChange.addPlayer((Player) sender)) {
                    Util.sendError(sender, "An error occured!");
                    return;
                }
                return;
            }
            Util.sendError(sender, "You need a password to join this room!");
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("leave"));
        arguments.put("name", chatRoomName);
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            if(!(sender instanceof Player)) {
                Util.sendError(sender, "This command can only be executed by a player!");
                return;
            }
            String name = (String) args[0];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            if (!toChange.removePlayer((Player) sender)) {
                Util.sendError(sender, "An error occured!");
                return;
            }
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("channel"));
        arguments.put("name", chatRoomName);
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            if (!(sender instanceof Player)) {
                Util.sendError(sender, "This command can only be executed by a player!");
                return;
            }
            String name = (String) args[0];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            if (!toChange.addToChannel((Player) sender)) {
                Util.sendError(sender, "An error occured!");
                return;
            }
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("spy").withPermission(CommandPermission.fromString("chatrooms.admin")));
        arguments.put("name", chatRoomName);
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            if (!(sender instanceof Player)) {
                Util.sendError(sender, "This command can only be executed by a player!");
                return;
            }
            String name = (String) args[0];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            if (!toChange.addToChannel((Player) sender)) {
                Util.sendError(sender, "An error occured!");
                return;
            }
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("global"));
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            if (!(sender instanceof Player)) {
                Util.sendError(sender, "This command can only be executed by a player!");
                return;
            }
            for (ChatRoom room : ChatRoom.getRooms()) {
                room.removeFromChannel((Player) sender);
            }
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("owner"));
        arguments.put("name", chatRoomName);
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            String name = (String) args[0];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            Util.sendMessage(sender, toChange.getOwner().getName() + " is the owner of this chatroom.");
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("info"));
        arguments.put("name", chatRoomName);
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            String name = (String) args[0];
            ChatRoom toChange = ChatRoom.getRoom(name);
            if (toChange == null) {
                Util.sendError(sender, "This room doesn't exist!");
                return;
            }
            sender.sendMessage(toChange.getInfo());
        });

        arguments = new LinkedHashMap<String, Argument>();
        arguments.put("literal", new LiteralArgument("list"));
        arguments.put("player", new PlayerArgument().withPermission(CommandPermission.fromString("chatrooms.admin")));
        CommandAPI.getInstance().register("chatroom", CommandPermission.fromString("chatrooms.user"), new String[] { "cr" }, arguments, (sender, args) -> {
            Player p = null;
            if (sender instanceof Player) {
                p = (Player) sender;
            }
            if (args.length > 0) {
                p = (Player) args[0];
            }
            if (p == null) {
                Util.sendError(sender, "This command must be executed as a player!");
                return;
            }
            List<ChatRoom> rooms = ChatRoom.getRooms(p);
            if (rooms.isEmpty()) {
                Util.sendMessage(sender, "This user is not part of any chatrooms.");
            }
            for (ChatRoom room : rooms) {
                sender.sendMessage(room.getInfo());
                sender.sendMessage("");
            }
        });
    }

    public void onEnable() {

    }
}

package io.github.rm2023.chatrooms;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.md_5.bungee.api.ChatColor;

public class ChatRoom implements Listener {
    protected static LinkedList<ChatRoom> rooms = new LinkedList<ChatRoom>();
    protected static FileConfiguration data;
    protected static File dataFile;

    protected String name;
    protected OfflinePlayer owner;
    protected String password;
    protected LinkedList<OfflinePlayer> members = new LinkedList<OfflinePlayer>();
    protected LinkedList<Player> invited = new LinkedList<Player>();
    protected LinkedList<Player> inChannel = new LinkedList<Player>();

    public static void initialize() {
        dataFile = new File(Main.plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            Main.plugin.saveResource("data.yml", false);
        }

        data = new YamlConfiguration();
        try {
            data.load(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            Main.plugin.getLogger().log(Level.SEVERE, "Error loading RankToken information!");
            e.printStackTrace();
            return;
        }

        for (String key : data.getKeys(false)) {
            new ChatRoom(data.getConfigurationSection(key));
        }
    }

    public static void save() {
        for (ChatRoom room : rooms) {
            ConfigurationSection roomData = data.getConfigurationSection(room.getName());
            if (roomData == null) {
                roomData = data.createSection(room.getName());
            }
            roomData.set("name", room.name);
            roomData.set("password", room.password);
            roomData.set("owner", room.owner);
            roomData.set("members", room.members);
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ChatRoom getRoom(String name) {
        for (ChatRoom room : rooms) {
            if (room.getName().equals(name)) {
                return room;
            }
        }
        return null;
    }
    public static List<ChatRoom> getRooms() {
        return rooms;
    }

    public static List<ChatRoom> getRooms(Player p) {
        LinkedList<ChatRoom> result = new LinkedList<ChatRoom>();
        for (ChatRoom room : rooms) {
            if (room.owner.equals(p) || room.members.contains(p)) {
                result.add(room);
            }
        }
        return result;
    }

    public ChatRoom(String name, Player owner, String password) {
        this.name = name;
        this.owner = owner;
        this.password = password;
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
        rooms.add(this);
        save();
        Main.plugin.getLogger().info("Chatroom " + name + " has been created by " + owner.getName() + ".");
    }

    public ChatRoom(ConfigurationSection data) {
        this.name = data.getString("name");
        this.owner = data.getOfflinePlayer("owner");
        this.members = (LinkedList<OfflinePlayer>) data.getList("members");
        this.password = data.getString("password");
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
        rooms.add(this);
    }

    public void remove() {
        rooms.remove(this);
        HandlerList.unregisterAll(this);
        Main.plugin.getLogger().info("Chatroom " + name + " has been deleted.");
        save();
    }

    public void sendChat(String message) {
        for (OfflinePlayer p : members) {
            if (p.isOnline()) {
                p.getPlayer().sendMessage(ChatColor.AQUA + message);
                Main.plugin.getLogger().info("[Chatroom " + name + "] " + message);
            }
        }
        for (Player p : inChannel) {
            if (p.isOnline() && !members.contains(p)) {
                p.sendMessage(ChatColor.AQUA + message);
                Main.plugin.getLogger().info("[Chatroom " + name + "] " + message);
            }
        }
    }

    public boolean invitePlayer(Player p) {
        if (p.equals(owner) || members.contains(p) || invited.contains(p)) {
            return false;
        }
        invited.add(p);
        Util.sendMessage(p, "You have been invited to chatroom " + name + ". Do /cr join " + name + " to accept.");
        return true;
    }

    public boolean addPlayer(Player p) {
        invited.remove(p);
        if (members.contains(p)) {
            return false;
        }
        if (members.add(p)) {
            Util.sendMessage(p, "You are now a member of chatroom " + name + ".");
            sendChat(p.getName() + " has entered the chatroom.");
            save();
            return true;
        }
        return false;
    }

    public boolean addToChannel(Player p) {
        if (!p.isOnline()) {
            return false;
        }
        for(ChatRoom room : rooms) {
            room.removeFromChannel(p);
        }
        Util.sendMessage(p, "You are now in channel " + getName() + ".");
        return inChannel.add(p);
    }

    public boolean removeFromChannel(OfflinePlayer p) {
        if (p.isOnline()) {
            Util.sendMessage(p.getPlayer(), "You are no longer in channel " + getName() + ".");
        }
        return inChannel.remove(p);
    }

    public boolean removePlayer(OfflinePlayer p) {
        if (members.remove(p)) {
            removeFromChannel(p);
            if (p.isOnline()) {
                Util.sendMessage(p.getPlayer(), "You are no longer a member of chatroom " + name + ".");
            }
            sendChat(p.getName() + " has left the chatroom.");
            save();
            return true;
        }
        return false;
    }

    public boolean removePlayer(String s) {
        for (OfflinePlayer p : members) {
            if (p.getName().equals(s)) {
                return removePlayer(p);
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        save();
    }

    public OfflinePlayer getOwner() {
        return owner;
    }

    public String[] getInfo() {
        String[] result = new String[3];
        result[0] = ChatColor.AQUA + "Name: " + ChatColor.DARK_BLUE + name;
        result[1] = ChatColor.AQUA + "Owner: " + ChatColor.DARK_BLUE + owner.getName();
        result[2] = ChatColor.AQUA + "Members: " + ChatColor.DARK_BLUE + owner.getName();
        for (OfflinePlayer member : members) {
            result[2] += ", " + member.getName();
        }
        return result;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (inChannel.contains(event.getPlayer())) {
            event.setCancelled(true);
            sendChat(event.getMessage());
        }
    }

    @EventHandler
    public void onPlayerLogout(PlayerQuitEvent event) {
        inChannel.remove(event.getPlayer());
    }
}

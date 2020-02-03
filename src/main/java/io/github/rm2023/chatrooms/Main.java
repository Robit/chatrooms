package io.github.rm2023.chatrooms;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    static public Main plugin;

    @Override
    public void onLoad() {
        plugin = this;
        ChatRoom.initialize();

    }

    public void onEnable() {
        // HEY! LISTEN!
    }
}

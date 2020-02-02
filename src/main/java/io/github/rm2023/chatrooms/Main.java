package io.github.rm2023.chatrooms;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    static public Main plugin;

    @Override
    public void onLoad() {
        // Construct data backend
        plugin = this;

    }

    public void onEnable() {
        // HEY! LISTEN!
    }
}

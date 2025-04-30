package net.glassmc.mapartcopyright;

import net.glassmc.mapartcopyright.commands.MapArtCommand;
import net.glassmc.mapartcopyright.commands.MapArtTabCompleter;
import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import net.glassmc.mapartcopyright.economy.EconomyHandler;
import net.glassmc.mapartcopyright.listeners.ChatInputListener;
import net.glassmc.mapartcopyright.listeners.MapArtMenuListener;
import net.glassmc.mapartcopyright.listeners.MapFrameListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MapArtCopyright extends JavaPlugin {

    private static MapArtCopyright instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("MapArtCopyright plugin enabled.");

        getCommand("mapart").setExecutor(new MapArtCommand());
        getCommand("mapart").setTabCompleter(new MapArtTabCompleter());

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MapFrameListener(), this);
        pm.registerEvents(new MapArtMenuListener(), this);
        pm.registerEvents(new ChatInputListener(), this);

        // Vault economy setup
        if (!EconomyHandler.setup()) {
            getLogger().warning("Vault not found or no economy provider detected.");
        }

        // Ownership DB setup
        OwnershipDatabase.connect();
    }

    @Override
    public void onDisable() {
        getLogger().info("MapArtCopyright plugin disabled.");
    }

    public static MapArtCopyright getInstance() {
        return instance;
    }
}

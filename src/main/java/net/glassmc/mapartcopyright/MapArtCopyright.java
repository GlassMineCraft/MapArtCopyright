package net.glassmc.mapartcopyright;

import net.glassmc.mapartcopyright.commands.MapArtCommand;
import net.glassmc.mapartcopyright.commands.MapArtTabCompleter;
import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import net.glassmc.mapartcopyright.economy.EconomyHandler;
import net.glassmc.mapartcopyright.listeners.ChatInputListener;
import net.glassmc.mapartcopyright.listeners.MapArtMenuListener;
import net.glassmc.mapartcopyright.listeners.MapFrameListener;
import net.glassmc.mapartcopyright.listeners.MapInteractionListener;
import net.glassmc.mapartcopyright.listeners.AnvilRenameListener;
import net.glassmc.mapartcopyright.listeners.MapDropListener;
import net.glassmc.mapartcopyright.metrics.Metrics;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MapArtCopyright extends JavaPlugin {

    private static MapArtCopyright instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        OwnershipDatabase.connect();

        int pluginId = getConfig().getInt("metrics.plugin-id", 0);
        if (pluginId > 0) {
            new Metrics(this, pluginId);
            getLogger().info("bStats metrics enabled (plugin ID: " + pluginId + ").");
        }
        
        getLogger().info("MapArtCopyright plugin enabled.");

        getCommand("mapart").setExecutor(new MapArtCommand());
        getCommand("mapart").setTabCompleter(new MapArtTabCompleter());

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MapFrameListener(), this);
        pm.registerEvents(new MapArtMenuListener(), this);
        pm.registerEvents(new ChatInputListener(), this);
        pm.registerEvents(new MapInteractionListener(), this);
        pm.registerEvents(new AnvilRenameListener(), this);
        pm.registerEvents(new MapDropListener(), this);

        if (!EconomyHandler.setup()) {
            getLogger().warning("Vault not found or no economy provider detected.");
        }
        if (Bukkit.getPluginManager().getPlugin("CMILib") == null) {
            getLogger().warning("CMILib not found! Some features may not work properly.");
        } else {
            getLogger().info("CMILib detected and hooked.");
        }
    }

    @Override
    public void onDisable() {
        OwnershipDatabase.close();
        getLogger().info("MapArtCopyright plugin disabled.");
    }

    public static MapArtCopyright getInstance() {
        return instance;
    }
}
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
import net.glassmc.mapartcopyright.listeners.ArtworkSyncListener;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.ItemFrame;
import net.glassmc.mapartcopyright.util.InputManager;
import net.glassmc.mapartcopyright.util.HologramUtil;

public class MapArtCopyright extends JavaPlugin {

    private static MapArtCopyright instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        if (!OwnershipDatabase.connect()) {
            getLogger().severe("Ownership database is unavailable. Protection stays active; registered-map edits will be refused.");
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
        pm.registerEvents(new ArtworkSyncListener(), this);

        if (!EconomyHandler.setup()) {
            getLogger().warning("Vault not found or no economy provider detected.");
        }
        Bukkit.getScheduler().runTask(this, MapFrameListener::initializeLoadedFrames);
    }

    @Override
    public void onDisable() {
        InputManager.clearAll();
        for (var world : Bukkit.getWorlds()) {
            for (var entity : world.getEntities()) {
                if (entity instanceof ItemFrame frame) HologramUtil.remove(frame);
            }
        }
        OwnershipDatabase.close();
        getLogger().info("MapArtCopyright plugin disabled.");
    }

    public static MapArtCopyright getInstance() {
        return instance;
    }
}

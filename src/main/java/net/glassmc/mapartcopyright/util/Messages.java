package net.glassmc.mapartcopyright.util;

import net.glassmc.mapartcopyright.MapArtCopyright;
import org.bukkit.command.CommandSender;

public final class Messages {
    private Messages() {}
    public static void send(CommandSender target, String key, String fallback) { send(target, key, fallback, ""); }
    public static void send(CommandSender target, String key, String fallback, String input) {
        target.sendMessage(MapArtCopyright.getInstance().getConfig().getString("messages." + key, fallback).replace("{input}", input));
    }
}

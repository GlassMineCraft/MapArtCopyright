package net.glassmc.mapartcopyright.util;

import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class PermissionUtil {
    private PermissionUtil() {}
    public static boolean canModify(Player player, ItemStack map) {
        return canModify(player, map, Component.text("Only the registered owner can change this map.", NamedTextColor.RED));
    }
    public static boolean canModify(Player player, ItemStack map, Component message) {
        boolean registered = MapArtAPI.getMapUUID(map) != null;
        if ((registered || MapArtAPI.isLocked(map)) && !MapArtAPI.isOwner(player, map) && !player.hasPermission("mapart.bypass")) {
            player.sendMessage(message);
            return false;
        }
        return true;
    }
}

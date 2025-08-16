package net.glassmc.mapartcopyright.util;

import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PermissionUtil {

    private PermissionUtil() {}

    public static boolean canModify(Player player, ItemStack map) {
        return canModify(player, map, Component.text("This map is locked and you are not the owner.", NamedTextColor.RED));
    }

    public static boolean canModify(Player player, ItemStack map, Component message) {
        if (MapArtAPI.isLocked(map) && !MapArtAPI.isOwner(player, map) && !player.hasPermission("mapart.admin")) {
            player.sendMessage(message);
            return false;
        }
        return true;
    }
}

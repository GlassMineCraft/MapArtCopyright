package net.glassmc.mapartcopyright.util;

import net.glassmc.mapartcopyright.api.MapArtAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

public class CreditUtil {

    /**
     * Gets the credited creator's name from a map.
     * @param item Map item
     * @return Creator name or null if not set
     */
    public static String getCredit(ItemStack item) {
        if (!(item.getItemMeta() instanceof MapMeta meta)) return null;
        return meta.getPersistentDataContainer().get(LockUtil.CREDIT_KEY, PersistentDataType.STRING);
    }

    /**
     * Sets the creator's credit on a map with security check.
     * Only works if map is not locked or the player is the owner/admin.
     * @param item Map item
     * @param name Creator name
     * @param player Player attempting to set the credit
     * @return true if credit was successfully set
     */
    public static boolean setCredit(ItemStack item, String name, Player player) {
        if (!(item.getItemMeta() instanceof MapMeta meta)) return false;

        boolean locked = MapArtAPI.isLocked(item);
        boolean isOwner = MapArtAPI.isOwner(player, item);
        boolean isAdmin = player.hasPermission("mapart.bypass");

        if (locked && !isOwner && !isAdmin) {
            player.sendMessage("§cYou cannot change the creator on a locked map you do not own.");
            return false;
        }

        meta.getPersistentDataContainer().set(LockUtil.CREDIT_KEY, PersistentDataType.STRING, name);
        item.setItemMeta(meta);
        LoreUtil.updateMapLore(item);
        return true;
    }

    /**
     * Checks whether the player is the credited creator.
     * @param item Map item
     * @param player Player to check
     * @return true if player is the credited creator
     */
    public static boolean isCreator(ItemStack item, Player player) {
        String credit = getCredit(item);
        return credit != null && credit.equalsIgnoreCase(player.getName());
    }
}

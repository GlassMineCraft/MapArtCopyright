package net.glassmc.mapartcopyright.util;

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
     * Sets the creator's credit on a map.
     * @param item Map item
     * @param name Creator name
     */
    public static void setCredit(ItemStack item, String name) {
        if (!(item.getItemMeta() instanceof MapMeta meta)) return;
        meta.getPersistentDataContainer().set(LockUtil.CREDIT_KEY, PersistentDataType.STRING, name);
        item.setItemMeta(meta);
        LoreUtil.updateMapLore(item);
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

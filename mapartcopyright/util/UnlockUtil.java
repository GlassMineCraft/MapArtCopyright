package net.glassmc.mapartcopyright.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

public class UnlockUtil {

    public static boolean isLocked(ItemStack item) {
        if (!(item.getItemMeta() instanceof MapMeta meta)) return false;
        Byte locked = meta.getPersistentDataContainer().get(LockUtil.LOCK_KEY, PersistentDataType.BYTE);
        return locked != null && locked == 1;
    }

    public static void unlock(ItemStack item) {
        if (!(item.getItemMeta() instanceof MapMeta meta)) return;

        // Only remove the LOCK_KEY — keep credit, ID, and hologram visibility
        meta.getPersistentDataContainer().remove(LockUtil.LOCK_KEY);
        item.setItemMeta(meta);
        LoreUtil.updateMapLore(item);
    }
}
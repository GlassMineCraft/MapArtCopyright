package net.glassmc.mapartcopyright.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class LoreUtil {

    public static void updateMapLore(ItemStack item) {
        if (!(item.getItemMeta() instanceof MapMeta meta)) return;

        String credit = meta.getPersistentDataContainer().get(LockUtil.CREDIT_KEY, PersistentDataType.STRING);

        List<String> lore = new ArrayList<>();
        if (credit != null) {
            lore.add("§7Creator: §f" + credit);
        }

        // Optionally show locked status
        Byte locked = meta.getPersistentDataContainer().get(LockUtil.LOCK_KEY, PersistentDataType.BYTE);
        if (locked != null && locked == (byte) 1) {
            lore.add("§cLocked");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}

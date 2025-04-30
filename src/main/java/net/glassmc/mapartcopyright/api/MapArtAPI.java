package net.glassmc.mapartcopyright.api;

import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import net.glassmc.mapartcopyright.util.LockUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class MapArtAPI {

    /**
     * Checks if the map is locked.
     */
    public static boolean isLocked(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return false;
        Byte locked = meta.getPersistentDataContainer().get(LockUtil.LOCK_KEY, PersistentDataType.BYTE);
        return locked != null && locked == 1;
    }

    /**
     * Gets the map's UUID stored in PDC, or null if not present.
     */
    public static String getMapUUID(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return null;
        return meta.getPersistentDataContainer().get(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING);
    }

    /**
     * Checks if the provided player UUID is the owner of the map.
     */
    public static boolean isOwner(ItemStack item, UUID playerUUID) {
        String mapUUID = getMapUUID(item);
        return mapUUID != null && OwnershipDatabase.isOwner(playerUUID, mapUUID);
    }

    /**
     * Gets the owner's UUID from the database.
     */
    public static UUID getOwner(ItemStack item) {
        String mapUUID = getMapUUID(item);
        return mapUUID != null ? OwnershipDatabase.getOwner(mapUUID) : null;
    }
}

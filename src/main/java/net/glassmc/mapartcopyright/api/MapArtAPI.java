package net.glassmc.mapartcopyright.api;

import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import net.glassmc.mapartcopyright.util.LockUtil;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.Optional;

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
        boolean dbOwner = mapUUID != null && OwnershipDatabase.isOwner(playerUUID, mapUUID);
        return dbOwner || verifyCreator(item, playerUUID);
    }

    /**
     * Gets the owner's UUID from the database.
     */
    public static UUID getOwner(ItemStack item) {
        String mapUUID = getMapUUID(item);
        return mapUUID != null ? OwnershipDatabase.getOwner(mapUUID) : null;
    }

    /**
     * Checks if the player is the registered owner of the map.
     */
    public static boolean isOwner(Player player, ItemStack item) {
        return isOwner(item, player.getUniqueId());
    }

    /**
     * Returns whether the map has a valid persistent UUID.
     */
    public static boolean hasMapUUID(ItemStack item) {
        return getMapUUID(item) != null;
    }
    
    public static Optional<String> getStoredMapName(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return Optional.empty();
        if (!meta.hasDisplayName()) return Optional.empty();
        return Optional.ofNullable(meta.getDisplayName());
    }
    
    public static boolean verifyCreator(ItemStack item, UUID uuid) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return false;
        String stored = meta.getPersistentDataContainer().get(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING);
        return stored != null && stored.equals(uuid.toString());
    }
    
    public static String getMapName(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return null;
        return meta.getPersistentDataContainer().get(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING);
    }

}

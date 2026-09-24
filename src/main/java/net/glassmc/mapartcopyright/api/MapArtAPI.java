package net.glassmc.mapartcopyright.api;

import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import net.glassmc.mapartcopyright.service.ArtworkService;
import net.glassmc.mapartcopyright.util.*;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.Optional;
import java.util.UUID;

public final class MapArtAPI {
    private MapArtAPI() {}
    public static boolean isLocked(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return false;
        try {
            var membership = ArtworkService.find(item);
            if (membership != null) return membership.artwork().locked();
        } catch (java.sql.SQLException ex) { return true; } // Fail closed, including stale pre-group copies.
        return meta.getPersistentDataContainer().getOrDefault(LockUtil.LOCK_KEY, PersistentDataType.BYTE, (byte) 0) == 1;
    }

    public static boolean isFrameLocked(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return false;
        try {
            var membership = ArtworkService.find(item);
            if (membership != null) return membership.artwork().frameLocked();
        } catch (java.sql.SQLException ex) { return true; }
        return meta.getPersistentDataContainer().getOrDefault(LockUtil.ITEMFRAME_LOCK_KEY, PersistentDataType.BYTE, (byte) 0) == 1;
    }

    public static boolean isArtworkTile(ItemStack item) {
        try { return ArtworkService.find(item) != null; }
        catch (java.sql.SQLException ex) { return true; }
    }
    public static String getMapUUID(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return null;
        return meta.getPersistentDataContainer().get(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING);
    }
    public static boolean isOwner(ItemStack item, UUID playerUUID) {
        String id = getMapUUID(item);
        return id != null && OwnershipDatabase.isOwner(playerUUID, id);
    }
    public static boolean isOwner(Player player, ItemStack item) { return isOwner(item, player.getUniqueId()); }
    public static UUID getOwner(ItemStack item) { return OwnershipDatabase.getOwner(getMapUUID(item)); }
    public static boolean hasMapUUID(ItemStack item) {
        try { return getMapUUID(item) != null && UUID.fromString(getMapUUID(item)) != null; }
        catch (IllegalArgumentException ex) { return false; }
    }
    public static Optional<String> getStoredMapName(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return Optional.empty();
        if (!meta.hasDisplayName() && !meta.getPersistentDataContainer().has(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING))
            return Optional.empty();
        return Optional.of(PlainTextComponentSerializer.plainText().serialize(MapMetadata.name(meta)));
    }
    /** Checks attribution only; this must never grant ownership rights. */
    public static boolean verifyCreator(ItemStack item, UUID uuid) {
        if (uuid == null || item == null || !(item.getItemMeta() instanceof MapMeta meta)) return false;
        return uuid.toString().equals(meta.getPersistentDataContainer().get(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING));
    }
    public static String getMapName(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return null;
        return meta.getPersistentDataContainer().get(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING);
    }
}

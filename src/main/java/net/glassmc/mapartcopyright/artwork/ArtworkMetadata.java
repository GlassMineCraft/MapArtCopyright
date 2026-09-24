package net.glassmc.mapartcopyright.artwork;

import net.glassmc.mapartcopyright.util.*;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

public final class ArtworkMetadata {
    private ArtworkMetadata() {}

    public static void apply(MapMeta meta, ArtworkMembership member) {
        var artwork = member.artwork();
        var data = meta.getPersistentDataContainer();
        data.set(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING, member.tile().mapId());
        data.set(LockUtil.ARTWORK_ID_KEY, PersistentDataType.STRING, artwork.id().toString());
        data.set(LockUtil.ARTWORK_WIDTH_KEY, PersistentDataType.INTEGER, artwork.size().width());
        data.set(LockUtil.ARTWORK_HEIGHT_KEY, PersistentDataType.INTEGER, artwork.size().height());
        data.set(LockUtil.TILE_X_KEY, PersistentDataType.INTEGER, member.tile().x());
        data.set(LockUtil.TILE_Y_KEY, PersistentDataType.INTEGER, member.tile().y());
        data.set(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING, artwork.name());
        if (artwork.credit() == null) data.remove(LockUtil.CREDIT_KEY);
        else data.set(LockUtil.CREDIT_KEY, PersistentDataType.STRING, artwork.credit());
        if (artwork.locked()) flag(meta, LockUtil.LOCK_KEY, true);
        else data.remove(LockUtil.LOCK_KEY);
        flag(meta, LockUtil.MAPART_NAME_VISIBLE_KEY, artwork.nameVisible());
        flag(meta, LockUtil.HOLOGRAM_VISIBLE_KEY, artwork.hologramVisible());
        flag(meta, LockUtil.ITEMFRAME_LOCK_KEY, artwork.frameLocked());
        MapMetadata.render(meta);
        LoreUtil.apply(meta);
    }

    private static void flag(MapMeta meta, NamespacedKey key, boolean enabled) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) (enabled ? 1 : 0));
    }

    public static ArtworkRecord edited(ArtworkRecord previous, MapMeta meta) {
        var data = meta.getPersistentDataContainer();
        return new ArtworkRecord(previous.id(), previous.owner(), previous.size(), MapMetadata.storedName(meta),
                data.get(LockUtil.CREDIT_KEY, PersistentDataType.STRING),
                enabled(meta, LockUtil.LOCK_KEY), MapMetadata.visible(meta),
                enabled(meta, LockUtil.HOLOGRAM_VISIBLE_KEY), enabled(meta, LockUtil.ITEMFRAME_LOCK_KEY), previous.revision() + 1);
    }

    private static boolean enabled(MapMeta meta, NamespacedKey key) {
        return meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.BYTE, (byte) 0) == 1;
    }
}

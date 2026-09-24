package net.glassmc.mapartcopyright.util;

import org.bukkit.NamespacedKey;
import net.glassmc.mapartcopyright.MapArtCopyright;

public class LockUtil {
    public static final NamespacedKey LOCK_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "locked");
    public static final NamespacedKey CREDIT_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "credit");
    public static final NamespacedKey HOLOGRAM_VISIBLE_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "hologram_visible");
    public static final NamespacedKey MAPART_ID_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "mapart_id");
    public static final NamespacedKey MAPART_NAME_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "map_name");
    public static final NamespacedKey CREATOR_UUID_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "creator_uuid");
    public static final NamespacedKey MAPART_NAME_VISIBLE_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "name_visible");
    public static final NamespacedKey ITEMFRAME_LOCK_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "frame_locked");
    public static final NamespacedKey HOLOGRAM_TAG_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "hologram_tag");
    public static final NamespacedKey HOLOGRAM_ENTITY_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "hologram_entity");
    public static final NamespacedKey ARTWORK_ID_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "artwork_id");
    public static final NamespacedKey ARTWORK_WIDTH_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "artwork_width");
    public static final NamespacedKey ARTWORK_HEIGHT_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "artwork_height");
    public static final NamespacedKey TILE_X_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "tile_x");
    public static final NamespacedKey TILE_Y_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "tile_y");
}

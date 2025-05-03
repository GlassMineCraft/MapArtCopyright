package net.glassmc.mapartcopyright.util;

import org.bukkit.NamespacedKey;
import net.glassmc.mapartcopyright.MapArtCopyright;

public class LockUtil {
    public static final NamespacedKey LOCK_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "locked");
    public static final NamespacedKey CREDIT_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "credit");
    public static final NamespacedKey HOLOGRAM_VISIBLE_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "hologram_visible");
    public static final NamespacedKey MAPART_ID_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "mapart_id");
    public static final NamespacedKey MAPART_NAME_KEY = new NamespacedKey(MapArtCopyright.getInstance(), "map_name");


}

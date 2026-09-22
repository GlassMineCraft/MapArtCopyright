package net.glassmc.mapartcopyright.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

/** Canonical text is independent from visible rendering. */
public final class MapMetadata {
    public static final MiniMessage MINI = MiniMessage.miniMessage();
    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('§').hexColors().build();
    private MapMetadata() {}

    public static Component name(MapMeta meta) {
        String stored = meta.getPersistentDataContainer().get(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING);
        if (stored != null) return MINI.deserialize(stored);
        return meta.displayName() != null ? meta.displayName() : Component.text("Untitled Map", NamedTextColor.GRAY);
    }

    public static void initialize(MapMeta meta) {
        var data = meta.getPersistentDataContainer();
        if (!data.has(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING)) {
            data.set(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING, MINI.serialize(name(meta)));
        }
        if (!data.has(LockUtil.MAPART_NAME_VISIBLE_KEY, PersistentDataType.BYTE)) {
            data.set(LockUtil.MAPART_NAME_VISIBLE_KEY, PersistentDataType.BYTE, (byte) 1);
        }
    }

    public static void setName(MapMeta meta, Component name) {
        meta.getPersistentDataContainer().set(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING, MINI.serialize(name));
    }

    public static boolean visible(MapMeta meta) {
        return meta.getPersistentDataContainer().getOrDefault(LockUtil.MAPART_NAME_VISIBLE_KEY, PersistentDataType.BYTE, (byte) 1) == 1;
    }

    public static void render(MapMeta meta) { meta.displayName(visible(meta) ? name(meta) : null); }
    public static String storedName(MapMeta meta) { return MINI.serialize(name(meta)); }
}

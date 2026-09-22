package net.glassmc.mapartcopyright.util;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;

public final class LoreUtil {
    private LoreUtil() {}
    public static void apply(MapMeta meta) {
        if (!MapArtCopyright.getInstance().getConfig().getBoolean("features.enable-map-lore-updates", true)) return;
        List<Component> lore = new ArrayList<>();
        String credit = meta.getPersistentDataContainer().get(LockUtil.CREDIT_KEY, PersistentDataType.STRING);
        if (credit != null) lore.add(Component.text("Creator: ", NamedTextColor.GRAY)
                .append(MapMetadata.LEGACY.deserialize(credit)).decoration(TextDecoration.ITALIC, false));
        if (meta.getPersistentDataContainer().getOrDefault(LockUtil.LOCK_KEY, PersistentDataType.BYTE, (byte) 0) == 1)
            lore.add(Component.text("Locked", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
    }
    public static void updateMapLore(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta)) return;
        apply(meta);
        item.setItemMeta(meta);
    }
}

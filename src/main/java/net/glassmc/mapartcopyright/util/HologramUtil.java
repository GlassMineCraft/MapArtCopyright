package net.glassmc.mapartcopyright.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

public class HologramUtil {

    private static final String TAG = "mapart_holo";

    public static void spawn(ItemFrame frame, String legacyText) {
        // Offset slightly below the item frame so the text sits in front of it
        Location loc = frame.getLocation().clone().add(0.5, -0.75, 0.5);

        Component text = LegacyComponentSerializer.legacySection().deserialize(legacyText);

        frame.getWorld().spawn(loc, TextDisplay.class, display -> {
            display.text(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.getPersistentDataContainer().set(
                    LockUtil.HOLOGRAM_TAG_KEY, PersistentDataType.STRING, TAG);
        });
    }

    public static void remove(ItemFrame frame) {
        frame.getNearbyEntities(1.0, 1.5, 1.0).stream()
                .filter(e -> e instanceof TextDisplay)
                .map(e -> (TextDisplay) e)
                .filter(e -> TAG.equals(e.getPersistentDataContainer()
                        .get(LockUtil.HOLOGRAM_TAG_KEY, PersistentDataType.STRING)))
                .forEach(Entity::remove);
    }
}

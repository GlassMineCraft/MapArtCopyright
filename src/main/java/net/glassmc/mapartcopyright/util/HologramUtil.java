package net.glassmc.mapartcopyright.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.UUID;

public final class HologramUtil {
    private HologramUtil() {}

    public static void spawn(ItemFrame frame, String text) {
        remove(frame);
        Location location = frame.getLocation().clone().add(0, -0.65, 0)
                .add(frame.getFacing().getDirection().multiply(0.12));
        TextDisplay display = frame.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.text(MapMetadata.LEGACY.deserialize(text));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setShadowed(true);
            entity.setDefaultBackground(false);
            entity.setPersistent(false);
            entity.getPersistentDataContainer().set(LockUtil.HOLOGRAM_TAG_KEY, PersistentDataType.STRING, frame.getUniqueId().toString());
        });
        frame.getPersistentDataContainer().set(LockUtil.HOLOGRAM_ENTITY_KEY, PersistentDataType.STRING, display.getUniqueId().toString());
    }

    public static void remove(ItemFrame frame) {
        String frameId = frame.getUniqueId().toString();
        String displayId = frame.getPersistentDataContainer().get(LockUtil.HOLOGRAM_ENTITY_KEY, PersistentDataType.STRING);
        if (displayId != null) {
            try {
                Entity display = Bukkit.getEntity(UUID.fromString(displayId));
                if (belongsTo(display, frameId)) display.remove();
            } catch (IllegalArgumentException ignored) { }
        }
        // Reconcile duplicate displays left by a previous reload, without touching adjacent frames.
        for (Entity entity : frame.getNearbyEntities(2, 2, 2)) if (belongsTo(entity, frameId)) entity.remove();
        frame.getPersistentDataContainer().remove(LockUtil.HOLOGRAM_ENTITY_KEY);
    }

    public static boolean belongsTo(Entity entity, String frameId) {
        return entity instanceof TextDisplay && frameId.equals(entity.getPersistentDataContainer().get(LockUtil.HOLOGRAM_TAG_KEY, PersistentDataType.STRING));
    }

    public static void removeLegacy(Entity entity) {
        if (entity instanceof TextDisplay && "mapart_holo".equals(entity.getPersistentDataContainer()
                .get(LockUtil.HOLOGRAM_TAG_KEY, PersistentDataType.STRING))) entity.remove();
        // Pre-TextDisplay versions used the credit key to mark invisible armor stands.
        if (entity instanceof ArmorStand && "mapart_holo".equals(entity.getPersistentDataContainer()
                .get(LockUtil.CREDIT_KEY, PersistentDataType.STRING))) entity.remove();
    }
}

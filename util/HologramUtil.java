package net.glassmc.mapartcopyright.util;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.persistence.PersistentDataType;

public class HologramUtil {

    private static final String TAG = "mapart_holo";

    public static void spawn(ItemFrame frame, String text) {
        Location above = frame.getLocation().clone().subtract(0, 1.0, 0);
        ArmorStand stand = frame.getWorld().spawn(above, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setMarker(true);
            s.setCustomNameVisible(true);
            s.setCustomName(text);
            s.setSmall(true);
            s.setGravity(false);
            s.getPersistentDataContainer().set(LockUtil.LOCK_KEY, PersistentDataType.BYTE, (byte) 1);
            s.getPersistentDataContainer().set(LockUtil.CREDIT_KEY, PersistentDataType.STRING, TAG);
        });
    }

    public static void remove(ItemFrame frame) {
        frame.getNearbyEntities(0.5, 1.5, 0.5).stream()
                .filter(e -> e instanceof ArmorStand)
                .map(e -> (ArmorStand) e)
                .filter(e -> TAG.equals(e.getPersistentDataContainer().get(LockUtil.CREDIT_KEY, PersistentDataType.STRING)))
                .forEach(Entity::remove);
    }
}

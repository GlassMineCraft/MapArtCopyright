package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.util.HologramUtil;
import net.glassmc.mapartcopyright.util.LockUtil;
import net.glassmc.mapartcopyright.Audit.AuditLogger;
import net.glassmc.mapartcopyright.api.MapArtAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

public class MapFrameListener implements Listener {

    @EventHandler
    public void onInteractItemFrame(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;
        Player player = event.getPlayer();
        ItemStack frameItem = frame.getItem();

        // Shift-click on a frame containing a locked map toggles the frame lock state
        if (player.isSneaking() && frameItem.getType() == Material.FILLED_MAP && MapArtAPI.isLocked(frameItem)) {
            if (!player.hasPermission("mapart.toggle.itemframe")) {
                player.sendMessage("§cYou don't have permission to toggle item frame locks.");
                event.setCancelled(true);
                return;
            }
            if (!(frameItem.getItemMeta() instanceof MapMeta meta)) return;
            byte current = meta.getPersistentDataContainer().getOrDefault(
                    LockUtil.ITEMFRAME_LOCK_KEY, PersistentDataType.BYTE, (byte) 0);
            byte toggled = (current == 1) ? (byte) 0 : (byte) 1;
            meta.getPersistentDataContainer().set(LockUtil.ITEMFRAME_LOCK_KEY, PersistentDataType.BYTE, toggled);
            frameItem.setItemMeta(meta);
            frame.setItem(frameItem);
            frame.setFixed(toggled == 1);
            if (toggled == 1) {
                player.sendMessage("§cItem frame locked.");
            } else {
                player.sendMessage("§aItem frame unlocked.");
            }
            String mapUUID = MapArtAPI.getMapUUID(frameItem);
            if (mapUUID != null) {
                AuditLogger.log("frame_lock_toggled", player.getName(), mapUUID);
            }
            event.setCancelled(true);
            return;
        }

        // Allow any player to remove a map (no ownership check for removal)
        // Handle map insertion (spawning holograms) and log removals
        Bukkit.getScheduler().runTaskLater(
            MapArtCopyright.getInstance(),
            () -> {
                ItemStack newItem = frame.getItem();
                if (newItem.getType() != Material.FILLED_MAP) {
                    // If the frame is now empty, the map was removed
                    frame.setFixed(false);
                    if (frameItem.getType() == Material.FILLED_MAP && MapArtAPI.isLocked(frameItem)) {
                        String mapUUID = MapArtAPI.getMapUUID(frameItem);
                        if (mapUUID != null) {
                            AuditLogger.log("removed_map", player.getName(), mapUUID);
                        }
                    }
                    return;
                }
                if (!(newItem.getItemMeta() instanceof MapMeta meta)) return;

                // Apply item frame lock state
                byte frameLock = meta.getPersistentDataContainer().getOrDefault(
                        LockUtil.ITEMFRAME_LOCK_KEY, PersistentDataType.BYTE, (byte) 0);
                frame.setFixed(frameLock == 1);

                String credit = meta.getPersistentDataContainer().get(LockUtil.CREDIT_KEY, PersistentDataType.STRING);
                Byte hologramFlag = meta.getPersistentDataContainer().get(LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE);

                if (credit != null && (hologramFlag == null || hologramFlag == 1)) {
                    HologramUtil.remove(frame); // Cleanup any existing
                    HologramUtil.spawn(frame, "§7Creator: §f" + credit); // Spawn if enabled
                } else {
                    HologramUtil.remove(frame);
                }
            },
            2L
        );
    }

    @EventHandler
    public void onFrameDetach(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        HologramUtil.remove(frame);
    }

    @EventHandler
    public void onBreak(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        ItemStack frameItem = frame.getItem();
        if (frameItem.getType() == Material.FILLED_MAP && MapArtAPI.isLocked(frameItem)) {
            String mapUUID = MapArtAPI.getMapUUID(frameItem);
            if (mapUUID != null && event.getDamager() instanceof Player player) {
                boolean isOwner = MapArtAPI.isOwner(player, frameItem);
                boolean hasBypass = player.hasPermission("mapart.bypass");
                if (!isOwner && !hasBypass) {
                    player.sendMessage("§cYou cannot break this item frame containing a locked map.");
                    event.setCancelled(true);
                    AuditLogger.log("denied_break_frame", player.getName(), mapUUID);
                    return;
                }
                AuditLogger.log("broke_frame", player.getName(), mapUUID);
            }
        }
        HologramUtil.remove(frame);
    }
}
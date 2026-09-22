package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.Audit.AuditLogger;
import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

public class MapFrameListener implements Listener {
    public static boolean frameLocked(ItemStack item) {
        return item != null && item.getItemMeta() instanceof MapMeta meta && meta.getPersistentDataContainer()
                .getOrDefault(LockUtil.ITEMFRAME_LOCK_KEY, PersistentDataType.BYTE, (byte) 0) == 1;
    }
    public static boolean protectedFrame(ItemFrame frame) { return MapArtAPI.isLocked(frame.getItem()) || frameLocked(frame.getItem()); }
    private boolean allowed(Player player, ItemFrame frame) {
        return player != null && (player.hasPermission("mapart.bypass") || MapArtAPI.isOwner(player, frame.getItem()));
    }
    private Player actor(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractItemFrame(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;
        if (protectedFrame(frame) && !allowed(event.getPlayer(), frame)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cOnly the owner can change this protected frame.");
            return;
        }
        // Legacy fixed frames blocked their owner too. Event guards now provide the protection.
        if (frameLocked(frame.getItem())) frame.setFixed(false);
        Bukkit.getScheduler().runTask(MapArtCopyright.getInstance(), () -> { if (!event.isCancelled()) refresh(frame); });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame) || !protectedFrame(frame)) return;
        Player player = event instanceof EntityDamageByEntityEvent byEntity ? actor(byEntity.getDamager()) : null;
        if (!allowed(player, frame)) {
            event.setCancelled(true);
            if (player != null) AuditLogger.log("denied_break_frame", player, MapArtAPI.getMapUUID(frame.getItem()), "");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof ItemFrame frame)
            Bukkit.getScheduler().runTask(MapArtCopyright.getInstance(), () -> refresh(frame));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectDetach(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame) || !protectedFrame(frame)) return;
        Player player = event instanceof HangingBreakByEntityEvent byEntity ? actor(byEntity.getRemover()) : null;
        if (!allowed(player, frame)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterDetach(HangingBreakEvent event) {
        if (event.getEntity() instanceof ItemFrame frame)
            Bukkit.getScheduler().runTask(MapArtCopyright.getInstance(), () -> HologramUtil.remove(frame));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectSupport(BlockBreakEvent event) {
        Location block = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (Entity entity : event.getBlock().getWorld().getNearbyEntities(block, 2, 2, 2)) {
            if (entity instanceof ItemFrame frame && protectedFrame(frame)
                    && frame.getLocation().getBlock().getRelative(frame.getAttachedFace()).equals(event.getBlock())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cRemove the protected map from its frame before breaking the supporting block.");
                return;
            }
        }
    }

    public static void refresh(ItemFrame frame) {
        if (!frame.isValid()) { HologramUtil.remove(frame); return; }
        ItemStack item = frame.getItem();
        if (!(item.getItemMeta() instanceof MapMeta meta)) { HologramUtil.remove(frame); return; }
        if (frameLocked(item)) frame.setFixed(false);
        String credit = CreditUtil.getCredit(item);
        boolean visible = meta.getPersistentDataContainer().getOrDefault(LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE,
                (byte) (MapArtCopyright.getInstance().getConfig().getBoolean("settings.default-hologram-visible", true) ? 1 : 0)) == 1;
        if (credit != null && visible) HologramUtil.spawn(frame, "§7Creator: §f" + credit);
        else HologramUtil.remove(frame);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Bukkit.getScheduler().runTask(MapArtCopyright.getInstance(), () -> {
            if (!event.getChunk().isLoaded()) return;
            for (Entity entity : event.getChunk().getEntities()) HologramUtil.removeLegacy(entity);
            for (Entity entity : event.getChunk().getEntities()) if (entity instanceof ItemFrame frame) refresh(frame);
        });
    }

    public static void initializeLoadedFrames() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) HologramUtil.removeLegacy(entity);
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) refresh(frame);
        }
    }
}

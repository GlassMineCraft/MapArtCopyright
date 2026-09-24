package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.service.ArtworkService;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import java.sql.SQLException;

/** Refresh copies that were offline, in closed containers, or on the ground during a group edit. */
public class ArtworkSyncListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        Bukkit.getScheduler().runTask(MapArtCopyright.getInstance(), () -> {
            if (!event.getPlayer().isValid()) return;
            ArtworkService.refreshInventorySafely(event.getInventory());
            ArtworkService.refreshInventorySafely(event.getPlayer().getInventory());
        });
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(MapArtCopyright.getInstance(), () -> {
            if (event.getPlayer().isOnline()) ArtworkService.refreshInventorySafely(event.getPlayer().getInventory());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        var stack = event.getItem().getItemStack();
        try { if (ArtworkService.refresh(stack)) event.getItem().setItemStack(stack); }
        catch (SQLException ex) { ArtworkService.warnSync(ex); }
    }
}

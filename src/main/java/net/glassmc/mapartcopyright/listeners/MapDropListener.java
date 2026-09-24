package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.util.MapMetadata;
import net.glassmc.mapartcopyright.util.LoreUtil;
import net.glassmc.mapartcopyright.service.ArtworkService;
import org.bukkit.event.*;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.meta.MapMeta;

/** Retain item metadata, and repair legacy dropped items from the stored canonical title. */
public class MapDropListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        var item = event.getEntity().getItemStack();
        if (MapArtAPI.getMapUUID(item) == null) return;
        try { ArtworkService.refresh(item); }
        catch (java.sql.SQLException ex) { ArtworkService.warnSync(ex); return; }
        if (!(item.getItemMeta() instanceof MapMeta meta)) return;
        MapMetadata.render(meta);
        LoreUtil.apply(meta);
        item.setItemMeta(meta);
        event.getEntity().setItemStack(item);
    }
}

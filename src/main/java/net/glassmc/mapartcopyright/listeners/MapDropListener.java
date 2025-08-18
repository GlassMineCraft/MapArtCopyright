package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.util.LockUtil;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener to clear visible map metadata when the map item is dropped.
 */
public class MapDropListener implements Listener {

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        if (item.getType() != Material.FILLED_MAP) return;
        if (!(item.getItemMeta() instanceof MapMeta meta)) return;

        // Only sanitize maps tracked by the plugin (those with a stored UUID).
        if (!meta.getPersistentDataContainer().has(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING)) return;

        meta.setDisplayName(null);
        meta.setLore(null);
        item.setItemMeta(meta);
        event.getEntity().setItemStack(item);
        event.getEntity().setCustomName(null);
        event.getEntity().setCustomNameVisible(false);
    }
}

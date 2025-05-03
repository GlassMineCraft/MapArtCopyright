package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.util.HologramUtil;
import net.glassmc.mapartcopyright.util.LockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

public class MapFrameListener implements Listener {

    @EventHandler
    public void onInsertMap(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;

        // Run a delayed task to check the inserted item
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("MapArtCopyright"),
            () -> {
                if (frame.getItem().getType() != Material.FILLED_MAP) return;
                if (!(frame.getItem().getItemMeta() instanceof MapMeta meta)) return;

                String credit = meta.getPersistentDataContainer().get(LockUtil.CREDIT_KEY, PersistentDataType.STRING);
                Byte hologramFlag = meta.getPersistentDataContainer().get(LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE);

                if (credit != null && (hologramFlag == null || hologramFlag == 1)) {
                    HologramUtil.remove(frame); // cleanup any existing
                    HologramUtil.spawn(frame, "§7Creator: §f" + credit); // only spawn if enabled
                }
            },
            2L
        );
    }

    @EventHandler
    public void onBreak(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame frame) {
            HologramUtil.remove(frame);
        }
    }
}

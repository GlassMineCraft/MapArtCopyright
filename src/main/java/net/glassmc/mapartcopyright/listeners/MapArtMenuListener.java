package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.gui.*;
import net.glassmc.mapartcopyright.service.MapArtService;
import net.glassmc.mapartcopyright.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

public class MapArtMenuListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MapArtMenu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= event.getView().getTopInventory().getSize()) return;
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
        Bukkit.getScheduler().runTask(MapArtCopyright.getInstance(), () -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory().getHolder() != menu) return;
            if (raw == 44) { player.closeInventory(); return; }
            if (!MapArtGUI.canOpen(player)) { player.closeInventory(); return; }
            if (!menu.matches(player)) {
                player.closeInventory();
                player.sendMessage("§cThe held map changed. Open its menu again.");
                return;
            }
            ItemStack map = player.getInventory().getItemInMainHand();
            boolean changed = false;
            switch (raw) {
                case 0 -> InputManager.ask(player, InputManager.InputType.RENAME_MAP, map);
                case 8 -> InputManager.ask(player, InputManager.InputType.SET_CREDIT, map);
                case 20 -> changed = MapArtService.credit(player, map, player.getName());
                case 22 -> changed = MapArtService.lock(player, map);
                case 24 -> changed = MapArtService.unlock(player, map);
                case 2, 33, 42 -> changed = MapArtService.toggle(player, map, LockUtil.MAPART_NAME_VISIBLE_KEY, "mapart.toggle.displayname");
                case 30, 39 -> changed = MapArtService.toggle(player, map, LockUtil.ITEMFRAME_LOCK_KEY, "mapart.toggle.itemframe");
                case 32, 41 -> changed = MapArtService.toggle(player, map, LockUtil.HOLOGRAM_VISIBLE_KEY, "mapart.toggle.hologram");
                default -> { }
            }
            if (changed) MapArtGUI.open(player, map);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MapArtMenu) event.setCancelled(true);
    }
}

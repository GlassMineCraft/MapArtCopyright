package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.Audit.AuditLogger;
import net.glassmc.mapartcopyright.api.MapArtAPI;
import org.bukkit.Material;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

public class MapInteractionListener implements Listener {
    private ItemStack protectedSource(InventoryClickEvent event) {
        if (event instanceof CraftItemEvent craft) {
            for (ItemStack item : craft.getInventory().getMatrix()) if (MapArtAPI.isLocked(item)) return item;
        } else if (event.getInventory().getType() == InventoryType.CARTOGRAPHY && event.getRawSlot() == 2) {
            ItemStack item = event.getInventory().getItem(0);
            if (MapArtAPI.isLocked(item)) return item;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectResult(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack source = protectedSource(event);
        if (source == null) return;
        if (!MapArtAPI.isOwner(player, source) && !player.hasPermission("mapart.bypass")) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot copy or change this protected map.");
            AuditLogger.log("denied_" + action(event), player, MapArtAPI.getMapUUID(source), "");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void auditResult(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getAction() == InventoryAction.NOTHING) return;
        ItemStack source = protectedSource(event);
        if (source != null) AuditLogger.log(action(event) + "_allowed", player, MapArtAPI.getMapUUID(source),
                "authorization recorded; vanilla inventory transfer follows");
    }

    private String action(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.CARTOGRAPHY) return "craft_map";
        ItemStack extra = event.getInventory().getItem(1);
        if (extra == null) return "cartography";
        return switch (extra.getType()) {
            case MAP -> "clone";
            case PAPER -> "scale";
            case GLASS_PANE -> "freeze";
            default -> "cartography";
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        ItemStack protectedItem = MapArtAPI.isLocked(event.getResult()) ? event.getResult() : null;
        if (event.getBlock().getState() instanceof Crafter crafter) {
            for (ItemStack item : crafter.getInventory().getContents()) {
                if (MapArtAPI.isLocked(item)) { protectedItem = item; break; }
            }
        }
        if (protectedItem != null) {
            event.setCancelled(true);
            AuditLogger.log("denied_automatic_craft", "automation", MapArtAPI.getMapUUID(protectedItem));
        }
    }
}

package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.Audit.AuditLogger;
import net.glassmc.mapartcopyright.api.MapArtAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class MapInteractionListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        // Check for crafting table, cartography table, or anvil (renaming)
        InventoryType type = event.getInventory().getType();
        if (type != InventoryType.CRAFTING && type != InventoryType.CARTOGRAPHY && type != InventoryType.ANVIL) return;

        // Check if clicking the result slot
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() != org.bukkit.Material.FILLED_MAP) return;
        if (!(result.getItemMeta() instanceof MapMeta)) return;

        // Check if the map is locked
        if (!MapArtAPI.isLocked(result)) return;

        String mapUUID = MapArtAPI.getMapUUID(result);
        if (mapUUID == null) {
            player.sendMessage("§cThis map has no ownership data.");
            event.setCancelled(true);
            return;
        }

        // Check ownership or permissions
        boolean isOwner = MapArtAPI.isOwner(player, result);
        boolean hasBypass = player.hasPermission("mapart.bypass") || player.hasPermission("mapart.admin");
        if (!isOwner && !hasBypass) {
            String actionWord = type == InventoryType.ANVIL ? "rename" : "clone or scale";
            player.sendMessage("§cYou cannot " + actionWord + " this locked map.");
            event.setCancelled(true);
            String denied = type == InventoryType.ANVIL ? "denied_rename" : "denied_clone_or_scale";
            AuditLogger.log(denied, player.getName(), mapUUID);
            return;
        }

        // Log successful action
        String action;
        if (type == InventoryType.CRAFTING) {
            action = "cloned";
        } else if (type == InventoryType.CARTOGRAPHY) {
            action = "scaled";
        } else {
            action = "renamed";
        }
        AuditLogger.log(action, player.getName(), mapUUID);
    }
}
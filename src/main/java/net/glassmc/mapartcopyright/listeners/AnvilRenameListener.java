package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.Audit.AuditLogger;
import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.database.*;
import net.glassmc.mapartcopyright.service.MapArtService;
import net.glassmc.mapartcopyright.util.*;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.view.AnvilView;
import java.sql.SQLException;

public class AnvilRenameListener implements Listener {
    private boolean changed(ItemStack input, String raw) {
        if (raw == null || !(input.getItemMeta() instanceof MapMeta meta)) return false;
        String current = meta.displayName() == null ? "" : PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        return !raw.equals(current);
    }

    private boolean allowed(Player player, ItemStack item) {
        if (MapArtAPI.isArtworkTile(item)) return false;
        return player.hasPermission("mapart.use") && player.hasPermission("mapart.rename")
                && ((MapArtAPI.getMapUUID(item) == null && !MapArtAPI.isLocked(item))
                || MapArtAPI.isOwner(player, item) || player.hasPermission("mapart.bypass"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack input = event.getInventory().getItem(0);
        String raw = event.getView().getRenameText();
        if (input == null || !changed(input, raw)) return;
        Player player = (Player) event.getView().getPlayer();
        if (!allowed(player, input)) { event.setResult(null); return; }
        try { event.setResult(MapArtService.previewAnvilName(input, raw)); }
        catch (IllegalArgumentException ex) { event.setResult(null); }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onResultClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory inventory) || event.getRawSlot() != 2
                || !(event.getWhoClicked() instanceof Player player) || event.getAction() == InventoryAction.NOTHING) return;
        ItemStack input = inventory.getItem(0);
        if (!(event.getView() instanceof AnvilView view)) return;
        String raw = view.getRenameText();
        if (input == null || !changed(input, raw)) return;
        if (!allowed(player, input)) {
            event.setCancelled(true);
            Messages.send(player, "no-permission", "§cYou cannot rename this map.");
            AuditLogger.log("denied_rename", player, MapArtAPI.getMapUUID(input), "anvil");
            return;
        }
        try {
            ItemStack result = MapArtService.previewAnvilName(input, raw);
            String id = MapArtAPI.getMapUUID(input);
            MapRecord before = id == null ? null : OwnershipDatabase.find(id);
            if (!MapArtService.saveAnvilResult(player, input, result)) { event.setCancelled(true); return; }
            event.setCurrentItem(result);
            MapMeta resultMeta = (MapMeta) result.getItemMeta();
            MapRecord expected = before == null ? null : new MapRecord(id, before.playerUUID, MapMetadata.storedName(resultMeta), CreditUtil.getCredit(result));
            Bukkit.getScheduler().runTask(MapArtCopyright.getInstance(), () -> {
                if (event.isCancelled()) {
                    if (before != null) {
                        try { OwnershipDatabase.restoreMetadataIfUnchanged(expected, before); }
                        catch (SQLException ex) { MapArtCopyright.getInstance().getLogger().severe("Could not revert canceled anvil metadata: " + ex.getMessage()); }
                    }
                } else AuditLogger.log("anvil_rename_allowed", player, id, "authorized vanilla result");
            });
        } catch (IllegalArgumentException | SQLException ex) {
            event.setCancelled(true);
            player.sendMessage("§cThe map could not be renamed: " + ex.getMessage());
        }
    }
}

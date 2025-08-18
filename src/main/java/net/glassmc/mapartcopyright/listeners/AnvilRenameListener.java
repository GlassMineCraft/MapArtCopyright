package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.Audit.AuditLogger;
import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.util.StringSanitizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class AnvilRenameListener implements Listener {

    private boolean canRename(Player player, ItemStack item) {
        if (!player.hasPermission("mapart.name")) {
            return false;
        }
        boolean locked = MapArtAPI.isLocked(item);
        boolean isOwner = MapArtAPI.isOwner(player, item);
        boolean hasBypass = player.hasPermission("mapart.bypass") || player.hasPermission("mapart.admin");
        return !locked || isOwner || hasBypass;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack input = inv.getItem(0);
        if (input == null || !(input.getItemMeta() instanceof MapMeta)) return;

        String rename = inv.getRenameText();
        if (rename == null) return;
        MapMeta meta = (MapMeta) input.getItemMeta();
        if (rename.isBlank() || (meta.hasDisplayName() && rename.equals(meta.getDisplayName()))) return;

        Player player = (Player) event.getView().getPlayer();
        if (!canRename(player, input)) {
            event.setResult(null);
            return;
        }

        try {
            Component name = StringSanitizer.parseComponent(rename, 32);
            ItemStack result = input.clone();
            MapMeta resultMeta = (MapMeta) result.getItemMeta();
            resultMeta.displayName(name);
            result.setItemMeta(resultMeta);
            event.setResult(result);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
            event.setResult(null);
        }
    }

    @EventHandler
    public void onResultClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory inv)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack input = inv.getItem(0);
        if (input == null || !(input.getItemMeta() instanceof MapMeta)) return;

        String rename = inv.getRenameText();
        if (rename == null) return;
        MapMeta meta = (MapMeta) input.getItemMeta();
        if (rename.isBlank() || (meta.hasDisplayName() && rename.equals(meta.getDisplayName()))) return;

        String mapUUID = MapArtAPI.getMapUUID(input);
        if (!canRename(player, input)) {
            event.setCancelled(true);
            if (!player.hasPermission("mapart.name")) {
                player.sendMessage("§cYou don’t have permission to rename maps.");
            } else {
                player.sendMessage("§cThis map is locked and you are not the owner.");
            }
            if (mapUUID != null) {
                AuditLogger.log("denied_rename", player.getName(), mapUUID);
            }
            return;
        }

        if (mapUUID != null) {
            AuditLogger.log("renamed", player.getName(), mapUUID);
        }
    }
}

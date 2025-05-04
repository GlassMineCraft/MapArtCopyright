package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.gui.MapArtGUI;
import net.glassmc.mapartcopyright.util.InputManager;
import net.glassmc.mapartcopyright.util.LockUtil;
import net.glassmc.mapartcopyright.util.LoreUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

public class MapArtMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(MapArtGUI.GUI_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        Material type = clicked.getType();
        String displayName = clicked.getItemMeta().getDisplayName();
        ItemStack map = player.getInventory().getItemInMainHand();

        switch (type) {
            case ANVIL -> handleInput(player, "mapart.rename", InputManager.InputType.RENAME_MAP, map, "rename maps");
            case WRITABLE_BOOK -> handleInput(player, "mapart.credit", InputManager.InputType.SET_CREDIT, map, "set creator names");
            case PLAYER_HEAD -> executeCommand(player, "mapart.credit", "mapart credit " + player.getName(), "set creator names");
            case ITEM_FRAME -> executeCommand(player, "mapart.lock", "mapart lock", "lock maps");
            case FILLED_MAP -> executeCommand(player, "mapart.unlock", "mapart unlock", "unlock maps");
            case BARRIER -> player.closeInventory();
            case LIME_STAINED_GLASS_PANE, RED_STAINED_GLASS_PANE -> handleToggle(player, displayName, map);
            default -> {}
        }
    }

    private void handleInput(Player player, String permission, InputManager.InputType type, ItemStack map, String action) {
        if (player.hasPermission(permission)) {
            InputManager.ask(player, type, map);
        } else {
            player.sendMessage("§cYou don’t have permission to " + action + ".");
        }
    }

    private void executeCommand(Player player, String permission, String command, String action) {
        if (player.hasPermission(permission)) {
            player.performCommand(command);
        } else {
            player.sendMessage("§cYou don’t have permission to " + action + ".");
        }
    }

    private void handleToggle(Player player, String displayName, ItemStack map) {
        if (!(map.getItemMeta() instanceof MapMeta meta)) return;
        boolean metaChanged = false;

        // Toggle Map Name Visibility
        if (displayName.contains("Map Name")) {
            if (!player.hasPermission("mapart.toggle.displayname")) {
                player.sendMessage("§cYou don’t have permission to toggle the map name.");
                return;
            }

            boolean nameShown = meta.hasDisplayName() && meta.displayName() != null;
            if (nameShown) {
                meta.setDisplayName(null);
                meta.getPersistentDataContainer().set(LockUtil.MAPART_NAME_VISIBLE_KEY, PersistentDataType.BYTE, (byte) 0);
                player.sendMessage("§7Map name §chidden§7.");
            } else {
                meta.setDisplayName("§fUntitled Map");
                meta.getPersistentDataContainer().set(LockUtil.MAPART_NAME_KEY, PersistentDataType.BYTE, (byte) 1);
                player.sendMessage("§aMap name §ashown§7.");
            }

            metaChanged = true;
        }

        // Toggle Creator Hologram
        if (displayName.contains("Creator Hologram")) {
            if (!player.hasPermission("mapart.toggle.hologram")) {
                player.sendMessage("§cYou don’t have permission to toggle the creator tag.");
                return;
            }

            byte current = meta.getPersistentDataContainer().getOrDefault(
                    LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE, (byte) 1);
            byte newState = current == 1 ? (byte) 0 : (byte) 1;

            meta.getPersistentDataContainer().set(LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE, newState);
            player.sendMessage("§7Creator tag " + (newState == 1 ? "§ashown§7." : "§chidden§7."));
            metaChanged = true;
        }

        if (metaChanged) {
            map.setItemMeta(meta);
            LoreUtil.updateMapLore(map);
            player.getInventory().setItemInMainHand(map);
            MapArtGUI.open(player, map); // Re-open GUI to reflect toggle visually
        }
    }

}

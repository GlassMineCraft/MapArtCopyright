package net.glassmc.mapartcopyright.gui;

import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.util.CreditUtil;
import net.glassmc.mapartcopyright.util.LockUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.Optional;

public class MapArtGUI {

    public static final String GUI_TITLE = "§8MapArt Manager";

    public static void open(Player player, ItemStack mapItem) {
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) gui.setItem(i, filler);

        gui.setItem(0, createItem(Material.ANVIL, "§eRename Map", "§7Click to enter a new map display name."));
        gui.setItem(1, createItem(Material.PAPER, "§fMap Name: " + MapArtAPI.getStoredMapName(mapItem).orElse("§7No name set")));
        gui.setItem(8, createItem(Material.WRITABLE_BOOK, "§eChange Creator Name", "§7Click to enter a custom creator name."));

        // Credit head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        String credit = CreditUtil.getCredit(mapItem);
        if (credit != null) {
            OfflinePlayer credited = Bukkit.getOfflinePlayer(credit);
            headMeta.setOwningPlayer(credited);
            headMeta.setDisplayName("§bCreator: §f" + credit);
        } else {
            headMeta.setDisplayName("§7No creator set");
        }
        headMeta.setLore(Collections.singletonList("§7Click to set yourself as creator"));
        head.setItemMeta(headMeta);
        gui.setItem(20, head);

        gui.setItem(22, createItem(Material.ITEM_FRAME, "§cLock Map", "§7Click to lock this map"));
        gui.setItem(24, createItem(Material.FILLED_MAP, "§aUnlock Map", "§7Click to unlock this map"));

        // Get meta to read toggle states
        MapMeta meta = (MapMeta) mapItem.getItemMeta();

        // Toggle Item Frame Lock
        byte frameLockState = meta.getPersistentDataContainer().getOrDefault(
            LockUtil.ITEMFRAME_LOCK_KEY, PersistentDataType.BYTE, (byte) 0);
        boolean frameLocked = frameLockState == 1;
        Material frameLockMaterial = frameLocked ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
        String frameLockText = "§eToggle Item Frame Lock";
        String frameLockLore = "§7Click to " + (frameLocked ? "§aUnlock" : "§cLock") + " item frames";
        gui.setItem(23, createItem(frameLockMaterial, frameLockText, frameLockLore));

        // Toggle Map Name Visibility
        boolean nameVisible = meta.hasDisplayName();
        Material nameMaterial = nameVisible ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String nameToggleText = "§eToggle Map Name";
        String nameToggleLore = "§7Click to " + (nameVisible ? "§cHide" : "§aShow") + " the map name";
        gui.setItem(33, createItem(nameMaterial, nameToggleText, nameToggleLore));

        // Toggle Creator Hologram
        byte holoState = meta.getPersistentDataContainer().getOrDefault(
            LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE, (byte) 1);
        boolean hologramVisible = holoState == 1;
        Material holoMaterial = hologramVisible ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String holoToggleText = "§eToggle Creator Hologram";
        String holoToggleLore = "§7Click to " + (hologramVisible ? "§cHide" : "§aShow") + " creator tag below frame";
        gui.setItem(29, createItem(holoMaterial, holoToggleText, holoToggleLore));

        gui.setItem(40, createItem(Material.BARRIER, "§cClose Menu", "§7Click to exit"));

        player.openInventory(gui);
    }


    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Collections.singletonList(lore[0]));
        item.setItemMeta(meta);
        return item;
    }
}

package net.glassmc.mapartcopyright.gui;

import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.util.CreditUtil;
import net.glassmc.mapartcopyright.util.LockUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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

import java.util.ArrayList;
import java.util.List;

public class MapArtGUI {

    public static final String GUI_TITLE = "§8MapArt Manager";

    /*
     * New layout (45 slots, 5 rows × 9 cols):
     *
     * Row 0:  [ANVIL:Rename] [PAPER:MapName] [NAME_TAG:ToggleMapName] [fill] [lbl] [lbl] [fill] [fill] [BOOK:Creator]
     *          0              1               2                         3      4     5      6      7      8
     *
     * Row 1:  [status×9]  — full-width lock status bar (green=unlocked, red=locked)
     *          9-17
     *
     * Row 2:  [fill] [fill] [HEAD:Creator] [lbl:Lock] [MAP:LockMap] [fill] [MAP:UnlockMap] [fill] [fill]
     *          18     19     20             21          22             23     24              25     26
     *
     * Row 3:  [fill] [fill] [lbl:Toggles] [lbl:Toggles] [ITEM_FRAME:FrameLockIcon] [fill] [ENDER_EYE:HoloIcon] [NAME_TAG:NameIcon] [fill]
     *          27     28     29(sectionL)  30(sectionL)  was wrong—
     *
     * Corrected Row 3:
     *  27:fill 28:lbl 29:lbl 30:ITEM_FRAME 31:fill 32:ENDER_EYE 33:NAME_TAG 34:fill 35:fill
     *
     * Row 4:  [fill×3] [LIME/RED:FramePane] [fill] [LIME/RED:HoloPane] [LIME/RED:NamePane] [fill] [BARRIER:Close]
     *          36 37 38  39                  40      41                  42                   43     44
     *
     * Icon → Pane (icon slot + 9):
     *   Item Frame Lock : icon=30, pane=39
     *   Creator Hologram: icon=32, pane=41
     *   Map Name        : icon=33, pane=42  (also accessible via slot 2 in row 0)
     */
    public static void open(Player player, ItemStack mapItem) {
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < 45; i++) gui.setItem(i, filler);

        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        boolean locked      = MapArtAPI.isLocked(mapItem);
        boolean frameLocked = meta.getPersistentDataContainer()
                .getOrDefault(LockUtil.ITEMFRAME_LOCK_KEY, PersistentDataType.BYTE, (byte) 0) == 1;
        boolean holoVisible = meta.getPersistentDataContainer()
                .getOrDefault(LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE, (byte) 1) == 1;
        boolean nameVisible = meta.hasDisplayName() && meta.displayName() != null;
        String  mapUUID     = MapArtAPI.getMapUUID(mapItem);
        String  credit      = CreditUtil.getCredit(mapItem);

        // ── Row 0: Display section ─────────────────────────────────────────────

        // Slot 0 — Rename Map
        List<Component> renameLore = lore(
                txt("Click to enter a new map display name.", NamedTextColor.GRAY));
        if (!player.hasPermission("mapart.rename")) renameLore.add(noPerms());
        gui.setItem(0, item(Material.ANVIL, txt("Rename Map", NamedTextColor.YELLOW), renameLore));

        // Slot 1 — Map name display + UUID info
        String displayedName = MapArtAPI.getStoredMapName(mapItem).orElse(null);
        List<Component> nameLore = lore(
                Component.text("UUID: ", NamedTextColor.DARK_GRAY)
                        .append(txt(mapUUID != null ? mapUUID : "None", NamedTextColor.GRAY)));
        gui.setItem(1, item(Material.PAPER,
                Component.text("Map Name: ", NamedTextColor.GRAY)
                        .append(displayedName != null
                                ? txt(displayedName, NamedTextColor.WHITE)
                                : txt("No name set", NamedTextColor.DARK_GRAY)),
                nameLore));

        // Slot 2 — Toggle Map Name (shortcut at top, mirrored in row 3 slot 33)
        gui.setItem(2, buildNameToggle(player, nameVisible));

        // Slots 4–5 — Section label
        ItemStack displayLbl = sectionLabel("Display");
        gui.setItem(4, displayLbl);
        gui.setItem(5, displayLbl);

        // Slot 8 — Change Creator Name
        List<Component> creatorInputLore = lore(
                txt("Click to enter a custom creator name.", NamedTextColor.GRAY));
        if (!player.hasPermission("mapart.credit")) creatorInputLore.add(noPerms());
        gui.setItem(8, item(Material.WRITABLE_BOOK, txt("Change Creator Name", NamedTextColor.YELLOW), creatorInputLore));

        // ── Row 1: Lock status bar ─────────────────────────────────────────────
        Material statusMat  = locked ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
        Component statusName = locked
                ? txt("● Map Locked",   NamedTextColor.RED)
                : txt("● Map Unlocked", NamedTextColor.GREEN);
        ItemStack statusPane = item(statusMat, statusName);
        for (int i = 9; i <= 17; i++) gui.setItem(i, statusPane);

        // ── Row 2: Creator + Lock controls ────────────────────────────────────

        // Slot 20 — Creator head
        ItemStack head     = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (credit != null) {
            OfflinePlayer creditedPlayer = Bukkit.getOfflinePlayer(credit);
            headMeta.setOwningPlayer(creditedPlayer);
            headMeta.displayName(Component.text("Creator: ", NamedTextColor.AQUA)
                    .append(txt(credit, NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            headMeta.displayName(txt("No creator set", NamedTextColor.GRAY));
        }
        headMeta.lore(lore(txt("Click to set yourself as creator.", NamedTextColor.GRAY)));
        head.setItemMeta(headMeta);
        gui.setItem(20, head);

        // Slot 21 — Section label
        gui.setItem(21, sectionLabel("Lock"));

        // Slot 22 — Lock Map (FILLED_MAP)
        List<Component> lockLore = lore(txt("Click to lock this map.", NamedTextColor.GRAY));
        if (!player.hasPermission("mapart.lock")) lockLore.add(noPerms());
        gui.setItem(22, item(Material.FILLED_MAP, txt("Lock Map", NamedTextColor.RED), lockLore));

        // Slot 24 — Unlock Map (FILLED_MAP)
        List<Component> unlockLore = lore(txt("Click to unlock this map.", NamedTextColor.GRAY));
        if (!player.hasPermission("mapart.unlock")) unlockLore.add(noPerms());
        gui.setItem(24, item(Material.FILLED_MAP, txt("Unlock Map", NamedTextColor.GREEN), unlockLore));

        // ── Row 3: Toggle icons ────────────────────────────────────────────────

        // Slots 27-28 — Section labels
        ItemStack toggleLbl = sectionLabel("Toggles");
        gui.setItem(27, toggleLbl);
        gui.setItem(28, toggleLbl);

        // Slot 30 — Item Frame Lock icon (ITEM_FRAME)
        List<Component> frameLockIconLore = lore(
                Component.text("Item frame lock: ", NamedTextColor.GRAY)
                        .append(frameLocked ? txt("Enabled", NamedTextColor.GREEN) : txt("Disabled", NamedTextColor.RED)),
                txt("Click to toggle.", NamedTextColor.GRAY));
        if (!player.hasPermission("mapart.toggle.itemframe")) frameLockIconLore.add(noPerms());
        gui.setItem(30, item(Material.ITEM_FRAME, txt("Toggle Item Frame Lock", NamedTextColor.YELLOW), frameLockIconLore));

        // Slot 32 — Creator Hologram icon (ENDER_EYE)
        List<Component> holoIconLore = lore(
                Component.text("Creator hologram: ", NamedTextColor.GRAY)
                        .append(holoVisible ? txt("Visible", NamedTextColor.GREEN) : txt("Hidden", NamedTextColor.RED)),
                txt("Click to toggle.", NamedTextColor.GRAY));
        if (!player.hasPermission("mapart.toggle.hologram")) holoIconLore.add(noPerms());
        gui.setItem(32, item(Material.ENDER_EYE, txt("Toggle Creator Hologram", NamedTextColor.YELLOW), holoIconLore));

        // Slot 33 — Map Name icon (NAME_TAG, mirrors slot 2)
        gui.setItem(33, buildNameToggle(player, nameVisible));

        // ── Row 4: Toggle state panes + Close ─────────────────────────────────

        // Slot 39 — Item Frame Lock state pane (below icon at slot 30)
        Material framePaneMat = frameLocked ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        gui.setItem(39, item(framePaneMat, txt("Toggle Item Frame Lock", NamedTextColor.YELLOW),
                lore(txt(frameLocked ? "Click to disable." : "Click to enable.", NamedTextColor.GRAY))));

        // Slot 41 — Hologram state pane (below icon at slot 32)
        Material holoPaneMat = holoVisible ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        gui.setItem(41, item(holoPaneMat, txt("Toggle Creator Hologram", NamedTextColor.YELLOW),
                lore(txt(holoVisible ? "Click to hide." : "Click to show.", NamedTextColor.GRAY))));

        // Slot 42 — Map Name state pane (below icon at slot 33)
        Material namePaneMat = nameVisible ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        gui.setItem(42, item(namePaneMat, txt("Toggle Map Name", NamedTextColor.YELLOW),
                lore(txt(nameVisible ? "Click to hide." : "Click to show.", NamedTextColor.GRAY))));

        // Slot 44 — Close
        gui.setItem(44, item(Material.BARRIER, txt("Close Menu", NamedTextColor.RED),
                lore(txt("Click to exit.", NamedTextColor.GRAY))));

        player.openInventory(gui);
    }

    /** Builds the NAME_TAG toggle item for map name visibility (used in slots 2 and 33). */
    private static ItemStack buildNameToggle(Player player, boolean nameVisible) {
        List<Component> lore = lore(
                Component.text("Map name: ", NamedTextColor.GRAY)
                        .append(nameVisible ? txt("Visible", NamedTextColor.GREEN) : txt("Hidden", NamedTextColor.RED)),
                txt("Click to toggle.", NamedTextColor.GRAY));
        if (!player.hasPermission("mapart.toggle.displayname")) lore.add(noPerms());
        return item(Material.NAME_TAG, txt("Toggle Map Name", NamedTextColor.YELLOW), lore);
    }

    // ── Item-building helpers ──────────────────────────────────────────────────

    /** Creates a Component text with no italic decoration. */
    private static Component txt(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /** Creates a plain item with a display name and no lore. */
    private static ItemStack item(Material material, Component name) {
        return item(material, name, List.of());
    }

    /** Creates an item with a display name and lore list. */
    private static ItemStack item(Material material, Component name, List<Component> loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        if (!loreLines.isEmpty()) meta.lore(loreLines);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Builds a lore list, stripping italic from each line. */
    private static List<Component> lore(Component... lines) {
        List<Component> list = new ArrayList<>();
        for (Component line : lines) {
            list.add(line.decoration(TextDecoration.ITALIC, false));
        }
        return list;
    }

    /** Standard "(No Permission)" lore append. */
    private static Component noPerms() {
        return Component.text("(No Permission)", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false);
    }

    /** Non-interactive section-header filler pane. */
    private static ItemStack sectionLabel(String label) {
        return item(Material.GRAY_STAINED_GLASS_PANE,
                Component.text("── " + label + " ──", NamedTextColor.DARK_GRAY));
    }
}

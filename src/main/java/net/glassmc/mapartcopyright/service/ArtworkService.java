package net.glassmc.mapartcopyright.service;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.Audit.AuditLogger;
import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.artwork.*;
import net.glassmc.mapartcopyright.database.*;
import net.glassmc.mapartcopyright.economy.EconomyUtil;
import net.glassmc.mapartcopyright.listeners.MapFrameListener;
import net.glassmc.mapartcopyright.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public final class ArtworkService {
    private ArtworkService() {}
    private record FrameEdit(ItemFrame frame, ItemStack before, ItemStack after) {}
    private static long lastSyncWarning;

    public static ArtworkMembership find(ItemStack item) throws SQLException {
        String id = MapArtAPI.getMapUUID(item);
        if (id == null) {
            if (item != null && item.getItemMeta() instanceof MapMeta meta && meta.getPersistentDataContainer().has(LockUtil.ARTWORK_ID_KEY))
                throw new SQLException("Artwork tile has no map UUID; restore its registration");
            return null;
        }
        var membership = ArtworkDatabase.membership(id);
        if (membership == null && item.getItemMeta().getPersistentDataContainer().has(LockUtil.ARTWORK_ID_KEY))
            throw new SQLException("Artwork registration is missing; restore the ownership database");
        return membership;
    }

    /** Copy-safe synchronization: modify the supplied stack, never manufacture another item. */
    public static boolean refresh(ItemStack item) throws SQLException {
        MapArtService.requireMainThread();
        var membership = find(item);
        if (membership == null) return false;
        MapMeta before = (MapMeta) item.getItemMeta();
        MapMeta next = before.clone();
        ArtworkMetadata.apply(next, membership);
        if (before.equals(next)) return false;
        if (!item.setItemMeta(next)) throw new IllegalStateException("Unable to render artwork metadata");
        return true;
    }

    public static boolean create(Player player, ItemFrame anchor, ArtworkSize size, String rawName) {
        MapArtService.requireMainThread();
        for (String permission : List.of("mapart.use", "mapart.wall", "mapart.lock", "mapart.rename", "mapart.credit")) {
            if (!player.hasPermission(permission)) { Messages.send(player, "no-permission", "§cYou don't have permission to do that."); return false; }
        }
        EconomyUtil.Receipt receipt = null;
        List<FrameEdit> applied = new ArrayList<>();
        String operation = UUID.randomUUID().toString();
        ArtworkRecord artwork;
        List<ArtworkTile> tiles = new ArrayList<>();
        List<FrameEdit> edits = new ArrayList<>();
        try {
            if (!OwnershipDatabase.isConnected()) throw new SQLException("Ownership database is unavailable");
            var frames = WallSelection.collect(anchor, size);
            Map<String, MapRecord> previous = new HashMap<>();
            Set<String> ids = new HashSet<>();
            List<ItemStack> originals = new ArrayList<>();
            int toLock = 0;
            for (int i = 0; i < frames.size(); i++) {
                ItemStack item = frames.get(i).getItem().clone();
                if (find(item) != null) throw new IllegalArgumentException("A tile already belongs to an artwork. Existing artworks cannot be combined.");
                String id = MapArtAPI.getMapUUID(item);
                if (id != null) {
                    MapRecord owner = OwnershipDatabase.find(id);
                    if (owner == null) throw new SQLException("A tile has no ownership record; restore the database");
                    if (!owner.playerUUID.equals(player.getUniqueId())) throw new IllegalArgumentException("Every registered tile must belong to you.");
                    previous.put(id, owner);
                } else {
                    if (MapArtAPI.isLocked(item)) throw new IllegalArgumentException("A protected tile is missing its map UUID.");
                    id = UUID.randomUUID().toString();
                }
                if (!ids.add(id)) throw new IllegalArgumentException("The wall repeats a registered tile. Each position needs its own map UUID.");
                if (!MapArtAPI.isLocked(item)) toLock++;
                originals.add(item);
                tiles.add(new ArtworkTile(id, i % size.width(), i / size.width()));
            }
            MapMeta first = (MapMeta) originals.getFirst().getItemMeta();
            Component name = rawName == null || rawName.isBlank() ? MapMetadata.name(first) : StringSanitizer.parseComponent(rawName, 32);
            if (PlainTextComponentSerializer.plainText().serialize(name).isBlank()) throw new IllegalArgumentException("Enter a nonempty artwork title.");
            if (MapArtCopyright.getInstance().getConfig().getBoolean("settings.require-display-name", false)
                    && (rawName == null || rawName.isBlank()) && !first.hasDisplayName())
                throw new IllegalArgumentException("Supply an artwork title before registering the wall.");
            String credit = CreditUtil.getCredit(originals.getFirst());
            artwork = new ArtworkRecord(UUID.randomUUID(), player.getUniqueId(), size, MapMetadata.MINI.serialize(name),
                    credit == null ? player.getName() : credit, true, true,
                    MapArtCopyright.getInstance().getConfig().getBoolean("settings.default-hologram-visible", true), true, 0);
            for (int i = 0; i < tiles.size(); i++) {
                ItemStack next = originals.get(i).clone();
                MapMeta meta = (MapMeta) next.getItemMeta();
                if (!meta.getPersistentDataContainer().has(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING))
                    meta.getPersistentDataContainer().set(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
                ArtworkMetadata.apply(meta, new ArtworkMembership(artwork, tiles.get(i)));
                if (!next.setItemMeta(meta)) throw new IllegalStateException("Unable to prepare artwork tile");
                edits.add(new FrameEdit(frames.get(i), originals.get(i), next));
            }
            receipt = EconomyUtil.withdraw(player, "lock", toLock);
            // A provider callback may have changed a frame while taking payment.
            if (!WallSelection.collect(anchor, size).equals(frames)) throw new IllegalStateException("The wall moved; try again.");
            for (var edit : edits)
                if (!edit.frame().getItem().equals(edit.before())) throw new IllegalStateException("A wall tile changed; try again.");
            ArtworkDatabase.create(artwork, tiles, previous, () -> {
                for (var edit : edits) {
                    applied.add(edit);
                    edit.frame().setItem(edit.after(), false);
                }
            });
        } catch (SQLException | RuntimeException ex) {
            for (var edit : applied) {
                try {
                    if (edit.frame().isValid() && edit.frame().getItem().equals(edit.after())) edit.frame().setItem(edit.before(), false);
                } catch (RuntimeException restore) { MapArtCopyright.getInstance().getLogger().severe("Wall item recovery needed, operation=" + operation); }
            }
            EconomyUtil.refund(player, receipt, operation);
            MapArtCopyright.getInstance().getLogger().warning("Wall registration failed " + operation + ": " + ex.getMessage());
            player.sendMessage("§c" + (ex instanceof SQLException ? "The artwork could not be saved. Nothing was registered." : ex.getMessage()));
            return false;
        }
        audit("artwork_created", player, artwork, tiles, operation);
        refreshLoaded(artwork, tiles);
        // Also render the selected frames when they are not in a world's general loaded-entity list yet.
        for (var edit : edits) refreshFrameSafely(edit.frame());
        player.sendMessage("§aRegistered " + size + " artwork (" + size.tiles() + " tiles). UUID: §f" + artwork.id());
        player.sendMessage("§7Hold any tile to manage the whole artwork with /mapart menu.");
        return true;
    }

    /** Called after the ordinary action permission/ownership checks in MapArtService. */
    static boolean change(Player player, ItemStack item, ArtworkMembership membership, String action,
                          String fee, Consumer<MapMeta> edit) {
        var previous = membership.artwork();
        if (!previous.owner().equals(player.getUniqueId()) && !player.hasPermission("mapart.bypass")) return false;
        EconomyUtil.Receipt receipt = null;
        MapMeta original = (MapMeta) item.getItemMeta();
        MapMeta next = original.clone();
        boolean[] applied = {false};
        String operation = UUID.randomUUID().toString();
        ArtworkRecord updated;
        List<ArtworkTile> tiles;
        try {
            ArtworkMetadata.apply(next, membership);
            edit.accept(next);
            updated = ArtworkMetadata.edited(previous, next);
            ArtworkMetadata.apply(next, new ArtworkMembership(updated, membership.tile()));
            tiles = ArtworkDatabase.tiles(previous.id());
            if (fee != null) receipt = EconomyUtil.withdraw(player, fee, previous.size().tiles());
            if (!item.getItemMeta().equals(original)) throw new IllegalStateException("The tile changed; try again.");
            ArtworkDatabase.update(previous, updated, () -> {
                applied[0] = true;
                if (!item.setItemMeta(next)) throw new IllegalStateException("The tile could not be updated.");
            });
        } catch (SQLException | RuntimeException ex) {
            if (applied[0] && item.getItemMeta().equals(next)) item.setItemMeta(original);
            EconomyUtil.refund(player, receipt, operation);
            MapArtCopyright.getInstance().getLogger().warning("Artwork operation failed " + operation + ": " + ex.getMessage());
            player.sendMessage("§cThe artwork could not be updated. Check ownership and database availability.");
            return false;
        }
        audit("artwork_" + action, player, updated, tiles, operation);
        refreshLoaded(updated, tiles);
        player.sendMessage("§7Updated all " + previous.size().tiles() + " tiles and their registered copies.");
        return true;
    }

    private static void audit(String action, Player player, ArtworkRecord artwork, List<ArtworkTile> tiles, String operation) {
        String details = "artwork=" + artwork.id() + " layout=" + artwork.size() + " operation=" + operation;
        AuditLogger.log(action, player, artwork.id().toString(), details);
        for (var tile : tiles) AuditLogger.log(action, player, tile.mapId(), details + " tile=" + (tile.x() + 1) + "," + (tile.y() + 1));
    }

    public static void refreshInventory(Inventory inventory) throws SQLException {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && refresh(item)) inventory.setItem(slot, item);
        }
    }

    public static void refreshInventorySafely(Inventory inventory) {
        if (inventory == null) return;
        try { refreshInventory(inventory); } catch (SQLException | RuntimeException ex) { warnSync(ex); }
    }

    private static void refreshLoaded(ArtworkRecord artwork, List<ArtworkTile> tiles) {
        Set<String> ids = new HashSet<>();
        tiles.forEach(tile -> ids.add(tile.mapId()));
        try {
            Set<Inventory> inventories = new HashSet<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                inventories.add(player.getInventory());
                if (player.getOpenInventory().getTopInventory() != null) inventories.add(player.getOpenInventory().getTopInventory());
                ItemStack cursor = player.getItemOnCursor();
                if (ids.contains(MapArtAPI.getMapUUID(cursor)) && refresh(cursor)) player.setItemOnCursor(cursor);
            }
            for (Inventory inventory : inventories) {
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    ItemStack stack = inventory.getItem(slot);
                    if (ids.contains(MapArtAPI.getMapUUID(stack)) && refresh(stack)) inventory.setItem(slot, stack);
                }
            }
            for (var world : Bukkit.getWorlds()) {
                for (var entity : world.getEntities()) {
                    if (entity instanceof ItemFrame frame && ids.contains(MapArtAPI.getMapUUID(frame.getItem()))) MapFrameListener.refresh(frame);
                    else if (entity instanceof Item dropped && ids.contains(MapArtAPI.getMapUUID(dropped.getItemStack()))) {
                        ItemStack stack = dropped.getItemStack();
                        if (refresh(stack)) dropped.setItemStack(stack);
                    }
                }
            }
        } catch (SQLException | RuntimeException ex) {
            // The database commit succeeded. Refresh failures must not undo it or issue a refund.
            warnSync(ex);
        }
    }

    private static void refreshFrameSafely(ItemFrame frame) {
        try { MapFrameListener.refresh(frame); } catch (RuntimeException ex) { warnSync(ex); }
    }

    public static void warnSync(Exception ex) {
        long now = System.nanoTime();
        if (now - lastSyncWarning > 30_000_000_000L) {
            lastSyncWarning = now;
            MapArtCopyright.getInstance().getLogger().warning("Artwork display refresh deferred: " + ex.getMessage());
        }
    }
}

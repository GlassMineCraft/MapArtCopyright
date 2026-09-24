package net.glassmc.mapartcopyright.service;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.Audit.AuditLogger;
import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.database.*;
import net.glassmc.mapartcopyright.economy.EconomyUtil;
import net.glassmc.mapartcopyright.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

/** All user-facing mutations pass through ownership, persistence and payment checks here. */
public final class MapArtService {
    private MapArtService() {}

    public static boolean lock(Player player, ItemStack item) {
        if (!authorize(player, item, "mapart.lock")) return false;
        if (MapArtAPI.isLocked(item)) {
            player.sendMessage("§eThis map is already locked; no fee was charged.");
            return false;
        }
        MapMeta meta = (MapMeta) item.getItemMeta();
        if (requireName() && (!meta.hasDisplayName()
                || PlainTextComponentSerializer.plainText().serialize(meta.displayName()).isBlank())) {
            player.sendMessage("§cName the map before locking it.");
            return false;
        }
        boolean changed = change(player, item, "locked", "lock", next -> {
            var data = next.getPersistentDataContainer();
            if (!data.has(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING)) {
                data.set(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
                data.set(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
            }
            data.set(LockUtil.LOCK_KEY, PersistentDataType.BYTE, (byte) 1);
            if (!data.has(LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE)) {
                data.set(LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE,
                        (byte) (MapArtCopyright.getInstance().getConfig().getBoolean("settings.default-hologram-visible", true) ? 1 : 0));
            }
        });
        if (changed) Messages.send(player, "locked", "§aMap locked successfully.");
        return changed;
    }

    public static boolean unlock(Player player, ItemStack item) {
        if (!authorize(player, item, "mapart.unlock")) return false;
        if (!MapArtAPI.isLocked(item)) {
            player.sendMessage("§eThis map is already unlocked; no fee was charged.");
            return false;
        }
        boolean changed = change(player, item, "unlocked", "unlock",
                meta -> meta.getPersistentDataContainer().remove(LockUtil.LOCK_KEY));
        if (changed) Messages.send(player, "unlocked", "§aMap unlocked successfully.");
        return changed;
    }

    public static boolean rename(Player player, ItemStack item, String input) {
        if (!authorize(player, item, "mapart.rename")) return false;
        try {
            Component name = StringSanitizer.parseComponent(input, 32);
            if (PlainTextComponentSerializer.plainText().serialize(name).isBlank()) {
                player.sendMessage("§cEnter a nonempty map name.");
                return false;
            }
            boolean changed = change(player, item, "renamed", null, meta -> MapMetadata.setName(meta, name));
            if (changed) Messages.send(player, "renamed", "§aMap renamed to: §f{input}", MapMetadata.LEGACY.serialize(name));
            return changed;
        } catch (IllegalArgumentException ex) {
            player.sendMessage("§c" + ex.getMessage());
            return false;
        }
    }

    public static boolean credit(Player player, ItemStack item, String input) {
        if (!authorize(player, item, "mapart.credit")) return false;
        try {
            Component credit = StringSanitizer.parseComponent(input, 16);
            if (PlainTextComponentSerializer.plainText().serialize(credit).isBlank()) {
                player.sendMessage("§cEnter a nonempty creator credit.");
                return false;
            }
            String stored = MapMetadata.LEGACY.serialize(credit);
            boolean changed = change(player, item, "credited", null,
                    meta -> meta.getPersistentDataContainer().set(LockUtil.CREDIT_KEY, PersistentDataType.STRING, stored));
            if (changed) Messages.send(player, "credited", "§aCreator set to: §f{input}", stored);
            return changed;
        } catch (IllegalArgumentException ex) {
            player.sendMessage("§c" + ex.getMessage());
            return false;
        }
    }

    public static boolean toggle(Player player, ItemStack item, NamespacedKey key, String permission) {
        if (!authorize(player, item, permission)) return false;
        if (!key.equals(LockUtil.MAPART_NAME_VISIBLE_KEY) && !key.equals(LockUtil.HOLOGRAM_VISIBLE_KEY)
                && !key.equals(LockUtil.ITEMFRAME_LOCK_KEY)) throw new IllegalArgumentException("Unsupported toggle");
        if (key.equals(LockUtil.ITEMFRAME_LOCK_KEY) && MapArtAPI.getMapUUID(item) == null) {
            player.sendMessage("§cRegister the map with /mapart lock before protecting a frame.");
            return false;
        }
        MapMeta current = (MapMeta) item.getItemMeta();
        byte fallback = key.equals(LockUtil.ITEMFRAME_LOCK_KEY) ? (byte) 0 : (byte) 1;
        if (key.equals(LockUtil.HOLOGRAM_VISIBLE_KEY)) fallback = (byte)
                (MapArtCopyright.getInstance().getConfig().getBoolean("settings.default-hologram-visible", true) ? 1 : 0);
        boolean enabled = current.getPersistentDataContainer().getOrDefault(key, PersistentDataType.BYTE, fallback) != 1;
        if (key.equals(LockUtil.MAPART_NAME_VISIBLE_KEY) && !enabled && requireName()) {
            player.sendMessage("§cMap names must remain visible on this server.");
            return false;
        }
        boolean changed = change(player, item, "toggled_" + key.getKey(), null,
                meta -> meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) (enabled ? 1 : 0)));
        if (changed) {
            if (key.equals(LockUtil.MAPART_NAME_VISIBLE_KEY))
                Messages.send(player, "toggled-displayname-" + (enabled ? "on" : "off"), enabled ? "§aMap name shown." : "§7Map name hidden.");
            else if (key.equals(LockUtil.HOLOGRAM_VISIBLE_KEY))
                Messages.send(player, "toggled-hologram-" + (enabled ? "on" : "off"), enabled ? "§aCreator tag shown." : "§7Creator tag hidden.");
            else player.sendMessage("§aItem frame protection " + (enabled ? "enabled." : "disabled."));
        }
        return changed;
    }

    public static boolean authorize(Player player, ItemStack item, String permission) {
        requireMainThread();
        if (!player.hasPermission("mapart.use") || !player.hasPermission(permission)) {
            Messages.send(player, "no-permission", "§cYou don't have permission to do that.");
            return false;
        }
        if (item == null || !(item.getItemMeta() instanceof MapMeta)) {
            Messages.send(player, "map-required", "§cHold a filled map in your hand to do this.");
            return false;
        }
        if (!PermissionUtil.canModify(player, item)) return false;
        try { ArtworkService.refresh(item); }
        catch (SQLException ex) {
            player.sendMessage("§cArtwork ownership is unavailable. Try again after the database is restored.");
            return false;
        }
        return true;
    }

    private static MapRecord existing(ItemStack item) throws SQLException {
        String id = MapArtAPI.getMapUUID(item);
        if (id == null) return null;
        MapRecord result = OwnershipDatabase.find(id);
        if (result == null) throw new SQLException("Registered map has no ownership record; restore the ownership database");
        return result;
    }

    private static boolean change(Player player, ItemStack item, String action, String fee, Consumer<MapMeta> edit) {
        requireMainThread();
        EconomyUtil.Receipt receipt = null;
        MapRecord before = null;
        MapRecord after = null;
        boolean persisted = false;
        String operation = UUID.randomUUID().toString();
        try {
            var artwork = ArtworkService.find(item);
            if (artwork != null) return ArtworkService.change(player, item, artwork, action, fee, edit);
            before = existing(item);
            if (before != null && !before.playerUUID.equals(player.getUniqueId()) && !player.hasPermission("mapart.bypass"))
                throw new IllegalStateException("You are not the owner of this map.");
            MapMeta next = (MapMeta) item.getItemMeta();
            MapMetadata.initialize(next);
            edit.accept(next);
            MapMetadata.render(next);
            LoreUtil.apply(next);
            String id = next.getPersistentDataContainer().get(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING);
            if (id != null) {
                if (!OwnershipDatabase.isConnected()) throw new SQLException("Ownership database is unavailable");
                after = new MapRecord(id, before == null ? player.getUniqueId() : before.playerUUID,
                        MapMetadata.storedName(next), next.getPersistentDataContainer().get(LockUtil.CREDIT_KEY, PersistentDataType.STRING));
            }
            // Validate all item and ownership state before contacting the provider.
            if (fee != null) receipt = EconomyUtil.withdraw(player, fee);
            if (after != null) {
                if (before == null) OwnershipDatabase.register(after);
                else OwnershipDatabase.updateMetadata(after);
                persisted = true;
            }
            if (!item.setItemMeta(next)) throw new IllegalStateException("The item could not be updated.");
            AuditLogger.log(action, player, id, "operation=" + operation);
            return true;
        } catch (SQLException | RuntimeException ex) {
            if (persisted && after != null) {
                try {
                    if (before == null) OwnershipDatabase.rollbackRegistration(after.mapUUID, after.playerUUID);
                    else OwnershipDatabase.updateMetadata(before);
                } catch (SQLException rollback) {
                    MapArtCopyright.getInstance().getLogger().severe("RECONCILIATION REQUIRED operation=" + operation + ": " + rollback.getMessage());
                }
            }
            EconomyUtil.refund(player, receipt, operation);
            MapArtCopyright.getInstance().getLogger().warning("Map operation failed " + operation + ": " + ex.getMessage());
            player.sendMessage("§c" + (ex instanceof SQLException ? "Ownership could not be saved. The action was canceled." : ex.getMessage()));
            return false;
        }
    }

    /** Prepare an anvil result without changing the input or database. */
    public static ItemStack previewAnvilName(ItemStack input, String raw) {
        if (MapArtAPI.isArtworkTile(input)) throw new IllegalArgumentException("Use /mapart name to rename all artwork tiles together.");
        Component name = StringSanitizer.parseComponent(raw, 32);
        boolean empty = PlainTextComponentSerializer.plainText().serialize(name).isBlank();
        if (empty && requireName()) throw new IllegalArgumentException("Map names must remain visible on this server.");
        ItemStack result = input.clone();
        MapMeta meta = (MapMeta) result.getItemMeta();
        MapMetadata.initialize(meta);
        MapMetadata.setName(meta, name);
        meta.getPersistentDataContainer().set(LockUtil.MAPART_NAME_VISIBLE_KEY, PersistentDataType.BYTE, (byte) (empty ? 0 : 1));
        MapMetadata.render(meta);
        LoreUtil.apply(meta);
        result.setItemMeta(meta);
        return result;
    }

    /** Persist an authorized anvil preview before allowing the result to be taken. */
    public static boolean saveAnvilResult(Player player, ItemStack input, ItemStack result) {
        if (!authorize(player, input, "mapart.rename")) return false;
        if (MapArtAPI.isArtworkTile(input)) {
            player.sendMessage("§cUse /mapart name to rename all artwork tiles together.");
            return false;
        }
        try {
            MapRecord before = existing(input);
            if (before != null) {
                MapMeta next = (MapMeta) result.getItemMeta();
                OwnershipDatabase.updateMetadata(new MapRecord(before.mapUUID, before.playerUUID,
                        MapMetadata.storedName(next), next.getPersistentDataContainer().get(LockUtil.CREDIT_KEY, PersistentDataType.STRING)));
            }
            return true;
        } catch (SQLException ex) {
            player.sendMessage("§cOwnership could not be saved. The rename was canceled.");
            return false;
        }
    }

    private static boolean requireName() { return MapArtCopyright.getInstance().getConfig().getBoolean("settings.require-display-name", false); }
    public static void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Map mutations must run on the server thread");
    }
}

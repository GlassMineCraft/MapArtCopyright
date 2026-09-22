package net.glassmc.mapartcopyright.util;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.gui.MapArtGUI;
import net.glassmc.mapartcopyright.service.MapArtService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InputManager {
    public enum InputType { RENAME_MAP, SET_CREDIT }
    public record Session(InputType type, int slot, ItemStack expected, long expiresAt) {}
    private static final ConcurrentHashMap<UUID, Session> waiting = new ConcurrentHashMap<>();
    private InputManager() {}

    public static void ask(Player player, InputType type, ItemStack item) {
        MapArtService.requireMainThread();
        String permission = type == InputType.RENAME_MAP ? "mapart.rename" : "mapart.credit";
        if (!MapArtService.authorize(player, item, permission) || !MapArtGUI.canOpen(player)) return;
        int slot = player.getInventory().getHeldItemSlot();
        if (!item.equals(player.getInventory().getItem(slot))) return;
        int seconds = Math.max(5, Math.min(300, MapArtCopyright.getInstance().getConfig().getInt("settings.chat-timeout-seconds", 60)));
        Session session = new Session(type, slot, item.clone(), System.nanoTime() + seconds * 1_000_000_000L);
        waiting.put(player.getUniqueId(), session);
        player.closeInventory();
        player.sendMessage("§7Type the new " + (type == InputType.RENAME_MAP ? "map name" : "creator credit") + " in chat, or type cancel.");
        Bukkit.getScheduler().runTaskLater(MapArtCopyright.getInstance(), () -> {
            if (waiting.remove(player.getUniqueId(), session) && player.isOnline()) player.sendMessage("§7Map editing timed out.");
        }, seconds * 20L);
    }

    /** Safe from the async chat callback; only one message can consume a session. */
    public static Session claim(UUID player) { return waiting.remove(player); }
    public static boolean has(Player player) { return waiting.containsKey(player.getUniqueId()); }
    public static void clear(Player player) { waiting.remove(player.getUniqueId()); }
    public static void clearAll() { waiting.clear(); }
    public static void cancel(Player player) {
        if (waiting.remove(player.getUniqueId()) != null) player.sendMessage("§7Map editing canceled because your inventory changed.");
    }

    public static void complete(Player player, Session session, String input) {
        MapArtService.requireMainThread();
        if (!player.isOnline()) return;
        if (input.equalsIgnoreCase("cancel")) { player.sendMessage("§7Map editing canceled."); return; }
        ItemStack current = player.getInventory().getItem(session.slot());
        if (System.nanoTime() > session.expiresAt() || player.getInventory().getHeldItemSlot() != session.slot()
                || current == null || !current.equals(session.expected())) {
            player.sendMessage("§cThe map changed or moved. Open the menu again.");
            return;
        }
        if (!MapArtGUI.canOpen(player)) return;
        boolean changed = session.type() == InputType.RENAME_MAP
                ? MapArtService.rename(player, current, input) : MapArtService.credit(player, current, input);
        // Mutate the validated real stack. Never insert the stored comparison snapshot.
        if (changed) MapArtGUI.open(player, current);
    }

    public static InputType getType(Player player) {
        Session session = waiting.get(player.getUniqueId());
        return session == null ? null : session.type();
    }
    public static ItemStack getHeldMap(Player player) {
        Session session = waiting.get(player.getUniqueId());
        return session == null ? null : session.expected().clone();
    }
}

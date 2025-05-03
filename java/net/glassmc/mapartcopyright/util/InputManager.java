package net.glassmc.mapartcopyright.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class InputManager {

    public enum InputType { RENAME_MAP, SET_CREDIT }

    private static final HashMap<UUID, InputType> waiting = new HashMap<>();
    private static final HashMap<UUID, ItemStack> heldMap = new HashMap<>();

    public static void ask(Player player, InputType type, ItemStack map) {
        waiting.put(player.getUniqueId(), type);
        heldMap.put(player.getUniqueId(), map.clone());
        player.closeInventory();
        player.sendMessage("§7Please type your new " + (type == InputType.RENAME_MAP ? "map name" : "creator name") + " in chat.");
    }

    public static boolean has(Player player) {
        return waiting.containsKey(player.getUniqueId());
    }

    public static InputType getType(Player player) {
        return waiting.get(player.getUniqueId());
    }

    public static ItemStack getHeldMap(Player player) {
        return heldMap.get(player.getUniqueId());
    }

    public static void clear(Player player) {
        waiting.remove(player.getUniqueId());
        heldMap.remove(player.getUniqueId());
    }
}

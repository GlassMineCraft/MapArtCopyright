package net.glassmc.mapartcopyright.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

/** The snapshot is a comparison token, never an item to put back into inventory. */
public final class MapArtMenu implements InventoryHolder {
    private final UUID playerId;
    private final int heldSlot;
    private final ItemStack expected;
    private Inventory inventory;

    public MapArtMenu(Player player, ItemStack item) {
        playerId = player.getUniqueId();
        heldSlot = player.getInventory().getHeldItemSlot();
        expected = item.clone();
    }
    public void inventory(Inventory value) { inventory = value; }
    @Override public Inventory getInventory() { return inventory; }
    public boolean matches(Player player) {
        return playerId.equals(player.getUniqueId()) && player.getInventory().getHeldItemSlot() == heldSlot
                && expected.equals(player.getInventory().getItem(heldSlot));
    }
}

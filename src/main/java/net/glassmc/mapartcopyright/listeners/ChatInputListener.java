package net.glassmc.mapartcopyright.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.util.InputManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;

public class ChatInputListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        InputManager.Session session = InputManager.claim(player.getUniqueId());
        if (session == null) return;
        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(MapArtCopyright.getInstance(), () -> InputManager.complete(player, session, input));
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { InputManager.clear(event.getPlayer()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { InputManager.clear(event.getEntity()); }
    @EventHandler(ignoreCancelled = true) public void onDrop(PlayerDropItemEvent event) { InputManager.cancel(event.getPlayer()); }
    @EventHandler(ignoreCancelled = true) public void onSlot(PlayerItemHeldEvent event) { InputManager.cancel(event.getPlayer()); }
    @EventHandler(ignoreCancelled = true) public void onSwap(PlayerSwapHandItemsEvent event) { InputManager.cancel(event.getPlayer()); }
    @EventHandler(ignoreCancelled = true) public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) InputManager.cancel(player);
    }
    @EventHandler(ignoreCancelled = true) public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) InputManager.cancel(player);
    }
}

package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.gui.MapArtGUI;
import net.glassmc.mapartcopyright.util.InputManager;
import net.glassmc.mapartcopyright.util.LockUtil;
import net.glassmc.mapartcopyright.util.LoreUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class ChatInputListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!InputManager.has(player)) return;

        event.setCancelled(true);
        String input = ChatColor.translateAlternateColorCodes('&', event.getMessage().trim());
        ItemStack item = InputManager.getHeldMap(player);

        if (!(item.getItemMeta() instanceof MapMeta meta)) {
            player.sendMessage("§cError: no valid map found.");
            InputManager.clear(player);
            return;
        }

        switch (InputManager.getType(player)) {
            case RENAME_MAP -> {
                meta.setDisplayName(input);
                player.sendMessage("§aMap renamed to: §f" + input);
            }
            case SET_CREDIT -> {
                meta.getPersistentDataContainer().set(LockUtil.CREDIT_KEY, PersistentDataType.STRING, input);
                player.sendMessage("§aCreator set to: §f" + input);
            }
        }

        item.setItemMeta(meta);
        LoreUtil.updateMapLore(item);
        player.getInventory().setItemInMainHand(item);

        // Open GUI again on next tick (main thread)
        new BukkitRunnable() {
            @Override
            public void run() {
                MapArtGUI.open(player, item);
            }
        }.runTask(MapArtCopyright.getInstance());

        InputManager.clear(player);
    }
}

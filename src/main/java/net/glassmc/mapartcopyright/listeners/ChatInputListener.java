package net.glassmc.mapartcopyright.listeners;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.gui.MapArtGUI;
import net.glassmc.mapartcopyright.util.InputManager;
import net.glassmc.mapartcopyright.util.LockUtil;
import net.glassmc.mapartcopyright.util.LoreUtil;
import net.glassmc.mapartcopyright.util.PermissionUtil;
import net.glassmc.mapartcopyright.util.StringSanitizer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import io.papermc.paper.event.player.AsyncChatEvent;

public class ChatInputListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!InputManager.has(player)) return;

        event.setCancelled(true);

        // Extract and sanitize input from Adventure Component
        String rawInput = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        ItemStack item = InputManager.getHeldMap(player);
        if (!(item.getItemMeta() instanceof MapMeta meta)) {
            player.sendMessage(Component.text("Error: no valid map found.", NamedTextColor.RED));
            InputManager.clear(player);
            return;
        }

        switch (InputManager.getType(player)) {
            case RENAME_MAP -> {
                if (!PermissionUtil.canModify(player, item)) break;

                try {
                    Component name = StringSanitizer.parseComponent(rawInput, 32);
                    meta.displayName(name);
                    player.sendMessage(
                        Component.text("Map renamed to: ", NamedTextColor.GREEN)
                            .append(name)
                    );
                } catch (IllegalArgumentException ex) {
                    player.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
                    InputManager.clear(player);
                    return;
                }
            }

            case SET_CREDIT -> {
                if (!PermissionUtil.canModify(player, item, Component.text("You cannot set the creator on a locked map you do not own.", NamedTextColor.RED))) break;

                String sanitized = StringSanitizer.clean(rawInput, 16);
                meta.getPersistentDataContainer().set(LockUtil.CREDIT_KEY, PersistentDataType.STRING, sanitized);
                player.sendMessage(
                    Component.text("Creator set to: ", NamedTextColor.GREEN)
                        .append(Component.text(sanitized, NamedTextColor.WHITE))
                );
            }
        }

        item.setItemMeta(meta);
        LoreUtil.updateMapLore(item);
        player.getInventory().setItemInMainHand(item);

        new BukkitRunnable() {
            @Override
            public void run() {
                MapArtGUI.open(player, item);
            }
        }.runTask(MapArtCopyright.getInstance());

        InputManager.clear(player);
    }
}

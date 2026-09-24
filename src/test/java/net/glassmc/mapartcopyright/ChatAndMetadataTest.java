package net.glassmc.mapartcopyright;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import net.glassmc.mapartcopyright.listeners.ChatInputListener;
import net.glassmc.mapartcopyright.listeners.MapDropListener;
import net.glassmc.mapartcopyright.service.MapArtService;
import net.glassmc.mapartcopyright.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatAndMetadataTest extends PluginTestBase {
    @Test void movingMapDuringPromptCannotDuplicateItOrReplaceAnotherItem() {
        var item = map();
        InputManager.ask(owner, InputManager.InputType.RENAME_MAP, item);
        var session = InputManager.claim(owner.getUniqueId());
        assertNotNull(session);
        owner.getInventory().setItem(1, item);
        owner.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 7));
        InputManager.complete(owner, session, "Lost art");
        assertEquals(Material.DIAMOND, owner.getInventory().getItemInMainHand().getType());
        assertEquals(7, owner.getInventory().getItemInMainHand().getAmount());
        assertEquals(item, owner.getInventory().getItem(1));
        assertEquals(1, owner.getInventory().all(Material.FILLED_MAP).size());
        assertTrue(MapArtAPI.getStoredMapName(owner.getInventory().getItem(1)).isEmpty());
    }

    @Test void pendingReplyRechecksPermissions() {
        var item = map();
        InputManager.ask(owner, InputManager.InputType.RENAME_MAP, item);
        var session = InputManager.claim(owner.getUniqueId());
        owner.addAttachment(plugin, "mapart.rename", false);
        var before = item.clone();
        InputManager.complete(owner, session, "Denied");
        assertEquals(before, owner.getInventory().getItemInMainHand());
    }

    @Test void aSessionCanBeConsumedOnlyOnce() {
        InputManager.ask(owner, InputManager.InputType.RENAME_MAP, map());
        assertNotNull(InputManager.claim(owner.getUniqueId()));
        assertNull(InputManager.claim(owner.getUniqueId()));
    }

    @Test void cancelAndTimeoutLeaveTheActualStackUntouched() {
        var item = map();
        InputManager.ask(owner, InputManager.InputType.RENAME_MAP, item);
        var session = InputManager.claim(owner.getUniqueId());
        var before = item.clone();
        InputManager.complete(owner, session, "cancel");
        InputManager.complete(owner, new InputManager.Session(session.type(), session.slot(), session.expected(), 0), "Expired");
        assertEquals(before, owner.getInventory().getItemInMainHand());
    }

    @Test void asyncChatDefersInventoryMutationUntilServerTick() throws Exception {
        var item = map();
        InputManager.ask(owner, InputManager.InputType.RENAME_MAP, item);
        var event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(owner);
        when(event.message()).thenReturn(Component.text("New title"));
        var before = item.clone();
        CompletableFuture.runAsync(() -> new ChatInputListener().onChat(event)).get();
        assertEquals(before, owner.getInventory().getItemInMainHand());
        verify(event).setCancelled(true);
        server.getScheduler().performOneTick();
        assertEquals("New title", MapArtAPI.getStoredMapName(owner.getInventory().getItemInMainHand()).orElseThrow());
    }

    @Test void hideShowAndRenameKeepCanonicalTitleAndDatabaseInSync() throws Exception {
        var item = map();
        assertTrue(MapArtService.rename(owner, item, "<gradient:#ff0000:#0000ff>Gallery</gradient>"));
        assertTrue(MapArtService.lock(owner, item));
        String title = MapArtAPI.getMapName(item);
        assertTrue(MapArtService.toggle(owner, item, LockUtil.MAPART_NAME_VISIBLE_KEY, "mapart.toggle.displayname"));
        assertFalse(item.getItemMeta().hasDisplayName());
        assertEquals("Gallery", MapArtAPI.getStoredMapName(item).orElseThrow());
        assertTrue(MapArtService.toggle(owner, item, LockUtil.MAPART_NAME_VISIBLE_KEY, "mapart.toggle.displayname"));
        assertEquals(title, MapArtAPI.getMapName(item));
        assertEquals("Gallery", PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName()));
        assertTrue(MapArtService.rename(owner, item, "Second title"));
        assertEquals(MapArtAPI.getMapName(item), OwnershipDatabase.find(MapArtAPI.getMapUUID(item)).mapName);
    }

    @Test void droppingRegisteredMapPreservesTitleAndCreditLore() {
        var item = map();
        assertTrue(MapArtService.rename(owner, item, "Sunset"));
        assertTrue(MapArtService.credit(owner, item, "&aArtist"));
        assertTrue(MapArtService.lock(owner, item));
        var before = item.clone();
        Item dropped = mock(Item.class);
        when(dropped.getItemStack()).thenReturn(item);
        ItemSpawnEvent event = mock(ItemSpawnEvent.class);
        when(event.getEntity()).thenReturn(dropped);
        new MapDropListener().onItemSpawn(event);
        assertEquals(before, item);
        assertEquals("Creator: Artist", PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().lore().getFirst()));
    }

    @Test void requireNameAndDefaultHologramConfigurationAreApplied() {
        var item = map();
        plugin.getConfig().set("settings.require-display-name", true);
        plugin.getConfig().set("settings.default-hologram-visible", false);
        assertFalse(MapArtService.lock(owner, item));
        assertTrue(MapArtService.rename(owner, item, "Named"));
        assertTrue(MapArtService.lock(owner, item));
        assertFalse(MapArtService.toggle(owner, item, LockUtil.MAPART_NAME_VISIBLE_KEY, "mapart.toggle.displayname"));
        assertEquals((byte)0, ((MapMeta)item.getItemMeta()).getPersistentDataContainer()
                .get(LockUtil.HOLOGRAM_VISIBLE_KEY, org.bukkit.persistence.PersistentDataType.BYTE));
    }
}

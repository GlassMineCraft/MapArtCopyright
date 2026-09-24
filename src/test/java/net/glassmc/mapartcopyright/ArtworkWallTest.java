package net.glassmc.mapartcopyright;

import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.artwork.*;
import net.glassmc.mapartcopyright.commands.*;
import net.glassmc.mapartcopyright.database.*;
import net.glassmc.mapartcopyright.listeners.MapFrameListener;
import net.glassmc.mapartcopyright.service.*;
import net.glassmc.mapartcopyright.util.*;
import net.milkbowl.vault.economy.*;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockbukkit.mockbukkit.world.WorldMock;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.delegatesTo;

class ArtworkWallTest extends PluginTestBase {
    private final class Wall {
        final ArtworkSize size;
        final WorldMock world;
        final List<ItemFrame> frames = new ArrayList<>();
        Wall(String preset, BlockFace facing) {
            size = ArtworkSize.parse(preset);
            plugin.getConfig().set("settings.default-hologram-visible", false);
            world = spy(new WorldMock());
            world.setName("Gallery-" + UUID.randomUUID());
            server.addWorld(world);
            doAnswer(call -> new ArrayList<Entity>(frames)).when(world).getEntities();
            doAnswer(call -> new ArrayList<>(frames)).when(world).getEntitiesByClass(ItemFrame.class);
            doAnswer(call -> new ArrayList<Entity>(frames)).when(world)
                    .getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble());
            Location origin = new Location(world, 100.5, 80.5, 100.5);
            for (int y = 0; y < size.height(); y++) for (int x = 0; x < size.width(); x++) {
                ItemFrame frame = mock(ItemFrame.class);
                AtomicReference<ItemStack> contents = new AtomicReference<>(new ItemStack(Material.FILLED_MAP));
                Location location = origin.clone().add(WallSelection.rightOf(facing).getDirection().multiply(x)).add(0, -y, 0);
                when(frame.getLocation()).thenAnswer(call -> location.clone());
                when(frame.getWorld()).thenReturn(world);
                when(frame.getUniqueId()).thenReturn(UUID.randomUUID());
                when(frame.getFacing()).thenReturn(facing);
                when(frame.isValid()).thenReturn(true);
                when(frame.getPersistentDataContainer()).thenReturn(new ItemStack(Material.FILLED_MAP).getItemMeta().getPersistentDataContainer());
                when(frame.getNearbyEntities(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of());
                when(frame.getItem()).thenAnswer(call -> contents.get().clone());
                doAnswer(call -> { contents.set(((ItemStack) call.getArgument(0)).clone()); return null; })
                        .when(frame).setItem(any(ItemStack.class), eq(false));
                frames.add(frame);
            }
        }
        ItemFrame anchor() { return frames.getFirst(); }
        boolean create() { return ArtworkService.create(owner, anchor(), size, "Sunset"); }
        ItemStack tile(int index) { return frames.get(index).getItem(); }
        void put(int index, ItemStack item) { frames.get(index).setItem(item, false); }
    }

    private Wall wall(String size) { return new Wall(size, BlockFace.NORTH); }
    private static Stream<Arguments> layouts() {
        return ArtworkSize.PRESETS.stream().flatMap(size -> Stream.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
                .map(face -> Arguments.of(size, face)));
    }

    @ParameterizedTest @MethodSource("layouts")
    void allPresetsRegisterCorrectCoordinatesOnEveryWall(String size, BlockFace face) throws Exception {
        var wall = new Wall(size, face);
        assertEquals(wall.frames, WallSelection.collect(wall.anchor(), wall.size));
        assertTrue(wall.create());
        UUID group = ArtworkService.find(wall.tile(0)).artwork().id();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < wall.size.tiles(); i++) {
            var item = wall.tile(i);
            var membership = ArtworkService.find(item);
            assertEquals(group, membership.artwork().id());
            assertEquals(wall.size, membership.artwork().size());
            assertEquals(owner.getUniqueId(), membership.artwork().owner());
            assertEquals(i % wall.size.width(), membership.tile().x());
            assertEquals(i / wall.size.width(), membership.tile().y());
            assertTrue(ids.add(MapArtAPI.getMapUUID(item)));
            assertTrue(MapArtAPI.isLocked(item));
            assertTrue(MapArtAPI.isFrameLocked(item));
            assertEquals("Sunset", MapArtAPI.getStoredMapName(item).orElseThrow());
            assertEquals(owner.getUniqueId(), OwnershipDatabase.find(membership.tile().mapId()).playerUUID);
        }
        assertEquals(wall.size.tiles(), OwnershipDatabase.dumpAll().size());
    }

    @ParameterizedTest @ValueSource(strings = {"1x1", "3x2", "0x2", "9x9", "oops"})
    void unsupportedDimensionsAreRejected(String size) {
        assertThrows(IllegalArgumentException.class, () -> ArtworkSize.parse(size));
    }

    @ParameterizedTest @ValueSource(strings = {"missing", "empty", "facing", "duplicate", "ceiling"})
    void malformedWallsDoNotPartiallyRegister(String problem) {
        var wall = wall("2x2");
        switch (problem) {
            case "missing" -> wall.frames.removeLast();
            case "empty" -> wall.put(1, new ItemStack(Material.AIR));
            case "facing" -> when(wall.frames.get(1).getFacing()).thenReturn(BlockFace.SOUTH);
            case "duplicate" -> wall.frames.add(wall.frames.get(1));
            case "ceiling" -> when(wall.anchor().getFacing()).thenReturn(BlockFace.DOWN);
        }
        assertFalse(wall.create());
        assertTrue(OwnershipDatabase.dumpAll().isEmpty());
        assertNull(MapArtAPI.getMapUUID(wall.tile(0)));
    }

    @Test void existingMapIdsAndCreatorAttributionSurviveGrouping() throws Exception {
        var wall = wall("2x1");
        var existing = wall.tile(0);
        assertTrue(MapArtService.lock(owner, existing));
        String id = MapArtAPI.getMapUUID(existing);
        var meta = existing.getItemMeta();
        ((org.bukkit.inventory.meta.MapMeta) meta).setMapId(321);
        meta.getPersistentDataContainer().set(new NamespacedKey("otherplugin", "custom"), PersistentDataType.STRING, "keep");
        meta.getPersistentDataContainer().set(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING, other.getUniqueId().toString());
        existing.setItemMeta(meta);
        wall.put(0, existing);
        assertTrue(wall.create());
        assertEquals(id, MapArtAPI.getMapUUID(wall.tile(0)));
        assertTrue(MapArtAPI.verifyCreator(wall.tile(0), other.getUniqueId()));
        assertEquals(owner.getUniqueId(), ArtworkService.find(wall.tile(0)).artwork().owner());
        assertEquals(321, ((org.bukkit.inventory.meta.MapMeta) wall.tile(0).getItemMeta()).getMapId());
        assertEquals("keep", wall.tile(0).getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey("otherplugin", "custom"), PersistentDataType.STRING));
    }

    @Test void foreignOwnedTileCannotBeClaimedEvenWithBypass() {
        var wall = wall("2x1");
        var item = wall.tile(1);
        assertTrue(MapArtService.lock(other, item));
        wall.put(1, item);
        owner.addAttachment(plugin, "mapart.bypass", true);
        assertFalse(wall.create());
        assertNull(MapArtAPI.getMapUUID(wall.tile(0)));
        assertEquals(other.getUniqueId(), MapArtAPI.getOwner(wall.tile(1)));
        assertEquals(1, OwnershipDatabase.dumpAll().size());
    }

    @Test void duplicatedRegisteredTilesAndRegroupingAreRejected() throws Exception {
        var wall = wall("2x1");
        var registered = wall.tile(0);
        assertTrue(MapArtService.lock(owner, registered));
        wall.put(0, registered);
        wall.put(1, registered.clone());
        assertFalse(wall.create());
        wall.put(1, new ItemStack(Material.FILLED_MAP));
        assertTrue(wall.create());
        var before = ArtworkService.find(wall.tile(0));
        assertFalse(wall.create());
        assertEquals(before, ArtworkService.find(wall.tile(0)));
    }

    @ParameterizedTest @ValueSource(strings = {"wall", "lock", "rename", "credit", "use"})
    void registrationRequiresEveryRelevantPermission(String permission) {
        var wall = wall("2x1");
        owner.addAttachment(plugin, "mapart." + permission, false);
        assertFalse(wall.create());
        assertTrue(OwnershipDatabase.dumpAll().isEmpty());
    }

    private Economy economy() {
        plugin.getConfig().set("economy.enabled", true);
        Economy economy = mock(Economy.class);
        server.getServicesManager().register(Economy.class, economy, plugin, ServicePriority.Normal);
        when(economy.withdrawPlayer(eq(owner), anyDouble())).thenAnswer(call ->
                new EconomyResponse(call.getArgument(1), 1000, EconomyResponse.ResponseType.SUCCESS, ""));
        when(economy.depositPlayer(eq(owner), anyDouble())).thenAnswer(call ->
                new EconomyResponse(call.getArgument(1), 1000, EconomyResponse.ResponseType.SUCCESS, ""));
        return economy;
    }

    @Test void feesArePerChangedTileInOnePaymentAndNoOpIsFree() {
        var wall = wall("2x2");
        var registered = wall.tile(0);
        assertTrue(MapArtService.lock(owner, registered));
        wall.put(0, registered);
        var economy = economy();
        assertTrue(wall.create());
        verify(economy).withdrawPlayer(owner, 300);
        var held = wall.tile(2);
        assertFalse(MapArtService.lock(owner, held));
        assertTrue(MapArtService.unlock(owner, held));
        verify(economy).withdrawPlayer(owner, 200);
        assertTrue(MapArtService.lock(owner, held));
        verify(economy).withdrawPlayer(owner, 400);
        verify(economy, times(3)).withdrawPlayer(eq(owner), anyDouble());
    }

    @Test void declinedPaymentDoesNotChangeWallOrDatabase() {
        var wall = wall("2x1");
        var before = wall.tile(0);
        var economy = economy();
        when(economy.withdrawPlayer(owner, 200)).thenReturn(new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "insufficient"));
        assertFalse(wall.create());
        assertEquals(before, wall.tile(0));
        assertTrue(OwnershipDatabase.dumpAll().isEmpty());
        verify(economy, never()).depositPlayer(eq(owner), anyDouble());
    }

    @Test void partialFrameFailureRollsBackEveryTableAndRefunds() throws Exception {
        var wall = wall("2x2");
        var before = wall.tile(0);
        var economy = economy();
        doThrow(new IllegalStateException("frame disappeared")).when(wall.frames.get(2)).setItem(any(ItemStack.class), eq(false));
        assertFalse(wall.create());
        assertEquals(before, wall.tile(0));
        assertEquals(before, wall.tile(1));
        assertTrue(OwnershipDatabase.dumpAll().isEmpty());
        assertEquals(0, rowCount("map_artworks"));
        assertEquals(0, rowCount("map_artwork_tiles"));
        verify(economy).depositPlayer(owner, 400);
    }

    @Test void databaseLossAfterPaymentRefundsWithoutChangingFrames() {
        var wall = wall("2x1");
        var before = wall.tile(0);
        var economy = economy();
        when(economy.withdrawPlayer(owner, 200)).thenAnswer(call -> {
            OwnershipDatabase.close();
            return new EconomyResponse(200, 1000, EconomyResponse.ResponseType.SUCCESS, "");
        });
        assertFalse(wall.create());
        assertEquals(before, wall.tile(0));
        verify(economy).depositPlayer(owner, 200);
    }

    @Test void successfulCommitIsNotRefundedWhenConnectionCleanupFails() throws Exception {
        var wall = wall("2x1");
        var economy = economy();
        Connection wrapped = mock(Connection.class, delegatesTo(connection()));
        doThrow(new SQLException("cleanup failed")).when(wrapped).setAutoCommit(true);
        replaceConnection(wrapped);
        assertTrue(wall.create());
        verify(economy, never()).depositPlayer(eq(owner), anyDouble());
        assertTrue(OwnershipDatabase.connect());
        assertNotNull(ArtworkService.find(wall.tile(0)));
        assertEquals(2, OwnershipDatabase.dumpAll().size());
    }

    @Test void failedRollbackDiscardsConnectionWithoutAccidentallyCommitting() throws Exception {
        var wall = wall("2x1");
        var economy = economy();
        Connection wrapped = mock(Connection.class, delegatesTo(connection()));
        doThrow(new SQLException("rollback failed")).when(wrapped).rollback();
        replaceConnection(wrapped);
        doThrow(new IllegalStateException("frame failed")).when(wall.frames.get(1)).setItem(any(ItemStack.class), eq(false));
        assertFalse(wall.create());
        verify(wrapped, never()).setAutoCommit(true);
        verify(wrapped).close();
        assertTrue(OwnershipDatabase.connect());
        assertEquals(0, rowCount("map_artworks"));
        assertTrue(OwnershipDatabase.dumpAll().isEmpty());
        verify(economy).depositPlayer(owner, 200);
    }

    @Test void sharedUnlockPaymentFailurePreservesAllTileLocks() throws Exception {
        var wall = wall("2x2");
        assertTrue(wall.create());
        var before = ArtworkService.find(wall.tile(0));
        var economy = economy();
        when(economy.withdrawPlayer(owner, 200)).thenReturn(new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "insufficient"));
        assertFalse(MapArtService.unlock(owner, wall.tile(3)));
        assertEquals(before, ArtworkService.find(wall.tile(0)));
        for (var frame : wall.frames) assertTrue(MapArtAPI.isLocked(frame.getItem()));
        verify(economy, never()).depositPlayer(eq(owner), anyDouble());
    }

    @Test void frameChangedByPaymentProviderIsNotOverwritten() {
        var wall = wall("2x1");
        var economy = economy();
        when(economy.withdrawPlayer(owner, 200)).thenAnswer(call -> {
            wall.put(1, new ItemStack(Material.DIAMOND));
            return new EconomyResponse(200, 1000, EconomyResponse.ResponseType.SUCCESS, "");
        });
        assertFalse(wall.create());
        assertEquals(Material.DIAMOND, wall.tile(1).getType());
        assertTrue(OwnershipDatabase.dumpAll().isEmpty());
        verify(economy).depositPlayer(owner, 200);
    }

    @Test void editingAnyTileUpdatesEveryFrameAndOwnerRecord() throws Exception {
        var wall = wall("2x3");
        assertTrue(wall.create());
        var held = wall.tile(5);
        assertTrue(MapArtService.rename(owner, held, "<gold>Evening</gold>"));
        assertTrue(MapArtService.credit(owner, held, "Painter"));
        assertTrue(MapArtService.unlock(owner, held));
        assertTrue(MapArtService.toggle(owner, held, LockUtil.ITEMFRAME_LOCK_KEY, "mapart.toggle.itemframe"));
        assertTrue(MapArtService.toggle(owner, held, LockUtil.MAPART_NAME_VISIBLE_KEY, "mapart.toggle.displayname"));
        for (int i = 0; i < wall.size.tiles(); i++) {
            var item = wall.tile(i);
            assertFalse(MapArtAPI.isLocked(item));
            assertFalse(MapArtAPI.isFrameLocked(item));
            assertEquals("Evening", MapArtAPI.getStoredMapName(item).orElseThrow());
            assertEquals("Painter", CreditUtil.getCredit(item));
            assertFalse(item.getItemMeta().hasDisplayName());
            assertEquals(owner.getUniqueId(), OwnershipDatabase.find(MapArtAPI.getMapUUID(item)).playerUUID);
            assertEquals("Painter", OwnershipDatabase.find(MapArtAPI.getMapUUID(item)).creatorName);
        }
        var before = ArtworkService.find(held);
        assertFalse(MapArtService.rename(other, held, "Stolen"));
        assertFalse(MapArtService.lock(other, held));
        assertEquals(before, ArtworkService.find(held));
    }

    @Test void preGroupAndStaleCopiesUseCurrentSharedProtection() throws Exception {
        var wall = wall("2x1");
        var registered = wall.tile(0);
        assertTrue(MapArtService.lock(owner, registered));
        assertTrue(MapArtService.unlock(owner, registered));
        var oldCopy = registered.clone();
        wall.put(0, registered);
        assertTrue(wall.create());
        assertTrue(MapArtAPI.isLocked(oldCopy));
        assertTrue(MapArtAPI.isFrameLocked(oldCopy));
        assertFalse(oldCopy.getItemMeta().getPersistentDataContainer().has(LockUtil.ARTWORK_ID_KEY));
        var staleLocked = wall.tile(0);
        assertTrue(MapArtService.unlock(owner, wall.tile(1)));
        assertFalse(MapArtAPI.isLocked(staleLocked));
        assertFalse(MapArtAPI.isLocked(oldCopy));
        oldCopy.setAmount(7);
        var chest = server.createInventory(null, 9);
        chest.setItem(3, oldCopy);
        ArtworkService.refreshInventory(chest);
        assertEquals(7, chest.getItem(3).getAmount());
        assertEquals(MapArtAPI.getMapUUID(oldCopy), MapArtAPI.getMapUUID(chest.getItem(3)));
        assertNotNull(ArtworkService.find(chest.getItem(3)));
        assertNull(chest.getItem(0));
        OwnershipDatabase.close();
        assertTrue(MapArtAPI.isLocked(oldCopy));
        assertTrue(MapArtAPI.isFrameLocked(oldCopy));
    }

    @ParameterizedTest @ValueSource(strings = {"h2", "sqlite"})
    void artworkAndLegacySingleMapsSurviveReconnect(String database) throws Exception {
        plugin.getConfig().set("database.type", database);
        assertTrue(OwnershipDatabase.connect());
        var single = map();
        assertTrue(MapArtService.lock(owner, single));
        String singleId = MapArtAPI.getMapUUID(single);
        var wall = wall("3x3");
        assertTrue(wall.create());
        assertTrue(MapArtService.rename(owner, wall.tile(8), "Persisted"));
        var before = ArtworkService.find(wall.tile(8));
        OwnershipDatabase.close();
        assertTrue(OwnershipDatabase.connect());
        assertEquals(before, ArtworkService.find(wall.tile(8)));
        assertEquals(9, ArtworkDatabase.tiles(before.artwork().id()).size());
        assertEquals(owner.getUniqueId(), OwnershipDatabase.find(singleId).playerUUID);
        assertNull(ArtworkService.find(single));
    }

    @Test void staleRevisionAndItemFailureDoNotPartiallyUpdateGroup() throws Exception {
        var wall = wall("2x2");
        assertTrue(wall.create());
        var before = ArtworkService.find(wall.tile(0)).artwork();
        var next = new ArtworkRecord(before.id(), before.owner(), before.size(), "Changed", before.credit(),
                false, true, false, true, before.revision() + 1);
        assertThrows(IllegalStateException.class, () -> ArtworkDatabase.update(before, next, () -> { throw new IllegalStateException("item failed"); }));
        assertEquals(before, ArtworkService.find(wall.tile(0)).artwork());
        for (var row : OwnershipDatabase.dumpAll()) assertEquals(before.name(), row.mapName);
        ArtworkDatabase.update(before, next, () -> {});
        assertThrows(SQLException.class, () -> ArtworkDatabase.update(before, next, () -> fail("Stale item callback must not run")));
        assertEquals(next, ArtworkService.find(wall.tile(0)).artwork());
    }

    @Test void orphanedMembershipFailsClosedEvenForCopiesWithoutArtworkTags() throws Exception {
        var wall = wall("2x1");
        var item = wall.tile(0);
        assertTrue(MapArtService.lock(owner, item));
        assertTrue(MapArtService.unlock(owner, item));
        wall.put(0, item);
        assertTrue(wall.create());
        try (var statement = connection().createStatement()) { statement.executeUpdate("DELETE FROM map_artworks"); }
        assertThrows(SQLException.class, () -> ArtworkService.find(item));
        assertTrue(MapArtAPI.isLocked(item));
        assertFalse(MapArtService.rename(owner, item, "Corrupt"));
    }

    @Test void groupedAnvilRenamesAreBlockedButCommandRenamesWork() {
        var wall = wall("2x1");
        assertTrue(wall.create());
        assertThrows(IllegalArgumentException.class, () -> MapArtService.previewAnvilName(wall.tile(0), "One tile only"));
        assertTrue(MapArtService.rename(owner, wall.tile(0), "Whole artwork"));
        assertEquals("Whole artwork", MapArtAPI.getStoredMapName(wall.tile(1)).orElseThrow());
    }

    @Test void menuClearlyLabelsGroupActionsAndPerTileFees() {
        var wall = wall("2x2");
        assertTrue(wall.create());
        owner.getInventory().setItemInMainHand(wall.tile(0));
        net.glassmc.mapartcopyright.gui.MapArtGUI.open(owner, owner.getInventory().getItemInMainHand());
        var plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();
        var button = owner.getOpenInventory().getTopInventory().getItem(24).getItemMeta();
        assertEquals("Unlock Artwork", plain.serialize(button.displayName()));
        assertTrue(button.lore().stream().map(plain::serialize).anyMatch(line -> line.contains("per tile")));
    }

    @Test void wallCommandUsesTopLeftTargetAndCompletesPresets() throws Exception {
        var wall = wall("2x2");
        var player = spy(owner);
        doReturn(wall.anchor()).when(player).getTargetEntity(6, false);
        new WallCommand().execute(player, new String[]{"wall", "create", "2x2", "Command", "title"});
        assertEquals("Command title", MapArtAPI.getStoredMapName(wall.tile(0)).orElseThrow());
        assertNotNull(ArtworkService.find(wall.tile(3)));
        var completer = new MapArtTabCompleter();
        assertEquals(ArtworkSize.PRESETS, completer.onTabComplete(owner, plugin.getCommand("mapart"), "mapart", new String[]{"wall", "create", ""}));
        owner.addAttachment(plugin, "mapart.wall", false);
        assertFalse(completer.onTabComplete(owner, plugin.getCommand("mapart"), "mapart", new String[]{""}).contains("wall"));
    }

    @Test void artworkShowsOneCreatorHologramCenteredUnderBottomRow() {
        var wall = wall("3x3");
        assertTrue(wall.create());
        List<Location> positions = new ArrayList<>();
        doAnswer(call -> {
            positions.add(((Location) call.getArgument(0)).clone());
            TextDisplay display = mock(TextDisplay.class);
            when(display.getUniqueId()).thenReturn(UUID.randomUUID());
            when(display.getPersistentDataContainer()).thenReturn(new ItemStack(Material.FILLED_MAP).getItemMeta().getPersistentDataContainer());
            Consumer<TextDisplay> setup = call.getArgument(2);
            setup.accept(display);
            return display;
        }).when(wall.world).spawn(any(Location.class), eq(TextDisplay.class), any(Consumer.class));
        assertTrue(MapArtService.toggle(owner, wall.tile(1), LockUtil.HOLOGRAM_VISIBLE_KEY, "mapart.toggle.hologram"));
        assertEquals(1, positions.size());
        Location expected = wall.frames.get(6).getLocation().add(-1, -0.65, -0.12);
        assertEquals(0, expected.distanceSquared(positions.getFirst()), 0.000001);
    }

    private Connection connection() throws Exception {
        var field = OwnershipDatabase.class.getDeclaredField("connection");
        field.setAccessible(true);
        return (Connection) field.get(null);
    }

    private void replaceConnection(Connection replacement) throws Exception {
        var field = OwnershipDatabase.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(null, replacement);
    }

    private int rowCount(String table) throws Exception {
        try (var statement = connection().createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }
}

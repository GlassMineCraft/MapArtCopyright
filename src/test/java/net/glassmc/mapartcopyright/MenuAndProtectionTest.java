package net.glassmc.mapartcopyright;

import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import net.glassmc.mapartcopyright.gui.*;
import net.glassmc.mapartcopyright.listeners.*;
import net.glassmc.mapartcopyright.service.MapArtService;
import net.glassmc.mapartcopyright.util.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.inventory.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MenuAndProtectionTest extends PluginTestBase {
    private InventoryClickEvent click(InventoryView view, int raw) {
        return new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, raw, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    }

    @Test void bottomInventoryClickCannotTriggerTopMenuAction() {
        var item = map();
        MapArtGUI.open(owner, item);
        var view = owner.getOpenInventory();
        // Bottom inventory slot 22 collides with the old menu lock button.
        var event = click(view, 45 + 22 - 9);
        assertEquals(22, event.getSlot());
        server.getPluginManager().callEvent(event);
        server.getScheduler().performOneTick();
        assertTrue(event.isCancelled());
        assertFalse(MapArtAPI.isLocked(owner.getInventory().getItemInMainHand()));
        assertTrue(OwnershipDatabase.dumpAll().isEmpty());
    }

    @Test void titleAloneDoesNotIdentifyAMapMenu() {
        var item = map();
        owner.openInventory(server.createInventory(null, 45, MapArtGUI.GUI_TITLE));
        var event = click(owner.getOpenInventory(), 22);
        server.getPluginManager().callEvent(event);
        server.getScheduler().performOneTick();
        assertFalse(event.isCancelled());
        assertFalse(MapArtAPI.isLocked(item));
    }

    @Test void previouslyCanceledMenuClickDoesNotApplyAnAction() {
        var item = map();
        MapArtGUI.open(owner, item);
        var event = click(owner.getOpenInventory(), 22);
        event.setCancelled(true);
        server.getPluginManager().callEvent(event);
        server.getScheduler().performOneTick();
        assertFalse(MapArtAPI.isLocked(item));
        assertTrue(OwnershipDatabase.dumpAll().isEmpty());
    }

    @Test void menuClickIsDeferredAndDragsAreCanceled() {
        var item = map();
        MapArtGUI.open(owner, item);
        var view = owner.getOpenInventory();
        var drag = new InventoryDragEvent(view, new ItemStack(Material.DIAMOND),
                new ItemStack(Material.DIAMOND, 2), false, Map.of(3, new ItemStack(Material.DIAMOND)));
        server.getPluginManager().callEvent(drag);
        assertTrue(drag.isCancelled());
        var event = click(view, 22);
        server.getPluginManager().callEvent(event);
        assertFalse(MapArtAPI.isLocked(item));
        server.getScheduler().performOneTick();
        assertTrue(MapArtAPI.isLocked(owner.getInventory().getItemInMainHand()));
    }

    @Test void menuPermissionAndConfigAreEnforced() {
        var item = map();
        owner.addAttachment(plugin, "mapart.menu", false);
        MapArtGUI.open(owner, item);
        assertTrue(owner.getOpenInventory().getTopInventory() == null
                || !(owner.getOpenInventory().getTopInventory().getHolder() instanceof MapArtMenu));
        other.getInventory().setItemInMainHand(item);
        plugin.getConfig().set("features.enable-gui", false);
        MapArtGUI.open(other, item);
        assertTrue(other.getOpenInventory().getTopInventory() == null
                || !(other.getOpenInventory().getTopInventory().getHolder() instanceof MapArtMenu));
    }

    @Test void customMenuTitleAndMaterialsWork() {
        var item = map();
        plugin.getConfig().set("gui.title", "Custom map menu");
        plugin.getConfig().set("gui.filler-item", "BLACK_STAINED_GLASS_PANE");
        plugin.getConfig().set("gui.exit-item", "OAK_DOOR");
        MapArtGUI.open(owner, item);
        var view = owner.getOpenInventory();
        assertEquals("Custom map menu", view.getTitle());
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, view.getTopInventory().getItem(3).getType());
        assertEquals(Material.OAK_DOOR, view.getTopInventory().getItem(44).getType());
        server.getPluginManager().callEvent(click(view, 22));
        server.getScheduler().performOneTick();
        assertTrue(MapArtAPI.isLocked(owner.getInventory().getItemInMainHand()));
    }

    @Test void automaticCrafterRejectsLockedInputEvenIfResultLosesTags() {
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        Block block = mock(Block.class);
        Crafter crafter = mock(Crafter.class);
        Inventory inventory = server.createInventory(null, 9);
        inventory.setItem(0, item);
        when(block.getState()).thenReturn(crafter);
        when(crafter.getInventory()).thenReturn(inventory);
        var event = new CrafterCraftEvent(block, mock(CraftingRecipe.class), new ItemStack(Material.FILLED_MAP, 2));
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
    }

    @Test void cartographyDenialIsNotLoggedAsSuccessfulCloning() throws Exception {
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        Inventory inventory = mock(Inventory.class);
        when(inventory.getType()).thenReturn(InventoryType.CARTOGRAPHY);
        when(inventory.getSize()).thenReturn(3);
        when(inventory.getItem(0)).thenReturn(item);
        when(inventory.getItem(1)).thenReturn(new ItemStack(Material.MAP));
        var view = spy(new SimpleInventoryViewMock(other, inventory, other.getInventory(), InventoryType.CARTOGRAPHY));
        doReturn(2).when(view).convertSlot(2); // MockBukkit does not implement cartography slot conversion.
        var event = click(view, 2);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        String audit = Files.readString(plugin.getDataFolder().toPath().resolve("audit.log"));
        assertTrue(audit.contains("denied_clone"));
        assertFalse(audit.contains("clone_allowed"));
    }

    @Test void blankAnvilRenameIsDeniedToNonOwnerInPreviewAndResult() {
        var item = map();
        assertTrue(MapArtService.rename(owner, item, "Protected title"));
        assertTrue(MapArtService.lock(owner, item));
        var inventory = new AnvilInventoryMock(null);
        inventory.setItem(0, item);
        inventory.setItem(2, item.clone());
        AnvilView view = anvilView(other, inventory, "");
        var preview = new PrepareAnvilEvent(view, item.clone());
        server.getPluginManager().callEvent(preview);
        assertNull(preview.getResult());
        var result = click(view, 2);
        server.getPluginManager().callEvent(result);
        assertTrue(result.isCancelled());
        assertEquals("Protected title", MapArtAPI.getStoredMapName(item).orElseThrow());
    }

    @Test void laterAnvilCancellationRestoresDatabaseMetadata() throws Exception {
        var item = map();
        assertTrue(MapArtService.rename(owner, item, "Before"));
        assertTrue(MapArtService.lock(owner, item));
        var inventory = new AnvilInventoryMock(null);
        inventory.setItem(0, item);
        inventory.setItem(2, item.clone());
        var event = click(anvilView(owner, inventory, "After"), 2);
        new AnvilRenameListener().onResultClick(event);
        assertFalse(event.isCancelled());
        assertTrue(OwnershipDatabase.find(MapArtAPI.getMapUUID(item)).mapName.contains("After"));
        event.setCancelled(true); // Another plugin cancels after our HIGHEST handler.
        server.getScheduler().performOneTick();
        assertEquals(MapArtAPI.getMapName(item), OwnershipDatabase.find(MapArtAPI.getMapUUID(item)).mapName);
        assertFalse(Files.readString(plugin.getDataFolder().toPath().resolve("audit.log")).contains("anvil_rename_allowed"));
    }

    private AnvilView anvilView(Player player, AnvilInventory inventory, String raw) {
        AnvilView view = mock(AnvilView.class);
        when(view.getPlayer()).thenReturn(player);
        when(view.getTopInventory()).thenReturn(inventory);
        when(view.getBottomInventory()).thenReturn(player.getInventory());
        when(view.getType()).thenReturn(InventoryType.ANVIL);
        when(view.getRenameText()).thenReturn(raw);
        return view;
    }

    @Test void frameProtectionResolvesProjectileShooterAndBlocksEnvironmentalDetach() {
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        ItemFrame frame = mock(ItemFrame.class);
        when(frame.getItem()).thenReturn(item);
        Projectile arrow = mock(Projectile.class);
        when(arrow.getShooter()).thenReturn(other);
        var shot = new EntityDamageByEntityEvent(arrow, frame, EntityDamageEvent.DamageCause.PROJECTILE, 1);
        server.getPluginManager().callEvent(shot);
        assertTrue(shot.isCancelled());
        var environmental = new HangingBreakEvent(frame, HangingBreakEvent.RemoveCause.PHYSICS);
        server.getPluginManager().callEvent(environmental);
        assertTrue(environmental.isCancelled());
        when(arrow.getShooter()).thenReturn(owner);
        var ownerShot = new EntityDamageByEntityEvent(arrow, frame, EntityDamageEvent.DamageCause.PROJECTILE, 1);
        new MapFrameListener().protectDamage(ownerShot);
        assertFalse(ownerShot.isCancelled());
    }

    @Test void removingHologramDoesNotRemoveAdjacentFramesDisplay() {
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        TextDisplay firstDisplay = display(first);
        TextDisplay secondDisplay = display(second);
        ItemFrame frame = mock(ItemFrame.class);
        when(frame.getUniqueId()).thenReturn(UUID.fromString(first));
        when(frame.getPersistentDataContainer()).thenReturn(new ItemStack(Material.FILLED_MAP).getItemMeta().getPersistentDataContainer());
        when(frame.getNearbyEntities(2, 2, 2)).thenReturn(List.of(firstDisplay, secondDisplay));
        HologramUtil.remove(frame);
        verify(firstDisplay).remove();
        verify(secondDisplay, never()).remove();
    }

    private TextDisplay display(String frameId) {
        TextDisplay display = mock(TextDisplay.class);
        var data = new ItemStack(Material.FILLED_MAP).getItemMeta().getPersistentDataContainer();
        data.set(LockUtil.HOLOGRAM_TAG_KEY, PersistentDataType.STRING, frameId);
        when(display.getPersistentDataContainer()).thenReturn(data);
        return display;
    }

    @Test void supportingBlockCannotBeBrokenWhileProtectedMapRemains() {
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        ItemFrame frame = mock(ItemFrame.class);
        when(frame.getItem()).thenReturn(item);
        Block support = mock(Block.class);
        Block frameBlock = mock(Block.class);
        World world = mock(World.class);
        when(support.getWorld()).thenReturn(world);
        when(support.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        Location frameLocation = mock(Location.class);
        when(frame.getLocation()).thenReturn(frameLocation);
        when(frameLocation.getBlock()).thenReturn(frameBlock);
        when(frame.getAttachedFace()).thenReturn(BlockFace.WEST);
        when(frameBlock.getRelative(BlockFace.WEST)).thenReturn(support);
        when(world.getNearbyEntities(any(Location.class), eq(2.0), eq(2.0), eq(2.0))).thenReturn(List.of(frame));
        var event = new BlockBreakEvent(support, other);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
    }

    @Test void legacyFixedFrameIsReleasedForItsOwnerWithoutLosingProtection() {
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        assertTrue(MapArtService.toggle(owner, item, LockUtil.ITEMFRAME_LOCK_KEY, "mapart.toggle.itemframe"));
        ItemFrame frame = mock(ItemFrame.class);
        when(frame.getItem()).thenReturn(item);
        when(frame.isValid()).thenReturn(true);
        when(frame.getUniqueId()).thenReturn(UUID.randomUUID());
        when(frame.getPersistentDataContainer()).thenReturn(new ItemStack(Material.FILLED_MAP).getItemMeta().getPersistentDataContainer());
        when(frame.getNearbyEntities(2, 2, 2)).thenReturn(List.of());
        MapFrameListener.refresh(frame);
        verify(frame).setFixed(false);
        assertTrue(MapFrameListener.protectedFrame(frame));
    }
}

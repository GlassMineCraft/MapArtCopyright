package net.glassmc.mapartcopyright;

import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.database.*;
import net.glassmc.mapartcopyright.service.MapArtService;
import net.glassmc.mapartcopyright.util.*;
import net.milkbowl.vault.economy.*;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnershipAndPaymentTest extends PluginTestBase {
    private Economy economy() {
        plugin.getConfig().set("economy.enabled", true);
        Economy economy = mock(Economy.class);
        server.getServicesManager().register(Economy.class, economy, plugin, ServicePriority.Normal);
        when(economy.withdrawPlayer(owner, 100)).thenReturn(new EconomyResponse(100, 900, EconomyResponse.ResponseType.SUCCESS, ""));
        when(economy.withdrawPlayer(owner, 50)).thenReturn(new EconomyResponse(50, 850, EconomyResponse.ResponseType.SUCCESS, ""));
        when(economy.depositPlayer(owner, 100)).thenReturn(new EconomyResponse(100, 1000, EconomyResponse.ResponseType.SUCCESS, ""));
        return economy;
    }

    @Test void unlockRemovesFlagWithoutLosingOwnershipOrMetadata() throws Exception {
        var item = map();
        assertTrue(MapArtService.rename(owner, item, "<gold>Sunset</gold>"));
        assertTrue(MapArtService.credit(owner, item, "&bArtist"));
        assertTrue(MapArtService.lock(owner, item));
        String id = MapArtAPI.getMapUUID(item);
        assertTrue(MapArtService.unlock(owner, item));
        assertFalse(MapArtAPI.isLocked(item));
        assertEquals(id, MapArtAPI.getMapUUID(item));
        assertEquals(owner.getUniqueId(), OwnershipDatabase.find(id).playerUUID);
        assertEquals("Sunset", MapArtAPI.getStoredMapName(item).orElseThrow());
        assertEquals(CreditUtil.getCredit(item), OwnershipDatabase.find(id).creatorName);
    }

    @Test void otherPlayerCannotRelockOrRenameAnUnlockedRegisteredMap() throws Exception {
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        assertTrue(MapArtService.unlock(owner, item));
        var before = item.clone();
        assertFalse(MapArtService.lock(other, item));
        assertFalse(MapArtService.rename(other, item, "Stolen"));
        assertEquals(before, item);
        assertEquals(owner.getUniqueId(), OwnershipDatabase.find(MapArtAPI.getMapUUID(item)).playerUUID);
    }

    @Test void alreadyLockedDoesNotChargeAgain() {
        var economy = economy();
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        assertFalse(MapArtService.lock(owner, item));
        verify(economy, times(1)).withdrawPlayer(owner, 100);
    }

    @Test void providerFailureLeavesItemAndDatabaseUntouched() {
        var economy = economy();
        when(economy.withdrawPlayer(owner, 100)).thenReturn(new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "insufficient"));
        var item = map();
        var before = item.clone();
        assertFalse(MapArtService.lock(owner, item));
        assertEquals(before, item);
        assertTrue(OwnershipDatabase.dumpAll().isEmpty());
        verify(economy, never()).depositPlayer(owner, 100);
    }

    @Test void disconnectedDatabaseDoesNotChargeOrChangeItem() {
        var economy = economy();
        var item = map();
        var before = item.clone();
        OwnershipDatabase.close();
        assertFalse(MapArtService.lock(owner, item));
        assertEquals(before, item);
        verify(economy, never()).withdrawPlayer(owner, 100);
    }

    @Test void writeFailureAfterPaymentRefundsAndLeavesItemUnchanged() {
        var economy = economy();
        when(economy.withdrawPlayer(owner, 100)).thenAnswer(invocation -> {
            OwnershipDatabase.close();
            return new EconomyResponse(100, 900, EconomyResponse.ResponseType.SUCCESS, "");
        });
        var item = map();
        var before = item.clone();
        assertFalse(MapArtService.lock(owner, item));
        assertEquals(before, item);
        verify(economy).depositPlayer(owner, 100);
    }

    @Test void unsuccessfulRefundCreatesDurableReconciliationRecord() throws Exception {
        var economy = economy();
        when(economy.withdrawPlayer(owner, 100)).thenAnswer(invocation -> {
            OwnershipDatabase.close();
            return new EconomyResponse(100, 900, EconomyResponse.ResponseType.SUCCESS, "");
        });
        when(economy.depositPlayer(owner, 100)).thenReturn(new EconomyResponse(0, 900, EconomyResponse.ResponseType.FAILURE, "offline"));
        assertFalse(MapArtService.lock(owner, map()));
        String record = Files.readString(plugin.getDataFolder().toPath().resolve("refunds-pending.log"));
        assertTrue(record.contains(owner.getUniqueId().toString()));
        assertTrue(record.contains("amount=100.0"));
    }

    @Test void creatorTagCannotOverrideDatabaseOwner() {
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        var meta = (MapMeta) item.getItemMeta();
        meta.getPersistentDataContainer().set(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING, other.getUniqueId().toString());
        item.setItemMeta(meta);
        assertTrue(MapArtAPI.verifyCreator(item, other.getUniqueId()));
        assertFalse(MapArtAPI.isOwner(other, item));
        assertFalse(MapArtService.unlock(other, item));
    }

    @Test void h2OwnershipSurvivesReconnectAndRejectsOverwrite() throws Exception {
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        String id = MapArtAPI.getMapUUID(item);
        OwnershipDatabase.close();
        assertTrue(OwnershipDatabase.connect());
        assertEquals(owner.getUniqueId(), OwnershipDatabase.find(id).playerUUID);
        assertThrows(IllegalStateException.class, () -> OwnershipDatabase.setOwner(id, other.getUniqueId()));
        assertEquals(owner.getUniqueId(), OwnershipDatabase.find(id).playerUUID);
    }

    @Test void sqliteDriverSupportsRegistrationAndMetadataUpdates() throws Exception {
        plugin.getConfig().set("database.type", "sqlite");
        assertTrue(OwnershipDatabase.connect());
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        assertTrue(MapArtService.rename(owner, item, "SQLite art"));
        String id = MapArtAPI.getMapUUID(item);
        OwnershipDatabase.close();
        assertTrue(OwnershipDatabase.connect());
        assertEquals(owner.getUniqueId(), OwnershipDatabase.find(id).playerUUID);
        assertEquals(MapArtAPI.getMapName(item), OwnershipDatabase.find(id).mapName);
    }

    @Test void registeredMapWithMissingRecordFailsClosed() throws Exception {
        var item = map();
        assertTrue(MapArtService.lock(owner, item));
        OwnershipDatabase.rollbackRegistration(MapArtAPI.getMapUUID(item), owner.getUniqueId());
        var before = item.clone();
        assertFalse(MapArtService.unlock(owner, item));
        assertEquals(before, item);
    }

    @Test void missingEconomyProviderDoesNotLockOrRegister() {
        plugin.getConfig().set("economy.enabled", true);
        var item = map();
        assertFalse(MapArtService.lock(owner, item));
        assertFalse(MapArtAPI.isLocked(item));
        assertNull(MapArtAPI.getMapUUID(item));
    }
}

package net.glassmc.mapartcopyright;

import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.*;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.permissions.PermissionAttachment;
import static org.junit.jupiter.api.Assertions.*;

abstract class PluginTestBase {
    protected ServerMock server;
    protected MapArtCopyright plugin;
    protected PlayerMock owner;
    protected PlayerMock other;

    @BeforeEach void startServer() {
        server = MockBukkit.mock();
        MockBukkit.createMockPlugin("Vault");
        plugin = MockBukkit.load(MapArtCopyright.class);
        plugin.getConfig().set("economy.enabled", false);
        owner = server.addPlayer("Artist");
        other = server.addPlayer("Visitor");
        permissions(owner);
        permissions(other);
        assertTrue(OwnershipDatabase.isConnected());
    }

    private void permissions(PlayerMock player) {
        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String permission : new String[]{"use", "menu", "lock", "unlock", "rename", "credit",
                "toggle.displayname", "toggle.hologram", "toggle.itemframe", "audit", "export"})
            attachment.setPermission("mapart." + permission, true);
        attachment.setPermission("mapart.bypass", false);
        attachment.setPermission("mapart.free", false);
    }

    protected ItemStack map() {
        owner.getInventory().setItemInMainHand(new ItemStack(Material.FILLED_MAP));
        return owner.getInventory().getItemInMainHand();
    }

    @AfterEach void stopServer() {
        OwnershipDatabase.close();
        MockBukkit.unmock();
    }
}

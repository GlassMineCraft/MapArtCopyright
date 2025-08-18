package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import net.glassmc.mapartcopyright.economy.EconomyUtil;
import net.glassmc.mapartcopyright.util.CreditUtil;
import net.glassmc.mapartcopyright.util.LockUtil;
import net.glassmc.mapartcopyright.util.LoreUtil;
import net.glassmc.mapartcopyright.Audit.AuditLogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class LockCommand implements SubCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    @Override
    public String getName() {
        return "lock";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can lock maps.");
            return;
        }

        if (!player.hasPermission("mapart.lock")) {
            player.sendMessage("§cYou do not have permission to lock maps.");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getItemMeta() == null || !(item.getItemMeta() instanceof MapMeta)) {
            player.sendMessage("§cHold a filled map to lock it.");
            return;
        }

        MapMeta mapMeta = (MapMeta) item.getItemMeta();
        if (EconomyUtil.isEnabled() && !player.hasPermission("mapart.free")) {
            double cost = EconomyUtil.getCost("lock");
            if (!EconomyUtil.charge(player, cost, true)) {
                return;
            }
        }

        // Generate UUID if it doesn't exist
        String mapUUID = mapMeta.getPersistentDataContainer().get(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING);
        if (mapUUID == null) {
            mapUUID = UUID.randomUUID().toString();
            mapMeta.getPersistentDataContainer().set(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING, mapUUID);
        }

        // Save the map display name to NBT in MiniMessage format
        if (!mapMeta.getPersistentDataContainer().has(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING)) {
            String storedName;
            if (mapMeta.hasDisplayName()) {
                Component displayName = mapMeta.displayName();
                storedName = MINI.serialize(displayName);
            } else {
                storedName = "<gray>Untitled Map";
            }
            mapMeta.getPersistentDataContainer().set(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING, storedName);
            mapMeta.getPersistentDataContainer().set(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
        }

        // Record ownership
        String mapName = mapMeta.getPersistentDataContainer().get(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING);
        String creatorName = CreditUtil.getCredit(item);
        OwnershipDatabase.setOwner(mapUUID, player.getUniqueId(), mapName, creatorName);

        // Lock the map (custom flag only)
        mapMeta.getPersistentDataContainer().set(LockUtil.LOCK_KEY, PersistentDataType.BYTE, (byte) 1);

        // Set hologram visibility by default
        if (!mapMeta.getPersistentDataContainer().has(LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE)) {
            mapMeta.getPersistentDataContainer().set(LockUtil.HOLOGRAM_VISIBLE_KEY, PersistentDataType.BYTE, (byte) 1);
        }

        item.setItemMeta(mapMeta);
        LoreUtil.updateMapLore(item);
        AuditLogger.log("locked", player.getName(), mapUUID);
        player.sendMessage("§aMap locked successfully.");
    }
}
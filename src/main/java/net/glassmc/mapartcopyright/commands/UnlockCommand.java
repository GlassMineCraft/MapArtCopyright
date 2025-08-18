package net.glassmc.mapartcopyright.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

import net.glassmc.mapartcopyright.Audit.AuditLogger;
import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import net.glassmc.mapartcopyright.economy.EconomyUtil;
import net.glassmc.mapartcopyright.util.LockUtil;
import net.glassmc.mapartcopyright.util.UnlockUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

public class UnlockCommand implements SubCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    @Override
    public String getName() {
        return "unlock";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can unlock maps.");
            return;
        }

        if (!player.hasPermission("mapart.unlock")) {
            player.sendMessage("§cYou don’t have permission to unlock maps.");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getItemMeta() == null || !(item.getItemMeta() instanceof MapMeta)) {
            player.sendMessage("§cHold a filled map to unlock it.");
            return;
        }

        MapMeta meta = (MapMeta) item.getItemMeta();
        if (!UnlockUtil.isLocked(item)) {
            player.sendMessage("§eThis map is not locked.");
            return;
        }

        String mapUUID = meta.getPersistentDataContainer().get(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING);
        if (mapUUID == null) {
            player.sendMessage("§cThis map has no ownership data.");
            return;
        }

        boolean isOwner = OwnershipDatabase.isOwner(player.getUniqueId(), mapUUID);
        if (!isOwner && !player.hasPermission("mapart.bypass")) {
            player.sendMessage("§cYou are not the owner of this map.");
            return;
        }

        if (EconomyUtil.isEnabled() && !player.hasPermission("mapart.free")) {
            double cost = EconomyUtil.getCost("unlock");
            if (!EconomyUtil.charge(player, cost, true)) {
                return;
            }
        }

        // Restore display name if previously saved in NBT
        String storedName = meta.getPersistentDataContainer().get(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING);
        if (storedName != null && meta.displayName() == null) {
            Component displayName = MINI.deserialize(storedName);
            meta.displayName(displayName);
        }

        // Unlock the map (custom flag only)
        UnlockUtil.unlock(item);
        item.setItemMeta(meta);

        AuditLogger.log("unlocked", player.getName(), mapUUID);
        player.sendMessage("§aMap unlocked.");
    }
}
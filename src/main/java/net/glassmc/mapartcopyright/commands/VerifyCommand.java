package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.util.LockUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class VerifyCommand implements SubCommand {

    @Override
    public String getName() {
        return "verify";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player target;

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can verify their own maps.");
                return;
            }
            if (!player.hasPermission("mapart.verify")) {
                player.sendMessage("§cYou don’t have permission to verify map art.");
                return;
            }
            target = player;

        } else if (args.length == 2) {
            if (!sender.hasPermission("mapart.verify.others")) {
                sender.sendMessage("§cYou don’t have permission to verify maps for others.");
                return;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or not online.");
                return;
            }
        } else {
            sender.sendMessage("§cUsage: /mapart verify [player]");
            return;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (!(item.getItemMeta() instanceof MapMeta meta)) {
            sender.sendMessage("§cThat player is not holding a valid filled map.");
            return;
        }

        String mapName = meta.getPersistentDataContainer().get(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING);
        if (!meta.getPersistentDataContainer().has(LockUtil.MAPART_NAME_KEY, PersistentDataType.STRING)) {
            sender.sendMessage("§cVerification failed: Map name is not a string or was corrupted.");
            return;
        }

        String creatorUUIDRaw = meta.getPersistentDataContainer().get(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING);

        sender.sendMessage("§7Verifying map for §f" + target.getName());

        if (mapName == null || creatorUUIDRaw == null) {
            sender.sendMessage("§cVerification failed: missing map name or creator UUID.");
            return;
        }

        sender.sendMessage("§7Map Name: §f" + mapName);
        sender.sendMessage("§7Creator UUID: §f" + creatorUUIDRaw);

        try {
            UUID creatorUUID = UUID.fromString(creatorUUIDRaw);
            boolean isMatch = creatorUUID.equals(target.getUniqueId());
            sender.sendMessage(isMatch ? "§aVerification PASSED." : "§cVerification FAILED.");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§cVerification failed: malformed creator UUID.");
        }
    }
}

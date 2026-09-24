package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.api.MapArtAPI;
import net.glassmc.mapartcopyright.util.LockUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.UUID;

public class VerifyCommand implements SubCommand {
    @Override public String getName() { return "verify"; }

    @Override public void execute(CommandSender sender, String[] args) {
        Player target;
        if (args.length == 1 && sender instanceof Player player) {
            if (!sender.hasPermission("mapart.verify")) {
                sender.sendMessage("§cYou do not have permission to verify map art.");
                return;
            }
            target = player;
        } else if (args.length == 2) {
            if (!sender.hasPermission("mapart.verify.others")) {
                sender.sendMessage("§cYou do not have permission to verify other players' maps.");
                return;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("§cPlayer is not online."); return; }
        } else {
            sender.sendMessage("§cUsage: /mapart verify [online player]");
            return;
        }
        var item = target.getInventory().getItemInMainHand();
        if (!(item.getItemMeta() instanceof MapMeta meta)) {
            sender.sendMessage("§cThat player is not holding a filled map.");
            return;
        }
        String id = MapArtAPI.getMapUUID(item);
        UUID owner = MapArtAPI.getOwner(item);
        sender.sendMessage("§7Map name: §f" + MapArtAPI.getStoredMapName(item).orElse("Untitled"));
        sender.sendMessage("§7Map UUID: §f" + (id == null ? "unregistered" : id));
        sender.sendMessage("§7Registered owner: §f" + (owner == null ? "not available" : owner));
        sender.sendMessage(owner != null && owner.equals(target.getUniqueId())
                ? "§aRegistered ownership verified." : "§eRegistered ownership could not be verified for this player.");
        var data = meta.getPersistentDataContainer();
        String creator = data.has(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING)
                ? data.get(LockUtil.CREATOR_UUID_KEY, PersistentDataType.STRING) : null;
        sender.sendMessage("§7Creator UUID (attribution only): §f" + (creator == null ? "not recorded" : creator));
    }
}

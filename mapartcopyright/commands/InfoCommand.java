package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.util.LockUtil;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

public class InfoCommand implements SubCommand {

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        if (!player.hasPermission("mapart.info")) {
            player.sendMessage("§cYou do not have permission to view map info.");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.FILLED_MAP || !(item.getItemMeta() instanceof MapMeta meta)) {
            player.sendMessage("§cYou must be holding a filled map.");
            return;
        }

        String uuid = meta.getPersistentDataContainer().get(LockUtil.MAPART_ID_KEY, PersistentDataType.STRING);
        String credit = meta.getPersistentDataContainer().get(LockUtil.CREDIT_KEY, PersistentDataType.STRING);
        boolean locked = meta.getPersistentDataContainer().getOrDefault(LockUtil.LOCK_KEY, PersistentDataType.BYTE, (byte) 0) == 1;

        player.sendMessage("§8§m-----------------------------");
        player.sendMessage("§bMapArt Information:");
        player.sendMessage(" §7UUID: §f" + (uuid != null ? uuid : "§cNone"));
        player.sendMessage(" §7Creator: §f" + (credit != null ? credit : "§cNone"));
        player.sendMessage(" §7Locked: " + (locked ? "§aYes" : "§cNo"));
        player.sendMessage("§8§m-----------------------------");
    }
}

package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.util.LockUtil;
import net.glassmc.mapartcopyright.util.CreditUtil;
import net.glassmc.mapartcopyright.util.LoreUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

public class CreditCommand implements SubCommand {

    @Override
    public String getName() {
        return "credit";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can set credit.");
            return;
        }
        
        if (!player.hasPermission("mapart.credit")) {
            player.sendMessage("§cYou don’t have permission to do this.");
            return;
        }


        if (args.length < 2) {
            player.sendMessage("§cUsage: /mapart credit <name>");
            return;
        }

        String credit = String.join(" ", args).substring(args[0].length()).trim();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.hasItemMeta()) {
            player.sendMessage("§cHold a filled map to credit it.");
            return;
        }

        CreditUtil.setCredit(item, credit);
        player.sendMessage("§aMap credited to: " + credit);
    }
}
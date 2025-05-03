package net.glassmc.mapartcopyright.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;


public class NameCommand implements SubCommand {

    @Override
    public String getName() {
        return "name";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can name maps.");
            return;
        }
        
        if (!player.hasPermission("mapart.name")) {
            player.sendMessage("§cYou don’t have permission to do this.");
            return;
        }


        if (args.length < 2) {
            player.sendMessage("§cUsage: /mapart name <title>");
            return;
        }

        String title = String.join(" ", args).substring(args[0].length()).trim();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getItemMeta() instanceof MapMeta mapMeta) {
            mapMeta.setDisplayName(title);
            item.setItemMeta(mapMeta);
            player.sendMessage("§aMap named: " + title);
        } else {
            player.sendMessage("§cHold a filled map to name it.");
        }
    }
}

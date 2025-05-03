package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.gui.MapArtGUI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class MenuCommand implements SubCommand {

    @Override
    public String getName() {
        return "menu";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can open the menu.");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !(item.getItemMeta() instanceof MapMeta)) {
            player.sendMessage("§cYou must be holding a filled map to use this.");
            return;
        }

        MapArtGUI.open(player, item);
    }
}

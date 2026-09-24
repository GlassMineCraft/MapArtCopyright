package net.glassmc.mapartcopyright.commands;
import net.glassmc.mapartcopyright.gui.MapArtGUI;
import net.glassmc.mapartcopyright.util.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MenuCommand implements SubCommand {
    public String getName() { return "menu"; }
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "only-players", "§cOnly players can use this command.");
            return;
        }
        MapArtGUI.open(player, player.getInventory().getItemInMainHand());
    }
}

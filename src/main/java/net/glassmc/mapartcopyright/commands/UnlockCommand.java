package net.glassmc.mapartcopyright.commands;
import net.glassmc.mapartcopyright.service.MapArtService;
import net.glassmc.mapartcopyright.util.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnlockCommand implements SubCommand {
    public String getName() { return "unlock"; }
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "only-players", "§cOnly players can use this command.");
            return;
        }
        MapArtService.unlock(player, player.getInventory().getItemInMainHand());
    }
}

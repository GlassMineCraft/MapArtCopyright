package net.glassmc.mapartcopyright.commands;
import net.glassmc.mapartcopyright.service.MapArtService;
import net.glassmc.mapartcopyright.util.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LockCommand implements SubCommand {
    public String getName() { return "lock"; }
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "only-players", "§cOnly players can use this command.");
            return;
        }
        MapArtService.lock(player, player.getInventory().getItemInMainHand());
    }
}

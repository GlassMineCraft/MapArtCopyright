package net.glassmc.mapartcopyright.commands;
import net.glassmc.mapartcopyright.service.MapArtService;
import net.glassmc.mapartcopyright.util.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Arrays;

public class CreditCommand implements SubCommand {
    public String getName() { return "credit"; }
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "only-players", "§cOnly players can use this command.");
            return;
        }
        if (args.length < 2) { player.sendMessage("§cUsage: /mapart credit <text>"); return; }
        MapArtService.credit(player, player.getInventory().getItemInMainHand(), String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
    }
}

package net.glassmc.mapartcopyright.commands;

import org.bukkit.command.CommandSender;

public class HelpCommand implements SubCommand {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§8§m---------------- §r§bMapArt Commands§8 §m----------------");
        sender.sendMessage("§e/mapart lock §7- Register and lock the held map");
        sender.sendMessage("§e/mapart unlock §7- Permit copies; retain registered ownership");
        sender.sendMessage("§e/mapart name <name> §7- Rename the held map");
        sender.sendMessage("§e/mapart credit <name> §7- Set creator credit for the held map");
        sender.sendMessage("§e/mapart verify §7- Check registered ownership and creator attribution");
        sender.sendMessage("§e/mapart info §7- Show map UUID and stored metadata");
        sender.sendMessage("§e/mapart audit <map-uuid> [page] §7- View audit logs for a map");
        sender.sendMessage("§e/mapart export §7- Export ownership records (mapart.export)");
        sender.sendMessage("§e/mapart menu §7- Open the GUI for managing the held map");
        sender.sendMessage("§8§m----------------------------------------------------");
    }
}

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
        sender.sendMessage("§e/mapart lock §7- Lock the held map and assign ownership");
        sender.sendMessage("§e/mapart unlock §7- Unlock the held map");
        sender.sendMessage("§e/mapart name <name> §7- Rename the held map");
        sender.sendMessage("§e/mapart credit <name> §7- Set creator credit for the held map");
        sender.sendMessage("§e/mapart verify §7- Verify if you are the creator of the held map");
        sender.sendMessage("§e/mapart info §7- Show map UUID and stored metadata");
        sender.sendMessage("§e/mapart menu §7- Open the GUI for managing the held map");
        sender.sendMessage("§8§m----------------------------------------------------");
    }
}

package net.glassmc.mapartcopyright.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class MapArtCommand implements CommandExecutor {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public MapArtCommand() {
        // Register subcommands
        register(new LockCommand());
        register(new UnlockCommand());
        register(new NameCommand());
        register(new CreditCommand());
        register(new MenuCommand());
        register(new InfoCommand());
        register(new AuditCommand());
        register(new VerifyCommand());
        register(new HelpCommand());
        register(new ExportCommand());


    }

    private void register(SubCommand subCommand) {
        subCommands.put(subCommand.getName(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
        	sender.sendMessage("§cUsage: /mapart <lock | unlock | name | credit | menu | info | audit>");
            return true;
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub != null) {
            sub.execute(sender, args);
        } else {
            sender.sendMessage("§cUnknown subcommand.");
        }

        return true;
    }
}

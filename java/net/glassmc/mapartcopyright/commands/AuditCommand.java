package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.MapArtCopyright;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class AuditCommand implements SubCommand {

    @Override
    public String getName() {
        return "audit";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mapart.audit")) {
            sender.sendMessage("§cYou do not have permission to view audits.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /mapart audit <map-uuid>");
            return;
        }

        String uuid = args[1];
        File logFile = new File(MapArtCopyright.getInstance().getDataFolder(), "audit.log");

        if (!logFile.exists()) {
            sender.sendMessage("§eNo audit log found.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            List<String> matching = reader.lines()
                    .filter(line -> line.contains(uuid))
                    .collect(Collectors.toList());

            if (matching.isEmpty()) {
                sender.sendMessage("§7No audit entries found for §f" + uuid);
            } else {
                sender.sendMessage("§6Audit entries for §e" + uuid + "§6:");
                matching.forEach(line -> sender.sendMessage("§7" + line));
            }
        } catch (IOException e) {
            sender.sendMessage("§cFailed to read audit log.");
            e.printStackTrace();
        }
    }
}

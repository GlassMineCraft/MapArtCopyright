package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.Audit.AuditLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.io.IOException;
import java.util.UUID;

public class AuditCommand implements SubCommand {
    @Override public String getName() { return "audit"; }

    @Override public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mapart.audit")) {
            sender.sendMessage("§cYou do not have permission to view audits.");
            return;
        }
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage("§cUsage: /mapart audit <map-uuid> [page]");
            return;
        }
        final UUID id;
        final int page;
        try {
            id = UUID.fromString(args[1]);
            if (!id.toString().equalsIgnoreCase(args[1])) throw new IllegalArgumentException();
            page = args.length == 3 ? Integer.parseInt(args[2]) : 1;
            if (page < 1 || page > 1000) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§cEnter a complete map UUID and a page between 1 and 1000.");
            return;
        }
        var plugin = MapArtCopyright.getInstance();
        var folder = plugin.getDataFolder().toPath();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var result = AuditLogger.recent(folder, id, page);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (sender instanceof Player player && !player.isOnline()) return;
                    if (!sender.hasPermission("mapart.audit")) return;
                    if (result.lines().isEmpty()) {
                        sender.sendMessage("§7No entries on this page. Available pages: " + result.totalPages());
                        return;
                    }
                    sender.sendMessage("§6Audit for §e" + id + "§6 (newest first, page " + page + "/" + result.totalPages() + "):");
                    result.lines().forEach(line -> sender.sendMessage("§7" + line));
                });
            } catch (IOException ex) {
                plugin.getLogger().warning("Unable to read audit log: " + ex.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§cFailed to read the audit log."));
            }
        });
    }
}

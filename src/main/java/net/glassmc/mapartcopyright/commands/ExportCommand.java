package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import org.bukkit.command.CommandSender;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class ExportCommand implements SubCommand {
    @Override public String getName() { return "export"; }

    public static String csv(String value) {
        if (value == null) return "\"\"";
        // Prevent spreadsheet formula execution when an administrator opens the export.
        String leading = value.stripLeading();
        if ((!leading.isEmpty() && "=+@-".indexOf(leading.charAt(0)) >= 0)
                || value.startsWith("\t") || value.startsWith("\r"))
            value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @Override public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mapart.export")) {
            sender.sendMessage("§cYou don't have permission to export the database.");
            return;
        }
        Path file = MapArtCopyright.getInstance().getDataFolder().toPath().resolve("ownership_export.csv");
        Path temporary = file.resolveSibling("ownership_export.csv.tmp");
        try {
            var records = OwnershipDatabase.dumpAll();
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write("map_uuid,player_uuid,map_name,creator_name\n");
                for (var record : records)
                    writer.write(csv(record.mapUUID) + "," + csv(record.playerUUID.toString()) + ","
                            + csv(record.mapName) + "," + csv(record.creatorName) + "\n");
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            sender.sendMessage("§aExport complete: §f" + file.getFileName());
        } catch (IOException | IllegalStateException ex) {
            sender.sendMessage("§cExport failed. Check the server log.");
            MapArtCopyright.getInstance().getLogger().warning("Ownership export failed: " + ex.getMessage());
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
        }
    }
}

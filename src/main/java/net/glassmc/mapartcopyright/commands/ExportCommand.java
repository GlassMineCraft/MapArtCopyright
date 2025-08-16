package net.glassmc.mapartcopyright.commands;

import net.glassmc.mapartcopyright.MapArtCopyright;
import net.glassmc.mapartcopyright.database.OwnershipDatabase;
import net.glassmc.mapartcopyright.database.MapRecord;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportCommand implements SubCommand {

    @Override
    public String getName() {
        return "export";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || !player.isOp()) {
            sender.sendMessage("§cThis test command is for server operators only.");
            return;
        }

        List<MapRecord> records = OwnershipDatabase.dumpAll();
        File file = new File(MapArtCopyright.getInstance().getDataFolder(), "ownership_export.csv");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("map_uuid,player_uuid,map_name,creator_name\n");
            for (MapRecord r : records) {
                writer.write(String.format("%s,%s,%s,%s\n",
                        r.mapUUID,
                        r.playerUUID,
                        r.mapName != null ? r.mapName.replace(",", " ") : "",
                        r.creatorName != null ? r.creatorName.replace(",", " ") : ""));
            }
            player.sendMessage("§aExport complete: §f" + file.getName());
        } catch (IOException e) {
            player.sendMessage("§cExport failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

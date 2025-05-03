package net.glassmc.mapartcopyright.Audit;

import net.glassmc.mapartcopyright.MapArtCopyright;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class AuditLogger {

    private static final File logFile = new File(MapArtCopyright.getInstance().getDataFolder(), "audit.log");

    public static void log(String action, String playerName, String mapUUID) {
        String time = LocalDateTime.now().toString();
        String message = "[" + time + "] " + playerName + " " + action + " map UUID: " + mapUUID;

        // Log to console
        MapArtCopyright.getInstance().getLogger().info(message);

        // Log to file
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

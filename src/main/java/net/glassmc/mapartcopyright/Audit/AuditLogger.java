package net.glassmc.mapartcopyright.Audit;

import net.glassmc.mapartcopyright.MapArtCopyright;
import org.bukkit.entity.Player;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

public final class AuditLogger {
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final int BACKUPS = 3;
    private AuditLogger() {}

    public record Page(List<String> lines, int totalPages) {}

    public static void log(String action, Player player, String mapUUID, String details) {
        write(action, player.getName() + " actor_uuid=" + player.getUniqueId(), mapUUID, details);
    }

    /** Compatibility entry point for automation and trusted integrations. */
    public static void log(String action, String playerName, String mapUUID) {
        write(action, playerName, mapUUID, "");
    }

    private static String singleLine(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\p{Cntrl}]", " ");
    }

    private static synchronized void write(String action, String actor, String mapUUID, String details) {
        var plugin = MapArtCopyright.getInstance();
        String message = "[" + Instant.now() + "] " + singleLine(actor) + " " + singleLine(action)
                + " map UUID: " + singleLine(mapUUID) + " " + singleLine(details);
        try {
            Path folder = plugin.getDataFolder().toPath();
            Files.createDirectories(folder);
            Path log = folder.resolve("audit.log");
            if (Files.exists(log) && Files.size(log) >= MAX_BYTES) {
                for (int i = BACKUPS; i >= 1; i--) {
                    Path source = folder.resolve(i == 1 ? "audit.log" : "audit.log." + (i - 1));
                    if (Files.exists(source)) Files.move(source, folder.resolve("audit.log." + i),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Files.writeString(log, message + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            // Logging failure must never undo an already committed item/payment operation.
            plugin.getLogger().severe("Unable to write audit entry: " + message + ": " + ex.getMessage());
        }
    }

    /** Read newest entries first, retaining only enough matches for the requested page. */
    public static Page recent(Path folder, UUID mapUUID, int page) throws IOException {
        if (page < 1 || page > 1000) throw new IllegalArgumentException("Page must be between 1 and 1000.");
        Pattern marker = Pattern.compile("map UUID: " + Pattern.quote(mapUUID.toString()) + "(?![0-9a-fA-F-])");
        int capacity = page * 10;
        int matches = 0;
        Deque<String> tail = new ArrayDeque<>();
        for (int i = BACKUPS; i >= 0; i--) {
            Path path = folder.resolve(i == 0 ? "audit.log" : "audit.log." + i);
            if (!Files.exists(path)) continue;
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!marker.matcher(line).find()) continue;
                    matches++;
                    tail.addLast(line);
                    if (tail.size() > capacity) tail.removeFirst();
                }
            } catch (NoSuchFileException ignored) {
                // A concurrent rotation may have retired this segment.
            }
        }
        List<String> newest = new ArrayList<>(tail);
        Collections.reverse(newest);
        int start = (page - 1) * 10;
        List<String> lines = start >= newest.size() ? List.of()
                : List.copyOf(newest.subList(start, Math.min(start + 10, newest.size())));
        return new Page(lines, Math.min(1000, (matches + 9) / 10));
    }
}

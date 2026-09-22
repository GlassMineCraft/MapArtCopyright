package net.glassmc.mapartcopyright;

import net.glassmc.mapartcopyright.Audit.AuditLogger;
import net.glassmc.mapartcopyright.commands.ExportCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuditAndExportTest {
    @TempDir Path folder;

    @Test void auditShowsNewestMatchesAfterHundredthEntryAndIgnoresPrefixCollisions() throws Exception {
        UUID id = UUID.randomUUID();
        StringBuilder log = new StringBuilder();
        for (int i = 1; i <= 125; i++) log.append("entry-").append(i).append(" map UUID: ").append(id).append("\n");
        log.append("not-this-map map UUID: ").append(id).append("a\n");
        Files.writeString(folder.resolve("audit.log"), log);
        var newest = AuditLogger.recent(folder, id, 1);
        assertEquals(13, newest.totalPages());
        assertEquals(10, newest.lines().size());
        assertTrue(newest.lines().getFirst().startsWith("entry-125 "));
        var last = AuditLogger.recent(folder, id, 13);
        assertEquals(5, last.lines().size());
        assertTrue(last.lines().getLast().startsWith("entry-1 "));
    }

    @Test void recentAuditIncludesRotatedLogsInCorrectOrder() throws Exception {
        UUID id = UUID.randomUUID();
        Files.writeString(folder.resolve("audit.log.1"), "older map UUID: " + id + "\n");
        Files.writeString(folder.resolve("audit.log"), "newer map UUID: " + id + "\n");
        var result = AuditLogger.recent(folder, id, 1);
        assertEquals(2, result.lines().size());
        assertTrue(result.lines().getFirst().startsWith("newer "));
    }

    @Test void csvEscapesCommasQuotesNewlinesAndFormulaPrefixes() {
        assertEquals("\"A, \"\"quote\"\"\nline\"", ExportCommand.csv("A, \"quote\"\nline"));
        assertEquals("\"'=1+1\"", ExportCommand.csv("=1+1"));
        assertEquals("\"'=1\n+1\"", ExportCommand.csv("=1\n+1"));
        assertEquals("\"\"", ExportCommand.csv(null));
    }
}

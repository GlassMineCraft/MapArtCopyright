package net.glassmc.mapartcopyright.database;

import net.glassmc.mapartcopyright.MapArtCopyright;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OwnershipDatabase {

    private static Connection connection;

    public static void connect() {
        try {
            FileConfiguration config = MapArtCopyright.getInstance().getConfig();
            boolean mysqlEnabled = config.getBoolean("mysql.enabled", false);

            if (mysqlEnabled) {
                String host = config.getString("mysql.host", "localhost");
                int port = config.getInt("mysql.port", 3306);
                String db = config.getString("mysql.database", "mapart");
                String user = config.getString("mysql.username", "root");
                String pass = config.getString("mysql.password", "");
                boolean useSSL = config.getBoolean("mysql.useSSL", false);
                boolean allowKey = config.getBoolean("mysql.allowPublicKeyRetrieval", true);

                String url = "jdbc:mysql://" + host + ":" + port + "/" + db +
                             "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=" + allowKey;

                connection = DriverManager.getConnection(url, user, pass);
            } else {
                String dbPath = MapArtCopyright.getInstance().getDataFolder() + "/Mapart.db";
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS map_ownership (
                    map_uuid VARCHAR(64) PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    map_name TEXT,
                    creator_name TEXT
                )
            """)) {
                stmt.executeUpdate();                
            }
         // Safe column addition: will fail silently if already exists
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("ALTER TABLE map_ownership ADD COLUMN map_name TEXT");
            } catch (SQLException ignored) {}

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("ALTER TABLE map_ownership ADD COLUMN creator_name TEXT");
            } catch (SQLException ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isOwner(UUID playerUUID, String mapUUID) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT player_uuid FROM map_ownership WHERE map_uuid = ?")) {
            stmt.setString(1, mapUUID);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && playerUUID.toString().equals(rs.getString("player_uuid"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void setOwner(String mapUUID, UUID playerUUID) {
        setOwner(mapUUID, playerUUID, null, null);
    }

    public static void setOwner(String mapUUID, UUID playerUUID, String mapName, String creatorName) {
        try (PreparedStatement stmt = connection.prepareStatement("""
            REPLACE INTO map_ownership (map_uuid, player_uuid, map_name, creator_name)
            VALUES (?, ?, ?, ?)
        """)) {
            stmt.setString(1, mapUUID);
            stmt.setString(2, playerUUID.toString());
            stmt.setString(3, mapName);
            stmt.setString(4, creatorName);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static UUID getOwner(String mapUUID) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT player_uuid FROM map_ownership WHERE map_uuid = ?")) {
            stmt.setString(1, mapUUID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return UUID.fromString(rs.getString("player_uuid"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<MapRecord> dumpAll() {
        List<MapRecord> records = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM map_ownership")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String mapUUID = rs.getString("map_uuid");
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                String mapName = rs.getString("map_name");
                String creatorName = rs.getString("creator_name");

                records.add(new MapRecord(mapUUID, playerUUID, mapName, creatorName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return records;
    }
}

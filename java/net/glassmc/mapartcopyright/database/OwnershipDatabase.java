package net.glassmc.mapartcopyright.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;

import net.glassmc.mapartcopyright.MapArtCopyright;

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
                        player_uuid VARCHAR(36) NOT NULL
                    )
            """)) {
                stmt.executeUpdate();
            }
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
        try (PreparedStatement stmt = connection.prepareStatement("REPLACE INTO map_ownership (map_uuid, player_uuid) VALUES (?, ?)")) {
            stmt.setString(1, mapUUID);
            stmt.setString(2, playerUUID.toString());
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
}

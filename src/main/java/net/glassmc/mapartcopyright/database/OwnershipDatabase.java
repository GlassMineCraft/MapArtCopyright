package net.glassmc.mapartcopyright.database;

import net.glassmc.mapartcopyright.MapArtCopyright;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class OwnershipDatabase {

    private static Connection connection;

    public static void connect() {
        try {
            String dbPath = MapArtCopyright.getInstance().getDataFolder() + "/Mapart.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            try (PreparedStatement stmt = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS map_ownership (
                        map_uuid TEXT PRIMARY KEY,
                        player_uuid TEXT NOT NULL
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
		// TODO Auto-generated method stub
		return null;
	}
}

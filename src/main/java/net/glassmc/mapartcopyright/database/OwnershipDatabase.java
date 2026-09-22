package net.glassmc.mapartcopyright.database;

import net.glassmc.mapartcopyright.MapArtCopyright;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.sql.*;
import java.util.*;

/** Serialized JDBC access. Writes never replace an existing owner. */
public final class OwnershipDatabase {
    private static Connection connection;
    private OwnershipDatabase() {}

    public static synchronized boolean connect() {
        close();
        try {
            var plugin = MapArtCopyright.getInstance();
            FileConfiguration config = plugin.getConfig();
            File folder = plugin.getDataFolder();
            if (!folder.isDirectory() && !folder.mkdirs()) throw new SQLException("Cannot create data directory");
            String type = config.getString("database.type", "h2").toLowerCase(Locale.ROOT);
            Properties properties = new Properties();
            connection = switch (type) {
                case "h2" -> new org.h2.Driver().connect("jdbc:h2:file:" + new File(folder, "mapart").getAbsolutePath(), properties);
                case "sqlite" -> new org.sqlite.JDBC().connect("jdbc:sqlite:" + new File(folder, "Mapart.db").getAbsolutePath(), properties);
                case "mysql" -> {
                    properties.setProperty("user", config.getString("mysql.username", "root"));
                    properties.setProperty("password", config.getString("mysql.password", ""));
                    properties.setProperty("useSSL", Boolean.toString(config.getBoolean("mysql.useSSL", false)));
                    properties.setProperty("allowPublicKeyRetrieval", Boolean.toString(config.getBoolean("mysql.allowPublicKeyRetrieval", true)));
                    properties.setProperty("connectTimeout", "10000");
                    properties.setProperty("socketTimeout", "10000");
                    String url = "jdbc:mysql://" + config.getString("mysql.host", "localhost") + ":"
                            + config.getInt("mysql.port", 3306) + "/" + config.getString("mysql.database", "mapart");
                    yield new com.mysql.cj.jdbc.Driver().connect(url, properties);
                }
                default -> throw new SQLException("Unsupported database.type: " + type);
            };
            try (Statement statement = requireConnection().createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS map_ownership (
                            map_uuid VARCHAR(64) PRIMARY KEY,
                            player_uuid VARCHAR(36) NOT NULL,
                            map_name TEXT,
                            creator_name TEXT
                        )
                        """);
            }
            plugin.getLogger().info("Ownership database connected: " + type);
            return true;
        } catch (SQLException | RuntimeException ex) {
            close();
            MapArtCopyright.getInstance().getLogger().severe("Ownership database unavailable: " + ex.getMessage());
            return false;
        }
    }

    public static synchronized boolean isConnected() {
        try { return connection != null && !connection.isClosed(); }
        catch (SQLException ex) { return false; }
    }

    private static Connection requireConnection() throws SQLException {
        if (!isConnected()) throw new SQLException("Ownership database is unavailable");
        return connection;
    }

    public static synchronized MapRecord find(String id) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement("SELECT * FROM map_ownership WHERE map_uuid = ?")) {
            statement.setString(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? record(results) : null;
            }
        }
    }

    /** INSERT only: a conflicting UUID must never transfer ownership. */
    public static synchronized void register(MapRecord record) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement(
                "INSERT INTO map_ownership (map_uuid, player_uuid, map_name, creator_name) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, record.mapUUID);
            statement.setString(2, record.playerUUID.toString());
            statement.setString(3, record.mapName);
            statement.setString(4, record.creatorName);
            statement.executeUpdate();
        }
    }

    public static synchronized void updateMetadata(MapRecord record) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement(
                "UPDATE map_ownership SET map_name = ?, creator_name = ? WHERE map_uuid = ? AND player_uuid = ?")) {
            statement.setString(1, record.mapName);
            statement.setString(2, record.creatorName);
            statement.setString(3, record.mapUUID);
            statement.setString(4, record.playerUUID.toString());
            if (statement.executeUpdate() != 1) throw new SQLException("Ownership changed or record is missing");
        }
    }

    public static synchronized void rollbackRegistration(String id, UUID owner) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement(
                "DELETE FROM map_ownership WHERE map_uuid = ? AND player_uuid = ?")) {
            statement.setString(1, id);
            statement.setString(2, owner.toString());
            statement.executeUpdate();
        }
    }

    public static synchronized void restoreMetadataIfUnchanged(MapRecord expected, MapRecord previous) throws SQLException {
        MapRecord current = find(expected.mapUUID);
        if (current != null && current.playerUUID.equals(expected.playerUUID)
                && Objects.equals(current.mapName, expected.mapName) && Objects.equals(current.creatorName, expected.creatorName)) {
            updateMetadata(previous);
        }
    }

    public static boolean isOwner(UUID player, String id) {
        UUID owner = getOwner(id);
        return player != null && player.equals(owner);
    }

    public static UUID getOwner(String id) {
        if (id == null) return null;
        try {
            MapRecord record = find(id);
            return record == null ? null : record.playerUUID;
        } catch (SQLException ex) { return null; }
    }

    /** Compatibility entry point; ownership replacement is intentionally rejected. */
    @Deprecated
    public static synchronized void setOwner(String id, UUID owner, String name, String credit) {
        try {
            MapRecord current = find(id);
            MapRecord next = new MapRecord(id, owner, name, credit);
            if (current == null) register(next);
            else if (current.playerUUID.equals(owner)) updateMetadata(next);
            else throw new IllegalStateException("Cannot replace an existing map owner");
        } catch (SQLException ex) { throw new IllegalStateException("Ownership was not saved", ex); }
    }

    @Deprecated
    public static void setOwner(String id, UUID owner) { setOwner(id, owner, null, null); }

    public static synchronized List<MapRecord> dumpAll() {
        try (Statement statement = requireConnection().createStatement();
             ResultSet results = statement.executeQuery("SELECT * FROM map_ownership ORDER BY map_uuid")) {
            List<MapRecord> records = new ArrayList<>();
            while (results.next()) records.add(record(results));
            return records;
        } catch (SQLException ex) { throw new IllegalStateException("Could not read ownership records", ex); }
    }

    private static MapRecord record(ResultSet result) throws SQLException {
        try {
            return new MapRecord(result.getString("map_uuid"), UUID.fromString(result.getString("player_uuid")),
                    result.getString("map_name"), result.getString("creator_name"));
        } catch (IllegalArgumentException ex) { throw new SQLException("Invalid owner UUID in database", ex); }
    }

    public static synchronized void close() {
        if (connection != null) {
            try { connection.close(); }
            catch (SQLException ex) { MapArtCopyright.getInstance().getLogger().warning("Database close failed: " + ex.getMessage()); }
            finally { connection = null; }
        }
    }
}

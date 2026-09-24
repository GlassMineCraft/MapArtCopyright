package net.glassmc.mapartcopyright.database;

import net.glassmc.mapartcopyright.artwork.*;
import java.sql.*;
import java.util.*;

/** Additive tables: existing single-map ownership rows and IDs are preserved. */
public final class ArtworkDatabase {
    private ArtworkDatabase() {}

    static void createTables(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS map_artworks (
                        artwork_uuid VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL,
                        width INTEGER NOT NULL, height INTEGER NOT NULL, map_name TEXT NOT NULL, creator_name TEXT,
                        locked INTEGER NOT NULL, name_visible INTEGER NOT NULL,
                        hologram_visible INTEGER NOT NULL, frame_locked INTEGER NOT NULL, revision INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS map_artwork_tiles (
                        map_uuid VARCHAR(64) PRIMARY KEY, artwork_uuid VARCHAR(36) NOT NULL,
                        tile_x INTEGER NOT NULL, tile_y INTEGER NOT NULL,
                        UNIQUE (artwork_uuid, tile_x, tile_y)
                    )
                    """);
        }
    }

    public static ArtworkMembership membership(String mapId) throws SQLException {
        if (mapId == null) return null;
        return OwnershipDatabase.read(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT a.*, t.map_uuid, t.tile_x, t.tile_y FROM map_artwork_tiles t
                    LEFT JOIN map_artworks a ON a.artwork_uuid = t.artwork_uuid WHERE t.map_uuid = ?
                    """)) {
                statement.setString(1, mapId);
                try (var result = statement.executeQuery()) {
                    if (!result.next()) return null;
                    var artwork = record(result);
                    var tile = new ArtworkTile(mapId, result.getInt("tile_x"), result.getInt("tile_y"));
                    if (tile.x() < 0 || tile.y() < 0 || tile.x() >= artwork.size().width() || tile.y() >= artwork.size().height())
                        throw new SQLException("Artwork tile coordinates are corrupt");
                    return new ArtworkMembership(artwork, tile);
                }
            }
        });
    }

    public static List<ArtworkTile> tiles(UUID artworkId) throws SQLException {
        return OwnershipDatabase.read(connection -> {
            List<ArtworkTile> tiles = new ArrayList<>();
            try (var statement = connection.prepareStatement("SELECT map_uuid, tile_x, tile_y FROM map_artwork_tiles WHERE artwork_uuid = ? ORDER BY tile_y, tile_x")) {
                statement.setString(1, artworkId.toString());
                try (var rows = statement.executeQuery()) {
                    while (rows.next()) tiles.add(new ArtworkTile(rows.getString(1), rows.getInt(2), rows.getInt(3)));
                }
            }
            return List.copyOf(tiles);
        });
    }

    public static void create(ArtworkRecord artwork, List<ArtworkTile> tiles,
                              Map<String, MapRecord> previous, Runnable applyItems) throws SQLException {
        validateTiles(artwork, tiles);
        OwnershipDatabase.transaction(connection -> {
            for (var tile : tiles) {
                MapRecord expected = previous.get(tile.mapId());
                MapRecord current = OwnershipDatabase.find(tile.mapId());
                if (expected == null ? current != null : current == null
                        || !expected.playerUUID.equals(current.playerUUID)
                        || !Objects.equals(expected.mapName, current.mapName)
                        || !Objects.equals(expected.creatorName, current.creatorName))
                    throw new SQLException("A tile's ownership record changed during registration");
                if (current != null && !current.playerUUID.equals(artwork.owner()))
                    throw new SQLException("Every tile must belong to the artwork owner");
                if (membership(tile.mapId()) != null) throw new SQLException("A tile is already part of an artwork");
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO map_artworks (map_name, creator_name, locked, name_visible, hologram_visible,
                        frame_locked, revision, artwork_uuid, player_uuid, width, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                bindState(statement, artwork);
                statement.setString(8, artwork.id().toString());
                statement.setString(9, artwork.owner().toString());
                statement.setInt(10, artwork.size().width());
                statement.setInt(11, artwork.size().height());
                statement.executeUpdate();
            }
            for (var tile : tiles) {
                var ownership = new MapRecord(tile.mapId(), artwork.owner(), artwork.name(), artwork.credit());
                if (previous.containsKey(tile.mapId())) OwnershipDatabase.updateMetadata(ownership);
                else OwnershipDatabase.register(ownership);
                try (var statement = connection.prepareStatement("INSERT INTO map_artwork_tiles (map_uuid, artwork_uuid, tile_x, tile_y) VALUES (?, ?, ?, ?)")) {
                    statement.setString(1, tile.mapId());
                    statement.setString(2, artwork.id().toString());
                    statement.setInt(3, tile.x());
                    statement.setInt(4, tile.y());
                    statement.executeUpdate();
                }
            }
            applyItems.run();
            return null;
        });
    }

    public static void update(ArtworkRecord previous, ArtworkRecord next, Runnable applyItem) throws SQLException {
        if (!previous.id().equals(next.id()) || !previous.owner().equals(next.owner()) || !previous.size().equals(next.size())
                || next.revision() != previous.revision() + 1) throw new SQLException("Artwork identity cannot be changed");
        OwnershipDatabase.transaction(connection -> {
            var members = tiles(previous.id());
            validateTiles(previous, members);
            try (var statement = connection.prepareStatement("""
                    UPDATE map_artworks SET map_name = ?, creator_name = ?, locked = ?, name_visible = ?,
                    hologram_visible = ?, frame_locked = ?, revision = ? WHERE artwork_uuid = ? AND player_uuid = ? AND revision = ?
                    """)) {
                bindState(statement, next);
                statement.setString(8, previous.id().toString());
                statement.setString(9, previous.owner().toString());
                statement.setInt(10, previous.revision());
                if (statement.executeUpdate() != 1) throw new SQLException("Artwork changed; try again");
            }
            for (var tile : members)
                OwnershipDatabase.updateMetadata(new MapRecord(tile.mapId(), next.owner(), next.name(), next.credit()));
            applyItem.run();
            return null;
        });
    }

    private static void validateTiles(ArtworkRecord artwork, List<ArtworkTile> tiles) throws SQLException {
        Set<String> ids = new HashSet<>();
        Set<Integer> positions = new HashSet<>();
        for (var tile : tiles) {
            if (tile.mapId() == null || !ids.add(tile.mapId()) || tile.x() < 0 || tile.y() < 0
                    || tile.x() >= artwork.size().width() || tile.y() >= artwork.size().height()
                    || !positions.add(tile.y() * artwork.size().width() + tile.x())) throw new SQLException("Invalid or duplicated artwork tile");
        }
        if (tiles.size() != artwork.size().tiles()) throw new SQLException("Artwork is missing registered tiles");
    }

    private static void bindState(PreparedStatement statement, ArtworkRecord artwork) throws SQLException {
        statement.setString(1, artwork.name());
        statement.setString(2, artwork.credit());
        statement.setInt(3, artwork.locked() ? 1 : 0);
        statement.setInt(4, artwork.nameVisible() ? 1 : 0);
        statement.setInt(5, artwork.hologramVisible() ? 1 : 0);
        statement.setInt(6, artwork.frameLocked() ? 1 : 0);
        statement.setInt(7, artwork.revision());
    }

    private static ArtworkRecord record(ResultSet row) throws SQLException {
        try {
            return new ArtworkRecord(UUID.fromString(row.getString("artwork_uuid")), UUID.fromString(row.getString("player_uuid")),
                    new ArtworkSize(row.getInt("width"), row.getInt("height")), row.getString("map_name"), row.getString("creator_name"),
                    row.getInt("locked") == 1, row.getInt("name_visible") == 1, row.getInt("hologram_visible") == 1,
                    row.getInt("frame_locked") == 1, row.getInt("revision"));
        } catch (IllegalArgumentException | NullPointerException ex) { throw new SQLException("Invalid artwork record", ex); }
    }
}

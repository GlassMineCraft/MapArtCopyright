package net.glassmc.mapartcopyright.database;

import java.util.UUID;

public class MapRecord {
    public final String mapUUID;
    public final UUID playerUUID;
    public final String mapName;
    public final String creatorName;

    public MapRecord(String mapUUID, UUID playerUUID, String mapName, String creatorName) {
        this.mapUUID = mapUUID;
        this.playerUUID = playerUUID;
        this.mapName = mapName;
        this.creatorName = creatorName;
    }
}

package net.glassmc.mapartcopyright.artwork;

import java.util.Objects;
import java.util.UUID;

/** Database-authoritative state shared by every registered copy of every tile. */
public record ArtworkRecord(UUID id, UUID owner, ArtworkSize size, String name, String credit,
                            boolean locked, boolean nameVisible, boolean hologramVisible,
                            boolean frameLocked, int revision) {
    public ArtworkRecord {
        Objects.requireNonNull(id);
        Objects.requireNonNull(owner);
        Objects.requireNonNull(size);
        Objects.requireNonNull(name);
        if (revision < 0) throw new IllegalArgumentException("Invalid artwork revision");
    }
}

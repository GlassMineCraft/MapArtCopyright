# Upgrading to 1.3.0 — Paper 26.x and artwork walls

## Requirements

- Java 25; minimum declared server API is Paper 26.1.2.
- Automated compatibility coverage for Paper API 26.1.2 build 72 and 26.2 build 111 with their matching MockBukkit releases. This does not guarantee every future 26.x release or replace live-server testing.
- Vault, plus an economy provider if enabled with nonzero fees. CMILib is optional.
- This JAR is not for Java 21 / Paper 1.21.x. Keep the previous JAR and a matching backup if that server line is still needed.

## Safe upgrade

1. Stop the server. Back up the plugin JAR, complete plugin data directory, world/player data (including map data and item metadata), and the external database if using MySQL.
2. Upgrade the server runtime to Java 25 and the intended Paper 26.x build. Replace the old plugin JAR with `mapartcopyright-1.3.0.jar`; do not leave both installed.
3. Start a staging copy first. Verify database connection and Vault/provider availability in the startup log.
4. Existing `map_ownership` rows and map UUIDs are preserved. Startup creates only the additive `map_artworks` and `map_artwork_tiles` tables. No automatic grouping or re-registration of old maps occurs.
5. Grant `mapart.wall` to intended creators. Creating a wall also requires `mapart.use`, `mapart.lock`, `mapart.rename`, and `mapart.credit`. Ordinary action permissions still apply to later group edits.

## New workflow

Place filled maps in a vertical `2x1`, `2x2`, `2x3`, or `3x3` rectangle, viewed from the front. Look at the top-left frame within six blocks and run `/mapart wall create 2x2 Sunset`. Dimensions are width × height. Every registered tile must belong to the creator; duplicate registered tile UUIDs and tiles already in another artwork are rejected.

Registration locks/protects every tile and charges the normal lock fee per newly locked tile, as one withdrawal. Hold any tile and use the normal management commands/menu to change the shared title, credit, copy lock, title visibility, creator hologram, or frame protection. Later group lock/unlock fees apply to all tiles. Bypass never transfers ownership. Normal successful single-map unlock behavior remains unchanged.

Artwork state is shared across **all registered copies**, including copies made before the maps were grouped. Cached item lore refreshes on access; protection consults current database state even before refresh. Unloaded/offline copies therefore cannot retain an obsolete unlocked state. This differs intentionally from independent single-map items.

Creator holograms use the registered bottom-left tile as their anchor and center below the expected wall width. Moving that tile alone also moves its hologram; the plugin does not rearrange frames or generate map pixels. Use commands/menu instead of anvils to rename grouped tiles. There is no ungroup, resize, transfer, or regroup command in this initial implementation.

## Staging checks

- Register each preset on north/east/south/west-facing walls; also check glow frames and chunk borders.
- Confirm native map images and IDs remain intact, with one shared UUID and distinct plugin tile UUIDs.
- Check incomplete/mixed-owner/duplicate walls fail without partial registration or charges.
- Edit from a non-anchor tile; check every frame, inventory copy, container copy, and an offline player's copy after rejoin.
- Confirm visitors cannot alter protected tiles, use cartography/crafting to copy them, shoot out frames, or remove their supporting blocks. Confirm owners can retrieve tiles.
- Verify a group unlock permits copying without transferring ownership; disable separate frame protection only when intended.
- Check payment failures, database outages, and `refunds-pending.log` with the actual Vault provider. Review per-tile fees before enabling the feature for players.
- Test H2/SQLite backups and, if used, your MySQL permissions/latency. Automated tests do not exercise a real MySQL server or production economy provider.
- Verify one centered creator hologram, frame rotations, entity loading, and interactions with other protection plugins on the real server.

## Recovery and limitations

Back up all three database tables together with world/player data. `ownership_export.csv` does not contain full artwork membership/settings and is not sufficient for recovery. Missing artwork data fails closed; restore a consistent backup rather than clearing metadata or re-registering tiles.

Registration and shared metadata changes use a single JDBC transaction with revision checks. Failed item application triggers database rollback and a payment refund attempt; failed refunds are logged for reconciliation. World inventories, JDBC, and the economy provider are not crash-atomic, so an abrupt process failure still needs backup/audit reconciliation. Database access remains synchronous and a slow MySQL connection can stall server ticks.

Downgrading after creating groups would disable shared-state enforcement and leave metadata the old build does not understand. Roll back the JAR, database, and world/player backup as a consistent set. Do not downgrade only the JAR on a live artwork database.

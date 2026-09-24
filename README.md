# MapArtCopyright

A Paper plugin that registers ownership of Minecraft map art, controls copying and editing, and displays titles and creator credits in inventories and item frames.

## Requirements and build

- Java 25.
- Paper 26.1.2 or 26.2. The distributable is compiled against 26.1.2 and tested unchanged against both APIs. Other 26.x releases need separate validation.
- Vault. An economy provider is required when `economy.enabled` is true and an action has a nonzero fee.
- Maven 3.9+ to build from source. CMILib is optional; these features do not require it.

```sh
mvn --batch-mode --no-transfer-progress clean verify
```

Install `target/mapartcopyright-1.3.0.jar`. The JAR bundles H2, SQLite, and MySQL JDBC drivers. Tests use MockBukkit, JUnit, real H2/SQLite databases, and a mocked Vault economy provider. GitHub Actions runs the tests and uploads the packaged JAR. Run the compatibility suite after the baseline build with `mvn -Ppaper26.2 -Dmaven.main.skip=true test`; this deliberately does not recompile production classes against 26.2.

## Multi-map artwork walls

Arrange existing filled maps in a vertical rectangle of item frames or glow item frames. Dimensions are **width × height**, viewed from the front:

| Layout | Tiles |
|---|---:|
| `2x1` | 2 |
| `2x2` | 4 |
| `2x3` | 6 |
| `3x3` | 9 |

Look directly at the **top-left frame** from within six blocks, then run:

```text
/mapart wall create 2x2 Sunset
/mapart wall info
```

Every position must contain one filled map facing the same way. Floor/ceiling layouts are not supported. This feature groups existing map art; it does not import images, generate pixels, or give out new maps. It preserves Minecraft map IDs, existing plugin UUIDs, and creator UUID attribution. Registered tiles must already belong to you, including when using administrative bypass. Repeated copies of the same registered tile cannot occupy multiple positions in one artwork.

Registration assigns a shared artwork UUID, tile coordinates, title, credit, copy lock, and frame protection. Its title defaults to the top-left tile's title when omitted; credit defaults to that tile's credit or your player name. The configured lock fee applies **per newly locked tile**, collected as one payment. Already locked tiles are not charged again. All registration rows are saved in one database transaction.

Hold any tile and use `/mapart name`, `/mapart credit`, `/mapart lock`, `/mapart unlock`, or `/mapart menu` to manage the **whole artwork and all registered copies**. Group lock/unlock fees apply per tile; an already-locked or already-unlocked artwork is free. Unlocking permits copying but retains ownership and separate frame protection. Use the menu's frame-protection toggle to disable that protection for the whole group.

Shared settings are authoritative in the database, so an older copy cannot bypass a newer lock. Loaded frames and open inventories refresh immediately; offline players, closed containers, and unloaded frames refresh when accessed. A single creator hologram is centered below the logical bottom-left tile when the wall is arranged correctly. Anvil renaming is disabled for artwork tiles; use the command or menu to rename all tiles together. Initial registration is permanent: there is no ungroup, resize, or merge command in this version. You can physically move the tiles without changing their registered layout or ownership.

## Ownership and protection

The first successful lock assigns a map UUID and saves its owner in the database. Later locks preserve that owner. Repeating a lock or unlock that is already in the requested state does not charge a fee.

**Unlocking permits copying; it does not transfer ownership.** Registered titles, credits, and protection settings remain editable only by the database owner or a player with `mapart.bypass`. The creator UUID is attribution, not a second source of ownership. Display credit can name a collaborator without granting that person control.

Locked maps reject unauthorized crafting, cartography, anvil renaming, and frame removal. Automatic crafters cannot process locked maps because there is no player to authorize. Owners can retrieve their maps from protected frames. Remove the map first before breaking its supporting block. An unlocked map can retain separate frame protection.

For **single maps**, protection follows the plugin metadata on each item stack; unlocking one copy does not remotely update other copies. For **registered artwork walls**, shared database settings apply to every tile and copy. The plugin does not prevent recreating an image independently or override trusted administrative plugins.

## Commands

All commands require `mapart.use` in addition to their individual permission.

| Command | Purpose | Permission |
|---|---|---|
| `/mapart lock` | Register and lock the held filled map | `mapart.lock` |
| `/mapart unlock` | Permit copying; retain ownership and metadata | `mapart.unlock` |
| `/mapart name <text>` | Change the canonical title | `mapart.rename` |
| `/mapart credit <text>` | Change the displayed creator credit | `mapart.credit` |
| `/mapart menu` | Open the management menu | `mapart.menu` |
| `/mapart wall create <size> [title]` | Register the wall starting at the targeted top-left frame | `mapart.wall`, `mapart.lock`, `mapart.rename`, `mapart.credit` |
| `/mapart wall info` | Inspect the held artwork tile or targeted frame | `mapart.wall` |
| `/mapart info` | Inspect map metadata | `mapart.info` |
| `/mapart verify [player]` | Check registered ownership and show attribution | `mapart.verify`; `mapart.verify.others` for another player |
| `/mapart audit <map-uuid> [page]` | Read retained audit entries, newest first | `mapart.audit` |
| `/mapart export` | Write `ownership_export.csv` in the plugin data directory | `mapart.export` |

The menu supports renaming, creator credit, locking, unlocking, and toggling title visibility, holograms, and frame protection. Toggles require `mapart.toggle.displayname`, `mapart.toggle.hologram`, or `mapart.toggle.itemframe`. Permissions and their defaults are listed in [plugin.yml](src/main/resources/plugin.yml).

`mapart.free` exempts a player from fees. `mapart.bypass` grants administrative ownership bypass. Both default to operators; grant them deliberately.

Titles support up to 32 visible UTF-16 code units and credits up to 16. Supported formatting includes `&aGreen`, `&#55aaffBlue`, `<#55aaff>Blue</#55aaff>`, decorations, and closed `<gradient:#ff0000:#0000ff>Text</gradient>` tags. GUI chat input accepts `cancel`, expires after the configured timeout, and is canceled if the item moves or changes. Hiding a title preserves its canonical value.

## Configuration and operations

See [config.yml](src/main/resources/config.yml). Database selection uses `database.type`: `h2` (default), `sqlite`, or `mysql`. H2 uses `mapart.mv.db`, SQLite uses `Mapart.db`. All backends preserve the existing `map_ownership` table and add `map_artworks` and `map_artwork_tiles`. Back up all three together. CSV ownership export remains an ownership list, not a complete artwork backup. MySQL needs a reachable server and a provisioned database.

Active settings include default hologram visibility, required visible titles, chat timeout, menu title/materials, configurable action messages, economy fees, GUI enablement, and lore updates. Permission checks always remain active.

Successful metadata changes and authorization decisions are written to UTF-8 audit logs. Entries include player UUIDs where available. An `*_allowed` event records authorization before vanilla processing; it does not claim that a completed item transfer was observed. Logs rotate at 5 MiB with three backups; audit pages contain ten entries, newest first, with at most 1,000 pages available per query.

If a database write fails after a successful payment, the plugin attempts a refund. Failed refunds are recorded in `refunds-pending.log` with an operation ID for administrator reconciliation. The game inventory, JDBC database, and external economy provider cannot form a single crash-atomic transaction.

Database operations are synchronous; slow remote MySQL connections can delay server ticks. If a database or artwork record is missing, protection fails closed: restore the complete database instead of deleting item tags. Tests do not replace staging checks on a live Paper server with your actual economy provider, permissions, protection plugins, and MySQL installation.

Read [the 1.3.0 upgrade and validation notes](docs/UPGRADE-1.3.0.md) before replacing an existing installation. The [1.2.1 integrity repair notes](docs/UPGRADE-1.2.1.md) remain relevant when upgrading from older builds.

Developed by GlassMC.

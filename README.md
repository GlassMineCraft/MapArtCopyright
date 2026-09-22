# MapArtCopyright

A Paper plugin that registers ownership of Minecraft map art, controls copying and editing, and displays titles and creator credits in inventories and item frames.

## Requirements and build

- Java 21.
- Paper 1.21.11, the API targeted by this build. Other server versions need separate validation.
- Vault. An economy provider is required when `economy.enabled` is true and an action has a nonzero fee.
- Maven 3.9+ to build from source. CMILib is optional; these features do not require it.

```sh
mvn --batch-mode --no-transfer-progress clean verify
```

Install `target/mapartcopyright-1.2.1.jar`. The JAR bundles H2, SQLite, and MySQL JDBC drivers. Tests use MockBukkit, JUnit, real H2/SQLite databases, and a mocked Vault economy provider. GitHub Actions runs the tests and uploads the packaged JAR.

## Ownership and protection

The first successful lock assigns a map UUID and saves its owner in the database. Later locks preserve that owner. Repeating a lock or unlock that is already in the requested state does not charge a fee.

**Unlocking permits copying; it does not transfer ownership.** Registered titles, credits, and protection settings remain editable only by the database owner or a player with `mapart.bypass`. The creator UUID is attribution, not a second source of ownership. Display credit can name a collaborator without granting that person control.

Locked maps reject unauthorized crafting, cartography, anvil renaming, and frame removal. Automatic crafters cannot process locked maps because there is no player to authorize. Owners can retrieve their maps from protected frames. Remove the map first before breaking its supporting block. An unlocked map can retain separate frame protection.

Protection follows the plugin metadata on each item stack. Unlocking one copy does not remotely update other copies already in the world, and the plugin does not prevent recreating an image independently or override trusted administrative plugins.

## Commands

All commands require `mapart.use` in addition to their individual permission.

| Command | Purpose | Permission |
|---|---|---|
| `/mapart lock` | Register and lock the held filled map | `mapart.lock` |
| `/mapart unlock` | Permit copying; retain ownership and metadata | `mapart.unlock` |
| `/mapart name <text>` | Change the canonical title | `mapart.rename` |
| `/mapart credit <text>` | Change the displayed creator credit | `mapart.credit` |
| `/mapart menu` | Open the management menu | `mapart.menu` |
| `/mapart info` | Inspect map metadata | `mapart.info` |
| `/mapart verify [player]` | Check registered ownership and show attribution | `mapart.verify`; `mapart.verify.others` for another player |
| `/mapart audit <map-uuid> [page]` | Read retained audit entries, newest first | `mapart.audit` |
| `/mapart export` | Write `ownership_export.csv` in the plugin data directory | `mapart.export` |

The menu supports renaming, creator credit, locking, unlocking, and toggling title visibility, holograms, and frame protection. Toggles require `mapart.toggle.displayname`, `mapart.toggle.hologram`, or `mapart.toggle.itemframe`. Permissions and their defaults are listed in [plugin.yml](src/main/resources/plugin.yml).

`mapart.free` exempts a player from fees. `mapart.bypass` grants administrative ownership bypass. Both default to operators; grant them deliberately.

Titles support up to 32 visible UTF-16 code units and credits up to 16. Supported formatting includes `&aGreen`, `&#55aaffBlue`, `<#55aaff>Blue</#55aaff>`, decorations, and closed `<gradient:#ff0000:#0000ff>Text</gradient>` tags. GUI chat input accepts `cancel`, expires after the configured timeout, and is canceled if the item moves or changes. Hiding a title preserves its canonical value.

## Configuration and operations

See [config.yml](src/main/resources/config.yml). Database selection uses `database.type`: `h2` (default), `sqlite`, or `mysql`. H2 uses `mapart.mv.db`, SQLite uses `Mapart.db`, and all backends use the existing `map_ownership` schema. MySQL needs a reachable server and a provisioned database.

Active settings include default hologram visibility, required visible titles, chat timeout, menu title/materials, configurable action messages, economy fees, GUI enablement, and lore updates. Permission checks always remain active.

Successful metadata changes and authorization decisions are written to UTF-8 audit logs. Entries include player UUIDs where available. An `*_allowed` event records authorization before vanilla processing; it does not claim that a completed item transfer was observed. Logs rotate at 5 MiB with three backups; audit pages contain ten entries, newest first, with at most 1,000 pages available per query.

If a database write fails after a successful payment, the plugin attempts a refund. Failed refunds are recorded in `refunds-pending.log` with an operation ID for administrator reconciliation. The game inventory, JDBC database, and external economy provider cannot form a single crash-atomic transaction.

Read [the 1.2.1 upgrade and validation notes](docs/UPGRADE-1.2.1.md) before replacing an existing installation.

Developed by GlassMC.

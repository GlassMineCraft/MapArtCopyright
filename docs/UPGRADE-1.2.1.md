# Upgrade to 1.2.1

## Installation

1. Stop the server. Back up the world, the complete MapArtCopyright data directory, and the external ownership database if using MySQL.
2. Replace the old plugin JAR with `mapartcopyright-1.2.1.jar`, retaining the existing data directory and configuration.
3. Use Java 21 and Paper 1.21.11, with Vault and your configured economy provider.
4. Start the server and verify that the ownership database connects. Complete the staging checks below before making the update available to players.

Database filenames, ownership table columns, and item metadata keys remain compatible with 1.2.0. H2 is opened as a single embedded connection; concurrent external H2 file access through AUTO_SERVER is no longer enabled. Do not open its database file from a second process while the plugin runs.

No ownership is inferred from an item when its database record is missing. Restore the matching database backup. Missing records and database failures leave registered-map edits blocked. The plugin stays loaded so its protection handlers can continue denying unauthorized operations.

## Intentional behavior changes

- Unlocking permits copies but preserves the registered owner's exclusive control of metadata and protection settings. There is no ownership transfer command.
- Creator UUID and displayed credit do not grant ownership.
- Automatic crafters reject locked maps, including when the output would lose its tags.
- Legacy fixed frames are made removable by their owner when loaded. Protection is enforced through events. Remove a protected map before breaking the supporting block.
- Existing holograms are reconciled on startup and chunk load. Each new display belongs to a specific frame and is recreated after chunk loading.
- GUI input expires after 60 seconds by default and never reinserts a saved copy of an item.
- Hidden names retain their canonical title. Renames and credit changes update the database. Dropping a map preserves its title and lore.
- Failed withdrawals no longer report success. Failed persistence after payment triggers a refund attempt and records unsuccessful refunds for administrators.
- `/mapart verify` distinguishes database ownership from creator attribution.
- Audit output is newest first, matches a complete map UUID, and records mutations and authorization decisions separately.

The deprecated `mysql.enabled`, `settings.default-lock-enabled`, and `features.enable-permissions` keys can be removed from existing configuration files. They are ignored: `database.type` selects the backend, the lock command always requests a lock, and authorization is always checked. Existing configuration files do not need to be regenerated. Add `settings.chat-timeout-seconds` if you want a value other than 60 (valid range 5–300).

## Audit finding coverage

| Audited problem | Repair |
|---|---|
| Chat duplicates maps or replaces the current hand | Compare a snapshot with the actual held slot; mutate only the validated real stack |
| Locking overwrites ownership or charges repeatedly | Insert-only registration, owner-preserving updates, no-op checks before payment |
| Unlock reapplies stale locked metadata | One coordinated metadata update removes the lock flag |
| Async chat writes inventories and races shared maps | Atomic session claim; inventory, authorization, and edits run on the server thread |
| Database errors are swallowed | Propagate failed writes; leave items unchanged and compensate successful payments |
| Automatic crafting bypass | Handle CrafterCraftEvent and inspect both input and result |
| Projectile and support destruction bypass | Resolve projectile shooters; guard damage, detachment, and supporting blocks |
| GUI spoofing, bottom-slot actions, and dragging | Inventory holder identity, raw top slots, deferred actions, drag cancellation |
| Hidden names become Untitled or diverge from stored metadata | Canonical title independent of visibility; persist name and credit changes |
| Dropping maps strips title and lore | Preserve and render canonical metadata |
| One frame deletes another hologram | Store frame UUID on each display and remove only matching displays |
| Vault withdrawal result ignored | Require a successful EconomyResponse |
| Blank anvil names bypass authorization | Validate empty renames in preview and result handlers |
| Fixed frame blocks its owner | Replace fixed-state protection with ownership-aware event handling |
| Menu permission unchecked | Gate opening, interactions, and completed prompts |
| Audit hides newest events or reports misleading success | Newest-first pagination, mutation logs, distinct allowed/denied event names, canceled-event handling |
| Config keys have no effect | Wire supported settings and remove misleading retired switches |

The build also packages explicit SQLite/MySQL drivers, corrects CSV escaping, limits audit retention, and adds CI.

## Automated validation

Run `mvn --batch-mode --no-transfer-progress clean verify`.

The regression suite covers payment failure, refunds, failed refund records, ownership takeover, unlock metadata, disconnected and missing databases, H2/SQLite reconnection, stale/canceled/asynchronous chat, canonical title rendering, drops, GUI routing and permissions, configuration, automatic crafters, cartography audit semantics, blank/canceled anvil renames, projectile and support protection, legacy fixed frames, adjacent hologram isolation, audit pagination/rotation reads, and CSV quoting.

Packaging integration tests also load the final JAR in an isolated classloader: H2 and SQLite perform database round trips, and the MySQL driver and relocated JDBC service entries are checked.

These tests use MockBukkit plus real embedded databases. They do not substitute for a running Minecraft server, real Vault provider, MySQL server, or other installed plugins.

## Staging checks before release

- Test with an owner, an unrelated player, and an administrator in survival mode. Verify lock/unlock balances and ownership after restarting the server.
- Start a GUI rename, move/drop/swap the map, and send the reply. Confirm item counts and the unrelated hand item remain unchanged.
- Exercise crafting-table copies, cartography copies/scaling/freezing, shift-clicks, number keys, and automatic crafters.
- Test blank and formatted anvil names, insufficient experience, a full inventory, shift-click results, and cancellation by other plugins. Confirm resulting item metadata and the export agree.
- Attack protected frames directly and with projectiles. Test support breaks, explosions, pistons, owner retrieval, and ordinary unprotected frames.
- Put two credited frames next to each other. Remove one map, unload/reload the chunk, and restart the server. Check for missing, duplicated, or orphaned holograms.
- Test the configured MySQL instance and actual economy provider, including disconnection and declined withdrawals. Review any `refunds-pending.log`.
- Restore a copy of the previous database and world and confirm existing owners are recognized.

JDBC ownership checks still run synchronously. MySQL connection and socket timeouts are bounded at ten seconds, but a slow remote database can stall the server thread. Crash recovery across inventory, economy, and database state, and interoperability with other plugins, remain staging concerns.

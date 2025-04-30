# 🖼️ MapArtCopyright
A Paper plugin for Minecraft 1.21+ that protects and credits original map art creations. Add locking, creator attribution, GUI management, and UUID tracking to ensure your in-game art stays yours.

---

## ✨ Features

- 🔐 **Lock/Unlock Maps** — Prevents duplication or tampering of map art
- 🧠 **Persistent Metadata** — Stores creator name, custom display name, and UUID
- 🧾 **UUID Assignment** — Assigns a unique identifier to each locked map
- 🎨 **Inventory Display** — Shows creator name and title when hovering over maps
- 🖼️ **Hologram Tag** — Displays the creator name below item frames (toggleable)
- 📦 **GUI Menu** — 9x5 menu to manage name, credit, lock/unlock, and toggles
- 💬 **Chat Input** — Rename or credit a map via typed input after clicking GUI icons
- 🔍 **Admin Info Command** — View a map’s UUID, creator, and lock status
- 🔑 **Permission-Based Access** — Fully configurable with permissions per feature

---

## 🔧 Commands

| Command                      | Description                               | Permission         |
|-----------------------------|-------------------------------------------|--------------------|
| `/mapart lock`              | Locks the map in your hand                | `mapart.lock`      |
| `/mapart unlock`            | Unlocks the map (retains metadata)        | `mapart.unlock`    |
| `/mapart credit <name>`     | Assigns a creator credit to the map       | `mapart.credit`    |
| `/mapart name <name>`       | Renames the map display name              | `mapart.rename`    |
| `/mapart menu`              | Opens the management GUI                  | `mapart.menu`      |
| `/mapart info`              | View UUID and metadata of held map        | `mapart.info` (OP) |

---

## 🖥️ GUI Controls

Open with `/mapart menu`  
Features:
- 🧠 Rename Map (Anvil icon)
- ✍️ Set Creator (Book & Quill)
- 👤 Auto-Credit (Player Head)
- 🔒 Lock (Item Frame)
- 🗺️ Unlock (Filled Map)
- 💡 Toggle Map Name (Sea Lantern)
- 💡 Toggle Hologram (Redstone Torch)
- ❌ Close Menu (Barrier)

---

## 🔐 Permissions

Defined in `plugin.yml`. Use LuckPerms or another permissions manager to assign:
```yaml
mapart.use
mapart.lock
mapart.unlock
mapart.credit
mapart.rename
mapart.menu
mapart.toggle.displayname
mapart.toggle.hologram
mapart.info

🧠 Roadmap Ideas
/mapart claim integration

Export/import system

Duplicate protection for UUID reuse

MapArt whitelist or shareable ownership

👤 Credits
Developed by GlassMC
Map art deserves proper credit 🖼️💡
# MapArtCopyright

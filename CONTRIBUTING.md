# 🛠️ Item Finder — Developer Guide

This document is for developers who want to fork, contribute, or build on top of Item Finder.

---

## 📋 Table of Contents

- [Project Overview](#-project-overview)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Project Structure](#-project-structure)
- [Architecture](#-architecture)
- [Adding a New Config Option](#-adding-a-new-config-option)
- [Adding a New Language](#-adding-a-new-language)
- [Adding a New Container Type](#-adding-a-new-container-type)
- [Networking (Multiplayer)](#-networking-multiplayer)
- [Cache System](#-cache-system)
- [Contributing](#-contributing)
- [Pull Request Guidelines](#-pull-request-guidelines)
- [Code Style](#-code-style)
- [Known Technical Debt](#-known-technical-debt)

---

## 🧭 Project Overview

Item Finder is a Fabric mod for Minecraft 1.21.5 that allows players to search for items across nearby storage containers. It supports both singleplayer and multiplayer (with or without server-side installation).

**Key design decisions:**
- Client-side GUI with server-side scan logic (via Fabric Networking packets)
- Automatic fallback to local cache when server does not have the mod installed
- Config stored as JSON on disk, loaded once at startup
- All rendering done via vanilla Minecraft GUI APIs — no external rendering libraries

---

## 🧰 Tech Stack

| Tool | Version |
|---|---|
| Minecraft | 1.21.5 |
| Java | 21 |
| Fabric Loader | 0.18.4+ |
| Fabric API | 0.128.2+1.21.5 |
| Gradle | 9.x (via wrapper) |
| Mappings | Mojang Official |
| Optional | ModMenu 13.0.0 |

---

## 🚀 Getting Started

### Prerequisites
- Java 21 (JDK, not JRE) — recommended: [Adoptium Temurin 21](https://adoptium.net)
- Git
- Any IDE — IntelliJ IDEA recommended, VSCode with Extension Pack for Java also works

### Clone and Setup

```bash
git clone https://github.com/fadhlanputra/itemfinder.git
cd itemfinder
./gradlew genSources   # generate Minecraft sources for navigation/autocomplete
./gradlew runClient    # launch Minecraft with the mod loaded
```

First run will take several minutes to download dependencies.

### Build

```bash
./gradlew build
```

Output `.jar` will be in `build/libs/`. Use the one **without** `-sources` or `-dev` suffix.

### Optional: ModMenu support

To enable ModMenu integration during development, download [ModMenu](https://modrinth.com/mod/modmenu) for 1.21.5 and place it in `run/mods/`.

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/lann/itemfinder/
│   │   ├── ItemFinderMod.java          # Mod entry point (server + client common)
│   │   ├── ConfigManager.java          # Config load/save (JSON via Gson)
│   │   ├── StorageScanner.java         # Core scan logic (runs on server thread)
│   │   ├── SearchPacket.java           # C2S packet: client sends search request
│   │   ├── SearchResultPacket.java     # S2C packet: server sends results back
│   │   └── ServerNetworkHandler.java   # Server-side packet receiver
│   └── resources/
│       ├── fabric.mod.json             # Mod metadata, entrypoints, dependencies
│       ├── itemfinder.mixins.json      # Server-side mixin config
│       └── assets/itemfinder/lang/    # Translation files (28 languages)
│
└── client/
    ├── java/com/lann/itemfinder/
    │   ├── ItemFinderClient.java        # Client entry point
    │   ├── KeyBindings.java             # Keybind registration
    │   ├── SearchScreen.java            # Main search GUI
    │   ├── ModConfigScreen.java         # ModMenu config screen
    │   ├── ModMenuIntegration.java      # ModMenu API hook
    │   ├── ClientNetworkHandler.java    # Client-side packet receiver + sender
    │   ├── ServerDetector.java          # Detects if server has mod installed
    │   ├── CacheManager.java            # Local container cache (per-server, persistent)
    │   ├── HighlightRenderer.java       # World-space container outline rendering
    │   ├── HudOverlay.java              # HUD panel (top-right, shows results)
    │   └── ParticleTrail.java           # Particle effect toward containers
    └── resources/
        └── itemfinder.client.mixins.json
            mixin/client/
                ├── ExampleClientMixin.java
                ├── ContainerOpenMixin.java   # Intercepts screen open for auto-clear
                └── ContainerCacheMixin.java  # Intercepts container open for caching
```

---

## 🏗️ Architecture

### Request Flow (Multiplayer with server mod)

```
Player presses Y
    → SearchScreen opens
    → Player types query, clicks item (or presses Enter)
    → ClientNetworkHandler.sendSearchRequest(itemId)
    → SearchPacket sent to server (C2S)
    → ServerNetworkHandler receives packet
    → StorageScanner.scanExact() or scanContains() runs on server thread
    → SearchResultPacket sent back to client (S2C)
    → ClientNetworkHandler.onResultReceived callback fires
    → SearchScreen updates results
    → HighlightRenderer.setHighlights() + ParticleTrail.setTargets()
```

### Request Flow (Cache fallback — no server mod)

```
Player presses Y
    → SearchScreen opens
    → ServerDetector.serverHasMod() returns false
    → CacheManager.searchExact() or searchContains() runs locally
    → Results displayed with ⚠ cache warning
```

### Scan prefix convention

`SearchPacket.itemId` uses a prefix to distinguish search modes:
- No prefix → exact match by item ID (e.g. `stone`)
- `*` prefix → contains match (e.g. `*stone` scans all items whose ID contains "stone")

This is handled in `ServerNetworkHandler` and mirrored in `CacheManager`.

---

## ➕ Adding a New Config Option

This is a common task. Follow these steps:

**1. Add field to `ConfigManager.Config`:**
```java
public boolean myNewOption = true; // always provide a default
```

**2. Add button in `SearchScreen.init()` config section:**
```java
// ROW N: My new option
this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
    btn -> { ConfigManager.get().myNewOption = !ConfigManager.get().myNewOption; })
    .bounds(btnX, cfgRowY(N), 50, btnH).build());
```

**3. Add label in `SearchScreen.renderConfig()`:**
```java
g.drawString(this.font,
    Component.translatable("gui.itemfinder.config_my_option").getString() + " §e" + (cfg.myNewOption ? "ON" : "OFF"),
    labelX, cfgRowY(N) + off, 0xFFFFFF);
```

**4. Increment `CFG_ROW_COUNT` in `SearchScreen`:**
```java
private static final int CFG_ROW_COUNT = N + 1; // was N
```

**5. Repeat steps 2-4 for `ModConfigScreen` (same pattern, uses `rowY()` instead of `cfgRowY()`):**
- Add widget in `init()`
- Add label in `render()`
- Increment `ROW_COUNT`

**6. Add translation key to all lang files** in `src/main/resources/assets/itemfinder/lang/`:
```json
"gui.itemfinder.config_my_option": "My option:"
```

---

## 🌐 Adding a New Language

1. Create a new file in `src/main/resources/assets/itemfinder/lang/` named with the correct Minecraft locale code (e.g. `sv_se.json` for Swedish)
2. Copy all keys from `en_us.json` and translate the values
3. No recompile needed — lang files are loaded at runtime

Minecraft locale codes: https://minecraft.wiki/w/Language

**Full list of required keys** (copy from `en_us.json`):
```
key.itemfinder.open_search
category.itemfinder
gui.itemfinder.title
gui.itemfinder.hint
gui.itemfinder.back
gui.itemfinder.config
gui.itemfinder.toggle
gui.itemfinder.change
gui.itemfinder.search_min
gui.itemfinder.no_match
gui.itemfinder.click_to_scan
gui.itemfinder.scan_all
gui.itemfinder.config_title
gui.itemfinder.config_radius
gui.itemfinder.config_highlight_duration
gui.itemfinder.config_highlight_pulse
gui.itemfinder.config_sort
gui.itemfinder.config_chat
gui.itemfinder.config_particle
gui.itemfinder.config_particle_type
gui.itemfinder.config_search_lang
gui.itemfinder.config_enter_mode
gui.itemfinder.config_save_hint
gui.itemfinder.sort_distance
gui.itemfinder.sort_count
gui.itemfinder.search_lang_english
gui.itemfinder.search_lang_game
gui.itemfinder.enter_off
gui.itemfinder.enter_top
gui.itemfinder.enter_all
gui.itemfinder.only_singleplayer
gui.itemfinder.searching
gui.itemfinder.found
gui.itemfinder.container
gui.itemfinder.not_found
gui.itemfinder.in_radius
gui.itemfinder.block
gui.itemfinder.cache_warning
gui.itemfinder.mode_server
gui.itemfinder.mode_cache
gui.itemfinder.cache_label
gui.itemfinder.not_found_cache
gui.itemfinder.cache_containers
```

---

## 📦 Adding a New Container Type

Container types fall into two categories:

### Block Entity containers (chest, barrel, furnace, etc.)
These are handled automatically in `StorageScanner` because they implement `Container`. No code change needed — they are detected via `blockEntity instanceof Container`.

Special cases like `EnderChestBlockEntity` need explicit handling because they don't directly expose inventory. See the existing `EnderChestBlockEntity` block in `scanExact()` for the pattern.

### Entity containers (minecart, boat)
Add a new `else if` branch in both `scanExact()` and `scanContains()` in `StorageScanner.java`:

```java
else if (entity instanceof MyNewEntityType e) {
    container = e;
    type = "MyNewEntityType";
}
```

Then add a `friendlyName()` mapping in `SearchResult`:
```java
if (className.equals("MyNewEntityType")) return "My New Container";
```

---

## 🌐 Networking (Multiplayer)

### Packet registration
Both packets must be registered in `ItemFinderMod.onInitialize()` (common side):
```java
SearchPacket.register();       // C2S — registers in PayloadTypeRegistry.playC2S()
SearchResultPacket.register(); // S2C — registers in PayloadTypeRegistry.playS2C()
```

### Adding a new packet
1. Create a class implementing `CustomPacketPayload`
2. Define `TYPE` (ResourceLocation) and `CODEC` (StreamCodec)
3. Register in `ItemFinderMod.onInitialize()`
4. Register receiver in `ServerNetworkHandler` (for C2S) or `ClientNetworkHandler` (for S2C)

### Server detection
`ServerDetector` detects mod presence by checking `ClientPlayNetworking.canSend(SearchPacket.TYPE)` after joining. This works because the server registers the channel only if the mod is installed. Detection result is cached per session in `ServerDetector.serverHasMod()`.

---

## 💾 Cache System

Cache files are stored in `.minecraft/itemfinder_cache/` as JSON files, one per server address (sanitized as filename).

**File naming:** server IP/address is sanitized with `[^a-zA-Z0-9._-]` → `_`

**Data structure per file:**
```json
{
  "serverAddress": "play.example.com",
  "dimensions": {
    "minecraft:overworld": {
      "-32,29,-25": {
        "type": "ChestBlockEntity",
        "lastOpened": 1234567890000,
        "items": {
          "stone": 64,
          "oak_log": 12
        }
      }
    }
  }
}
```

**Cache lifecycle:**
- `CacheManager.onJoinServer()` → load from disk
- `CacheManager.cacheContainer()` → update entry + auto-save to disk
- `CacheManager.onLeaveServer()` → final save + clear memory
- `CacheManager.onChangeDimension()` → update active dimension key

**Cache is written by** `ContainerCacheMixin` which intercepts `Minecraft.setScreen()` and reads slot data from the opened `AbstractContainerScreen`.

---

## 🤝 Contributing

Contributions are welcome! Here are the best ways to contribute:

- 🐛 **Bug fixes** — always welcome, please include steps to reproduce
- 🌐 **New translations** — just add a lang file, no Java knowledge needed
- ✨ **New features** — please open an issue first to discuss before implementing
- 📖 **Documentation** — improving this guide or the player-facing README

### Before submitting a PR

- [ ] Test in singleplayer
- [ ] Test with server (if networking changes)
- [ ] No compilation warnings (run `./gradlew build`)
- [ ] New config options follow the pattern in [Adding a New Config Option](#-adding-a-new-config-option)
- [ ] New translation keys added to `en_us.json` at minimum

---

## 📐 Pull Request Guidelines

- **One feature per PR** — keep PRs focused and reviewable
- **Branch naming:** `feature/my-feature`, `fix/bug-description`, `lang/sv_se`
- **Commit messages:** short and descriptive in English (`Add barrel entity support`, `Fix cache not clearing on disconnect`)
- **Do not** reformat unrelated code in the same PR
- **Do not** change `ROW_COUNT` / `CFG_ROW_COUNT` without adding the corresponding widget and label

### PR Template

```
## What does this PR do?
Brief description.

## Type of change
- [ ] Bug fix
- [ ] New feature
- [ ] Translation
- [ ] Refactor
- [ ] Documentation

## Tested on
- [ ] Singleplayer
- [ ] Multiplayer (server + client)
- [ ] Multiplayer (cache fallback, no server mod)
```

---

## 🎨 Code Style

- **Indentation:** 4 spaces (no tabs)
- **Braces:** same line (`{` not on new line)
- **Naming:** camelCase for variables/methods, PascalCase for classes, UPPER_SNAKE for constants
- **Translation:** never hardcode user-facing strings — always use `Component.translatable()`
- **Logging:** use `ItemFinderMod.LOGGER` with `[ItemFinder]` prefix for any log output
- **Threading:** scan logic always runs on server thread via `server.execute()`, UI updates always run on client thread via `client.execute()`

---

## ⚠️ Known Technical Debt

| Issue | Location | Notes |
|---|---|---|
| `ContainerCacheMixin` uses positional heuristic to find opened container | `ContainerCacheMixin.java` | Works in most cases but may cache wrong container in edge cases with many adjacent containers |
| `updateButtonVisibility()` relies on widget list order | `SearchScreen.java` | Fragile — if widget order changes, wrong buttons may be hidden/shown |
| Lang files for non-English languages not fully translated | `lang/*.json` | Most files are copies of `en_us.json` — community translations welcome |
| No unit tests | — | All testing is manual in-game |
| Cache has no expiry or size limit | `CacheManager.java` | Could grow large on servers with many containers over time |

---

## 📜 License

MIT License. You are free to fork, modify, and redistribute this mod, including in modpacks. Attribution is appreciated but not required.

If you publish a fork, please make it clear it is a fork and link back to the original.
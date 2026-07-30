# ☀ Solstice

**An all-in-one optimization + quality-of-life client for Fabric Minecraft 1.21.11**

Solstice replaces dozens of separate mods with one cohesive, modular client: performance
optimizations, quality-of-life features, a HUD editor, texture pack management, and a
profile system for switching between saved setups instantly - all sharing the same
config, rendering, and module framework.

---

## Features

### Performance (Advanced tab)

Always-on tuning plus toggleable optimizations: render-distance/minimized-window
handling, particle rate limiting, GC-hint memory management, network socket tuning,
entity culling, startup speedups, and view-distance handling. Three built-in performance
profiles - **Lite**, **Balanced**, **Aggressive** - apply a whole tuned combination at
once, plus unlimited saved Custom profiles.

### Quality of Life

- **Shulker Box Tooltip** - hold a key combo to preview a container's contents as a real
  icon grid instead of vanilla's plain-text list, with a compact (merged, deduplicated)
  and full (every slot) mode, and a lockable frozen preview.
- **Chat Heads** - player skin heads next to chat messages.
- **Locator Heads** - nearby players shown as head icons around the XP bar.
- **Armor HUD** - real worn armor pieces rendered next to the hotbar.
- **Inventory HUD** - your inventory shown as a small movable icon grid on screen.
- **Combat Hitbox** - an outline around nearby entities, highlighting whichever one is
  in your crosshair. Purely visual - never changes actual hitboxes, reach, or combat.
- **AppleSkin-style hunger overlay**, **dynamic crosshair**, **zoom**, **fullbright**,
  **status effect timer**, **no blindness/nausea**, **view model customization**, and a
  bundle of visual tweaks (no fog, low fire, small totem pop, pull indicators, and more).

### HUD Manager

A free-drag HUD editor (FPS/RAM widgets, a real repositioned Boss Bar, a real
repositioned Scoreboard sidebar, a free-text Watermark, Inventory HUD) with per-element
visibility toggles and one master switch for the whole overlay layer.

### Textures tab

- **Presets** - whole real resource packs, one click to activate, including packs you add
  yourself via a native file/folder picker.
- **Advanced** - independent per-category swaps (Tools, Utilities, Armor, GUI, Fonts),
  mixable with whichever Preset is active.
- **Custom combos** - save a Preset + Advanced-row combination as a named, reapplicable
  preset of your own, the same way Custom performance/visual profiles work.

See [NOTICE.md](NOTICE.md) for exactly which bundled/adapted third-party content backs
which feature, and its license.

---

## Requirements

| Dependency | Version |
|---|---|
| Java | 21+ |
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.3+ |
| Fabric API | 0.141.4+1.21.11 |
| Fabric Loom | 1.16.x |

Minecraft version is pinned exactly, not a range - a build compiled against one patch
version isn't guaranteed compatible with another.

---

## Build

```bash
git clone https://github.com/soul-lyte/Solstice.git
cd Solstice
gradle build
```

The compiled JAR lands in `build/libs/`. Drop it into your `.minecraft/mods/` folder
alongside Fabric API.

---

## Configuration

Config is written automatically on first run and edited live from Solstice's own settings
screen - press **Right Shift** (rebindable) in-game to open it. There's no need to
hand-edit the config file; every module, HUD element, texture selection, and profile is
adjustable from that screen.

---

## Multiplayer Fairness

Solstice is a **pure optimization + QOL client**. It never modifies:

- Combat mechanics, reach, hitboxes, or aim
- Movement, velocity, or knockback
- Entity or ore visibility (no ESP/X-Ray)
- Packet content or timing
- Automation of any kind

All changes are graphical, informational, or transport-layer only, and are safe on any
server.

---

## License

MIT - see [LICENSE](LICENSE). Several features are ported/adapted from other open-source
mods under their own licenses (LGPL-3.0, MPL-2.0, Apache-2.0, and a couple of unresolved
attribution-only cases) - see [NOTICE.md](NOTICE.md) for the full breakdown.

# ☀ Solstice

**Stop building the same 30-mod pack every time you make a new instance.**

Solstice is one mod that replaces the pile of separate ones you'd normally hunt down and
reinstall on every fresh instance - performance tuning, PvP/SMP quality-of-life features,
a HUD editor, texture pack management, and a profile system for switching between saved
setups instantly, all sharing one config, one settings screen, and one module framework.

It's not trying to be the single most extreme optimization mod out there - if squeezing
out the absolute last frame is your whole goal, dedicated performance mods will still
edge it out. What Solstice actually does is give a genuinely solid performance boost
*and* the full set of PvP/SMP/solo quality-of-life features you'd normally cobble
together from a dozen different mods, all in one jar, with sane defaults, so you can drop
it into a new instance and just start playing instead of spending an hour on Modrinth
first.

**Who this is for:** people who spin up new instances often and are tired of rebuilding
the same modpack every time, and people who aren't sure which mods they even need for a
new playthrough. If you already run a hand-tuned stack of a dozen specialized mods
picked for your exact setup, Solstice won't beat that - it's built for everyone else.

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

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.3+ for Minecraft **1.21.11
   exactly** - the version is pinned, not a range, so other 1.21.x releases aren't
   guaranteed compatible.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) 0.141.4+1.21.11 (or newer)
   and Solstice, both for 1.21.11.
3. Drop both `.jar` files into your instance's `mods` folder.
4. Launch the game, then press **Right Shift** in-game to open Solstice's settings and
   turn on whatever you actually want - most features are off by default so nothing
   changes until you ask for it.

That's it - no separate config file to edit, no other dependencies to track down. Java
21+ is required (bundled with recent Minecraft launchers, so most people already have it).

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

## Building from source

Most users just want the [Installation](#installation) steps above - this is only for
building the jar yourself.

```bash
git clone https://github.com/soul-lyte/Solstice.git
cd Solstice
gradle build
```

The compiled JAR lands in `build/libs/`.

| Dependency | Version |
|---|---|
| Java | 21+ |
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.3+ |
| Fabric API | 0.141.4+1.21.11 |
| Fabric Loom | 1.16.x |

---

## License

MIT - see [LICENSE](LICENSE). Several features are ported/adapted from other open-source
mods under their own licenses (LGPL-3.0, MPL-2.0, Apache-2.0, and a couple of unresolved
attribution-only cases) - see [NOTICE.md](NOTICE.md) for the full breakdown.

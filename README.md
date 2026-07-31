# ☀ Solstice

**One mod instead of the 30-mod pack you rebuild on every new instance.** Solid
performance tuning plus every PvP/SMP/solo quality-of-life feature you actually want, in
one jar with sane defaults. Not the single most hardcore optimization mod out there -
just the one you don't have to think about.

Requires **[Fabric API](https://modrinth.com/mod/fabric-api)**, nothing else.

---

## Features

- **Performance** - always-on tuning (render-distance/minimized-window handling,
  particle rate limiting, GC-hint memory management, network socket tuning, entity
  culling, startup speedups, view-distance handling) plus three built-in profiles -
  **Lite**, **Balanced**, **Aggressive** - and unlimited saved Custom ones.
- **Quality of Life** - Shulker Box Tooltip (icon-grid container preview), Chat Heads,
  Locator Heads, Armor HUD, Inventory HUD, Combat Hitbox (crosshair-target outline,
  purely visual), AppleSkin-style hunger overlay, dynamic crosshair, zoom, fullbright,
  status effect timer, no blindness/nausea, view model customization, and a bundle of
  visual tweaks (no fog, low fire, small totem pop, pull indicators, and more).
- **HUD Manager** - free-drag editor for the FPS/RAM widgets, a real repositioned Boss
  Bar and Scoreboard, a free-text Watermark, and Inventory HUD, with per-element
  visibility toggles and one master switch for the whole overlay layer.
- **Textures** - whole-pack Presets (one click, including packs you add yourself),
  independent per-category Advanced swaps (Tools, Utilities, Armor, GUI, Fonts), and
  savable Custom Preset + Advanced combos.

See [NOTICE.md](NOTICE.md) for exactly which bundled/adapted third-party content backs
which feature, and its license.

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

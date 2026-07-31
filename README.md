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

Requires **[Fabric API](https://modrinth.com/mod/fabric-api)** - that's the only other
mod you need alongside it.

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

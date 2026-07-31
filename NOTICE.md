# Third-Party Licensing Notice

Solstice is MIT-licensed (see `LICENSE`), except for specific files adapted
from other open-source Minecraft mods, which retain their original license.
Each affected file carries a header comment identifying its source, original
copyright holder, and license - this file is the index.

## LGPL-3.0-only

Adapted directly from source code, not just inspired by:

- **Lithium** (CaffeineMC) - https://github.com/CaffeineMC/lithium - LGPL-3.0-only
- **ImmediatelyFast** (RaphiMC) - https://github.com/RaphiMC/ImmediatelyFast - LGPL-3.0-only
- **Locator Heads** (Haage001) - https://github.com/Haage001/locator-heads - LGPL-3.0-only
- **Bobby** (Johni0702) - https://github.com/Johni0702/bobby - LGPL-3.0-only.
  The entire "Chunk Retention" feature (`com.example.solstice.viewdistance`,
  `com.example.solstice.mixin.viewdistance`) is a port of Bobby's real
  architecture (tag `v5.2.11.1+mc1.21.11`) onto this project's own naming
  conventions: `FakeChunk`, `FakeChunkManager`, `FakeChunkStorage`,
  `ChunkSerializer`, `VisibleChunksTracker`, `FileSystemUtils`, and the
  `LightingProviderMixin`/`ChunkLightProviderMixin` shadow light-data system.
  Scoped down from upstream Bobby: no dynamic multi-world merge/fingerprinting
  (`Worlds`), no `/bobby` commands, no `LastAccessFile`-based automatic
  region cleanup, and no Sodium/Starlight compatibility shims.

Full license text: `licenses/LICENSE-LGPL-3.0.txt`.

Per LGPL-3.0 Section 3/4, files under this license (and this distributed jar
as a combined work incorporating them) are provided such that the LGPL-covered
portions remain identifiable and replaceable - see the per-file headers for
exactly which files this applies to. This does not relicense the rest of
Solstice; only the specifically-marked files are LGPL-3.0-only.

## MPL-2.0 (file-level copyleft, distinct from LGPL-3.0's whole-work scope)

Adapted directly from source code:

- **Chat Heads** (dzwdz) - https://github.com/dzwdz/chat_heads - MPL-2.0

Full license text: `licenses/LICENSE-MPL-2.0.txt`. MPL-2.0's copyleft is
per-file, not per-combined-work like LGPL-3.0 - only the files whose content
actually originated from chat_heads need to stay MPL-2.0-licensed and have
their source available. See the per-file headers in `mixin/chat/` for exactly
which files this applies to.

## MIT (compatible with Solstice's own license, still attributed)

- **C2ME** (RelativityMC) - https://github.com/RelativityMC/C2ME-fabric - MIT,
  **except** the `c2me-opts-accel-opencl/` directory in the upstream repo,
  which is All Rights Reserved and was never used here.
- **BadOptimizations** (imthosea) - https://github.com/imthosea/BadOptimizations - MIT
- **quick-pack** (DrexHD) - https://github.com/DrexHD/quick-pack - MIT. Only its
  splash-screen fade-out skip was ported.
- **ShulkerBoxTooltip** (MisterPeModder) - https://github.com/MisterPeModder/ShulkerBoxTooltip - MIT.
  Its hold-a-key preview trigger, Compact/Full preview modes, and dye-colored
  background panel were ported, including its bundled 9-slice sprite
  (`assets/solstice/textures/gui/sprites/shulker_box_tooltip.png`).
- **armor-hud** (uku3lig) - https://github.com/uku3lig/armor-hud - MIT. The
  bundled `warn.png` low-durability warning icon is copied directly from
  this repo (`assets/solstice/textures/gui/armor_hud_warning.png`).
- **VMP** (RelativityMC) - https://github.com/RelativityMC/VMP-fabric - MIT.
  Only its `no_flush` networking optimization was ported (skipping a
  redundant per-tick socket flush and an unnecessary event-loop wakeup on
  non-flushing sends), merged into the existing `NetworkModule`. VMP's own
  primary focus - server-side chunk/entity/player-tracking scaling for
  high player counts - doesn't apply to a singleplayer-facing client mod
  and was not ported.

## Apache-2.0 (compatible with Solstice's own license, still attributed)

- **combat-hitboxes** (sootysplash) - https://github.com/sootysplash/combat-hitboxes - Apache-2.0.
  Its entity-outline concept and "turn red on your crosshair target" behavior
  were ported into `CombatHitboxRenderer.java`. Width/color settings and the
  preview screen are Solstice's own addition.

## Non-commercial-only (used, with restriction)

- **EntityCulling** (tr7zw) - https://github.com/tr7zw/EntityCulling - "tr7zw
  Protective License": permits use/modify/compile, but explicitly **forbids
  commercial use or monetary compensation**. If Solstice is ever monetized
  (sales, paywalled builds, etc.), any code adapted from this source must be
  removed or replaced first.

## Removed: previously-bundled resource packs with no redistribution license

Five texture packs (Tiny Tools by jahirtrap, Vanilla+ by Marlowww, Tournament
[16x] - credited per its own `credits.txt` to Saki 16x/Keno, Cuboids/Cryokine,
Fabled/Belmu, Alius, Skeletony, Kemiu, ovaszos_uborka, WeNAN Studios, and
Clyred for different pieces - SMP Essentials by MrOrdenador, and Mini Sword)
used to be bundled directly inside the jar as Presets and Advanced-row options. None of
them ever came with a license file, so redistributing them was never actually
cleared. Removed entirely on this branch rather than left flagged: Solstice no
longer bundles or redistributes any third-party texture pack. If you have one
of these packs installed yourself, its content now shows up automatically in
the Textures tab's Advanced row via runtime detection (`DynamicTextureRegistry`)
instead - nothing is copied into Solstice's own jar, only read locally from
files you already obtained.

## Closed-source, not copied - built from the public feature description only

- **InventoryHUD+** (DmitryLovin) - https://modrinth.com/mod/inventoryhudplus -
  All-Rights-Reserved, source not public. `InventoryHudElement.java` was
  built independently from its public feature description; no code from
  this mod is used anywhere in Solstice, and its separate potion/armor
  HUD features were not replicated.

## Everything else

All other Solstice code is original, or was independently reimplemented after
studying (not copying) a reference mod's approach.

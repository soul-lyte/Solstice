# One-off asset pipeline: copies/normalizes References/Textures source folders into
# src/main/resources/resourcepacks/<id>/, ready to be registered as Fabric builtin
# resource packs. Not part of the build - run manually whenever References/Textures
# changes. Overwrites the destination folders each time (safe - they're generated).

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$refTextures = Join-Path $root "References\Textures"
$destRoot = Join-Path $root "src\main\resources\resourcepacks"

# Pads a texture up to a target canvas size, centered, with no smoothing/resampling
# (nearest-neighbor only - it's pixel art). Only touches the file if it's smaller
# than the target; leaves already-correctly-sized files untouched.
function Pad-TextureCanvas($path, $targetSize) {
    $src = [System.Drawing.Bitmap]::FromFile($path)
    if ($src.Width -ge $targetSize -and $src.Height -ge $targetSize) {
        $src.Dispose()
        return
    }
    Write-Host ("  padding {0}: {1}x{2} -> {3}x{3}" -f (Split-Path $path -Leaf), $src.Width, $src.Height, $targetSize)

    $canvas = New-Object System.Drawing.Bitmap $targetSize, $targetSize, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($canvas)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $offsetX = [int]([Math]::Floor(($targetSize - $src.Width) / 2))
    $offsetY = [int]([Math]::Floor(($targetSize - $src.Height) / 2))
    $g.DrawImage($src, $offsetX, $offsetY, $src.Width, $src.Height)
    $g.Dispose()
    $src.Dispose()

    $canvas.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $canvas.Dispose()
}

function Write-PackMcmeta($destDir, $description) {
    # pack_format 34 - deliberately NOT the real 1.21.11 value (75). Above 64,
    # 1.21.11's metadata reader unconditionally requires min_format/max_format
    # fields too ("Pack declares support for version newer than 64, but is
    # missing mandatory fields..."), independent of whether supported_formats
    # is present - confirmed by testing both 75-with-supported_formats and
    # bare 75 in-game, both failed to load with the exact same error. Staying
    # under that threshold sidesteps the check entirely. These packs are only
    # PNGs/models/fonts (nothing format-sensitive), and are only ever enabled
    # programmatically, never shown in vanilla's pack picker - a "stale"
    # pack_format number has no real downside here.
    $mcmeta = @{
        pack = @{
            pack_format = 34
            description = $description
        }
    } | ConvertTo-Json -Depth 5
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText((Join-Path $destDir "pack.mcmeta"), $mcmeta, $utf8NoBom)
}

function New-Pack($id) {
    $dest = Join-Path $destRoot $id
    if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
    New-Item -ItemType Directory -Path $dest -Force | Out-Null
    return $dest
}

function Copy-AssetsSubtree($sourceAssets, $destAssets, $onlyNamespace) {
    New-Item -ItemType Directory -Path $destAssets -Force | Out-Null
    if ($onlyNamespace) {
        $src = Join-Path $sourceAssets $onlyNamespace
        $dst = Join-Path $destAssets $onlyNamespace
        Copy-Item -Path $src -Destination $dst -Recurse -Force
    } else {
        Get-ChildItem -Path $sourceAssets -Directory | Where-Object { $_.Name -ne "__MACOSX" } | ForEach-Object {
            Copy-Item -Path $_.FullName -Destination (Join-Path $destAssets $_.Name) -Recurse -Force
        }
    }
}

Write-Host "=== tools_mini_sword ==="
$dest = New-Pack "tools_mini_sword"
Copy-AssetsSubtree (Join-Path $refTextures "Tools\Sword\Mini Sword\assets") (Join-Path $dest "assets") $null
# The source pack's iron_sword.png/netherite_sword.png are a 12x12 canvas while
# every other sword in it is 16x16 - Minecraft's generated-item-model geometry is
# built from the texture's own pixel grid, so that mismatch is what made those two
# render "giant" next to the properly-mini others. Fixed here (on the bundled COPY,
# never on the original References/ file) rather than by hand, so it survives a
# re-run of this script if the reference pack gets updated again.
$itemTexDir = Join-Path $dest "assets\minecraft\textures\item"
Pad-TextureCanvas (Join-Path $itemTexDir "iron_sword.png") 16
Pad-TextureCanvas (Join-Path $itemTexDir "netherite_sword.png") 16
Write-PackMcmeta $dest "Solstice - Mini Sword tool skin"

Write-Host "=== gui_dark ==="
$dest = New-Pack "gui_dark"
Copy-AssetsSubtree (Join-Path $refTextures "GUI\GUI - Dark\Default-Dark-Mode-1.21.11-2026.4.0\assets") (Join-Path $dest "assets") "minecraft"
# The source pack also bundles a core shader override (shaders/core/rendertype_text*.fsh -
# a global hack recoloring one specific dark-gray text color so it stays readable against
# the darkened panels) and an inert assets/minecraft/optifine/color.properties (OptiFine-only,
# no-op under Fabric). Deliberately NOT bundled: compiled core shader programs are cached at
# a lower level than textures and aren't reliably reverted by a plain reloadResources() the
# way texture swaps are - this is the confirmed suspect for "still looks dark after switching
# to Vanilla/Transparent" (a GUI *skin* swap has no business patching the text shader used
# everywhere in the game anyway). The actual dark look is 100% the PNGs; nothing is lost.
Remove-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $dest "assets\minecraft\shaders")
Remove-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $dest "assets\minecraft\optifine")
Write-PackMcmeta $dest "Solstice - Dark inventory GUI skin"

Write-Host "=== gui_transparent ==="
$dest = New-Pack "gui_transparent"
Copy-AssetsSubtree (Join-Path $refTextures "GUI\GUI - Transparent\assets") (Join-Path $dest "assets") $null
# The source pack also bundles an unrelated textures/block/fire_1.png (+ mcmeta) - not a GUI
# texture at all, and it would silently fight visuals_low_fire's own fire_1.png override
# (whichever pack loads later would win, non-deterministically from the user's perspective)
# if both are enabled at once, which is a completely normal combination. A GUI skin pack has
# no business touching fire textures - dropped.
Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $dest "assets\minecraft\textures\block\fire_1.png")
Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $dest "assets\minecraft\textures\block\fire_1.png.mcmeta")
Write-PackMcmeta $dest "Solstice - Transparent inventory GUI skin"

Write-Host "=== util_bow_crossbow ==="
$dest = New-Pack "util_bow_crossbow"
$bowCrossbowSrc = Join-Path $refTextures "Utilities and misc\BOW & CROSSBOW INDICATOR"
Copy-AssetsSubtree (Join-Path $bowCrossbowSrc "assets") (Join-Path $dest "assets") $null
Copy-Item -LiteralPath (Join-Path $bowCrossbowSrc "preview.png") -Destination (Join-Path $dest "preview.png") -Force -ErrorAction SilentlyContinue
Write-PackMcmeta $dest "Solstice - Bow/Crossbow pull indicator"

Write-Host "=== util_cobweb_cyan ==="
$dest = New-Pack "util_cobweb_cyan"
Copy-AssetsSubtree (Join-Path $refTextures "Utilities and misc\Cobweb - Cyan\Cyan Cobwebs\assets") (Join-Path $dest "assets") $null
Write-PackMcmeta $dest "Solstice - Cyan cobweb"

Write-Host "=== util_cobweb_outline ==="
$dest = New-Pack "util_cobweb_outline"
$cobwebOutlineDir = Get-ChildItem -Path (Join-Path $refTextures "Utilities and misc\Cobweb -Outline") -Directory | Select-Object -First 1
Copy-AssetsSubtree (Join-Path $cobwebOutlineDir.FullName "assets") (Join-Path $dest "assets") $null
Copy-Item -LiteralPath (Join-Path $cobwebOutlineDir.FullName "preview.png") -Destination (Join-Path $dest "preview.png") -Force -ErrorAction SilentlyContinue
Write-PackMcmeta $dest "Solstice - Cobweb outline hitbox"

Write-Host "=== visuals_shield_side_low ==="
$dest = New-Pack "visuals_shield_side_low"
Copy-AssetsSubtree (Join-Path $refTextures "Utilities and misc\SideShieldPack (2)\assets") (Join-Path $dest "assets") $null
Write-PackMcmeta $dest "Solstice - Shield side/low position (Visuals module)"

Write-Host "=== visuals_low_fire ==="
$dest = New-Pack "visuals_low_fire"
# BetterPvpFire supersedes the older LowOnFire source (kept in References for history,
# no longer copied) - it covers all 6 fire/soul_fire texture variants (LowOnFire only
# ever had fire_1.png), so both the first-person fire overlay (InGameOverlayRenderer
# renders literally the block/fire_1 sprite, confirmed via decompile) AND the
# third-person burning-entity/fire-block model textures are shortened consistently,
# not just the one frame the overlay happens to reuse. The block models themselves
# (fire_floor1.json etc) are untouched vanilla parents - confirmed byte-identical to
# the real jar's own template_fire_floor.json - the "1 pixel high" look is achieved
# entirely by the flame artwork being drawn only in the texture's bottom couple of
# pixel rows, so only the textures need bundling, no model JSONs.
Copy-AssetsSubtree (Join-Path $refTextures "Utilities and misc\BetterPvpFire\assets") (Join-Path $dest "assets") $null
Write-PackMcmeta $dest "Solstice - Low fire overlay + model (Visuals module)"

Write-Host "=== visuals_small_totem_pop ==="
$dest = New-Pack "visuals_small_totem_pop"
# Only overrides totem_of_undying.json's display transforms (ground/head/thirdperson/
# firstperson/fixed all scaled down) - vanilla's own model has no "display" block at
# all, confirmed by comparing against the real jar. The old TotemParticleMixin (scales
# the golden sparkle particle burst) stays alongside this - they're complementary, not
# duplicates: the particle Mixin covers the sparkle effect, this pack covers the totem
# item's own rendered size in every context, including the death-save screen flash.
Copy-AssetsSubtree (Join-Path $refTextures "Utilities and misc\SmallTotemPop\assets") (Join-Path $dest "assets") $null
Write-PackMcmeta $dest "Solstice - Small totem pop (Visuals module)"

Write-Host "=== visuals_no_fishing_bobber ==="
$dest = New-Pack "visuals_no_fishing_bobber"
# Fully transparent fishing_hook.png (confirmed alpha=0 across the whole texture) -
# replaces the old FishingBobberRenderMixin (cancelled shouldRender outright, which
# didn't work as intended). A transparent texture hides only the bobber; the line
# itself isn't part of this texture and renders independently.
Copy-AssetsSubtree (Join-Path $refTextures "Utilities and misc\NoFishingBobber\assets") (Join-Path $dest "assets") $null
Write-PackMcmeta $dest "Solstice - No fishing bobber (Visuals module)"

function New-FontPack($id, $sourceFolderName, $description) {
    $dest = New-Pack $id
    $destFontDir = Join-Path $dest "assets\minecraft\textures\font"
    New-Item -ItemType Directory -Path $destFontDir -Force | Out-Null
    $srcFontDir = Join-Path $refTextures "Fonts\$sourceFolderName\minecraft\textures\font"
    Copy-Item -Path (Join-Path $srcFontDir "*.png") -Destination $destFontDir -Force
    Write-PackMcmeta $dest $description
}

Write-Host "=== font_capitalized ==="
New-FontPack "font_capitalized" "Capitalized Font" "Solstice - Capitalized font"

Write-Host "=== font_common32 ==="
New-FontPack "font_common32" "Common 32px Font" "Solstice - Common 32px font"

Write-Host "=== font_smooth48 ==="
New-FontPack "font_smooth48" "Smooth 48px Font" "Solstice - Smooth 48px font"

Write-Host "`nDone. Contents:"
Get-ChildItem $destRoot -Directory | ForEach-Object {
    $count = (Get-ChildItem $_.FullName -Recurse -File).Count
    Write-Host ("  {0}: {1} files" -f $_.Name, $count)
}

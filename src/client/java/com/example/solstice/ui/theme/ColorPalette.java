package com.example.solstice.ui.theme;

/**
 * Solstice color palette - all colors are ARGB integers.
 *
 * Design language: dark, minimal, near-monochrome. Hierarchy comes from
 * brightness/opacity, not a brand hue - the only "color" in the whole
 * palette is the semantic green/red used for on/off and status states.
 */
public final class ColorPalette {

    private ColorPalette() {}

    // ── Backgrounds ─────────────────────────────────────────────────────────
    // ~72-78% opacity so panels/cards/buttons read as translucent rather than
    // solid black, across both the Solstice GUI and the Video Settings screen.
    public static final int BG_DEEP        = 0xBF06080B; // near-black, ~75%
    public static final int BG_PANEL       = 0xB8090B0F; // dark panel, ~72%
    public static final int BG_CARD        = 0xB80C0F14; // card surface, ~72%
    public static final int BG_HOVER       = 0xC712161C; // hovered card, ~78%
    public static final int BG_SELECTED    = 0xC7171B21; // selected item, neutral, not a brand color

    // ── Accent ──────────────────────────────────────────────────────────────
    // A single restrained neutral accent, no brand hue, kept subdued (a thin
    // hairline highlight, not a bold fill). Used sparingly for selected-state
    // outlines and the "enabled" edge marker on cards.
    public static final int ACCENT_NEUTRAL = 0xFF9BA1AB;
    public static final int ACCENT_DIM     = 0xFF5B6169;

    // ── Text ────────────────────────────────────────────────────────────────
    public static final int TEXT_PRIMARY   = 0xFFEAECF0; // near-white
    public static final int TEXT_SECONDARY = 0xFF8B949E; // muted grey
    public static final int TEXT_DISABLED  = 0xFF484F58; // disabled state
    public static final int TEXT_ACCENT    = 0xFFC7CCD4; // neutral accent text

    // ── Status ──────────────────────────────────────────────────────────────
    public static final int STATUS_OK      = 0xFF3FB950; // green
    public static final int STATUS_WARN    = 0xFFD29922; // yellow
    public static final int STATUS_ERROR   = 0xFFF85149; // red

    // ── Toggle ──────────────────────────────────────────────────────────────
    public static final int TOGGLE_ON      = 0xFF238636; // enabled green
    public static final int TOGGLE_OFF     = 0xFF30363D; // disabled grey
    public static final int TOGGLE_KNOB    = 0xFFEAECF0; // knob white

    // ── Borders ─────────────────────────────────────────────────────────────
    // Soft / low-contrast: hard, high-contrast borders read as blocky.
    public static final int BORDER_DEFAULT = 0x8A2A2F38;
    public static final int BORDER_ACCENT  = 0xB0555C68;

    // ── Semi-transparent overlays ────────────────────────────────────────────
    // OVERLAY_DARK is a flat semi-transparent dim behind the GUI. A real
    // background blur (Screen#applyBlur) was tried here, but Minecraft only
    // allows one blur pass per frame and something else already claims it,
    // so this stays a plain dark fill instead (see SolsticeScreen/
    // ModuleDetailScreen#render()).
    public static final int OVERLAY_DARK   = 0x800A0D12; // ~50 % black
    public static final int OVERLAY_SUBTLE = 0x880D1117; // 53 % black

    // ── Category badge colors ────────────────────────────────────────────────
    // Muted, low-saturation tones only.
    public static final int CAT_ADVANCED        = 0xFF3D5166; // muted slate-blue
    public static final int CAT_QUALITY_OF_LIFE = 0xFF3D664F; // muted sage/teal
    public static final int CAT_TEXTURES        = 0xFF66593D; // muted amber/tan
    public static final int CAT_PROFILES        = 0xFF4A3D66; // muted violet

    public static int categoryColor(com.example.solstice.core.module.ModuleCategory cat) {
        return switch (cat) {
            case ADVANCED         -> CAT_ADVANCED;
            case QUALITY_OF_LIFE  -> CAT_QUALITY_OF_LIFE;
            case TEXTURES         -> CAT_TEXTURES;
            case PROFILES         -> CAT_PROFILES;
        };
    }
}

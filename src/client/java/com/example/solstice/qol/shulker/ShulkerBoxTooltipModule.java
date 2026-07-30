package com.example.solstice.qol.shulker;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

/**
 * Toggle + hold-keys for the real icon-grid container tooltip - ported to
 * match MisterPeModder/ShulkerBoxTooltip's own real trigger system (MIT,
 * see NOTICE.md), then reworked into a three-key combo per direct request:
 * holding {@link #previewKeyCode} (default Left Shift) alone shows a
 * compact, merged-and-deduplicated preview; holding it together with
 * {@link #fullPreviewKeyCode} (default Left Control) shows the full grid
 * (every slot in its real position, including empty ones); holding all
 * three (also {@link #lockKeyCode}, default Right Control) locks the last
 * full preview in place so the mouse can move away. None of the three
 * defaults is Alt - Left Alt held with Left Shift is the Windows keyboard-
 * layout-switch shortcut, and an earlier default collided with it directly
 * (see the config-key Javadocs below). See {@code
 * ShulkerTooltipDataMixin} for where the compact/full data is built, and
 * {@code HandledScreenLockOverlayMixin} for where the locked preview
 * actually gets drawn once the mouse is no longer over the original item.
 *
 * <p>Deliberately NOT real vanilla {@code KeyBinding}s - registering one
 * via {@code KeyBindingHelper} makes Fabric API list it in vanilla's own
 * Controls screen automatically, and per explicit request these three
 * hold-keys should only ever be rebindable from Solstice's own settings
 * (leaving just "Open Solstice Settings" and "Zoom" in vanilla's Controls
 * menu). Instead, each is a raw GLFW keycode persisted directly through
 * {@link ConfigManager}, polled the same way vanilla itself polls Shift/Alt
 * (see {@link #isKeyHeld(int)}) - no {@code KeyBinding} object, no
 * accessor, no vanilla options.txt entry at all.</p>
 *
 * <p>Not ported from the original mod: colored preview backgrounds (a
 * whole per-item-category color registry) and short-form item counts
 * ("1,000,000" -&gt; "1M"). Both are secondary polish, not the behavior
 * the user asked to match.</p>
 */
public final class ShulkerBoxTooltipModule extends AbstractModule {

    private static final ShulkerBoxTooltipModule INSTANCE = new ShulkerBoxTooltipModule();

    private static final String PREVIEW_KEY_CONFIG = "shulker.preview_key_code";
    /**
     * Config keys bumped to "_v2" - Left Alt (the old Full Preview default) held
     * together with Left Shift (Compact) is the exact Windows "switch keyboard
     * layout" shortcut, confirmed by direct report. Since these are raw GLFW
     * codes persisted straight through ConfigManager (not real KeyBindings),
     * changing the coded default alone would never migrate an already-persisted
     * value - same class of gotcha as the vanilla-KeyBinding case documented
     * elsewhere in this project, just needing a config-key bump instead of a
     * translation-key bump. Neither new default uses Alt at all, so no
     * combination of these three keys can ever match that shortcut again.
     */
    private static final String FULL_PREVIEW_KEY_CONFIG = "shulker.full_preview_key_code_v2";
    private static final String LOCK_KEY_CONFIG = "shulker.lock_key_code_v2";

    private int previewKeyCode = GLFW.GLFW_KEY_LEFT_SHIFT;
    private int fullPreviewKeyCode = GLFW.GLFW_KEY_LEFT_CONTROL;
    private int lockKeyCode = GLFW.GLFW_KEY_RIGHT_CONTROL;

    /** The last stack+data seen while full preview (Shift+Control) was actually active - the "arming" snapshot a lock freezes from. */
    private ItemStack armedStack = ItemStack.EMPTY;
    private ShulkerTooltipData armedData;

    /** Set by {@code recordFullPreviewFrame} and cleared at the end of every {@code tickLockState} call, so a lock only engages once the *current* frame no longer has a live hover-driven preview to rely on instead. */
    private boolean armedThisFrame;

    /** Identity of the {@code HandledScreen} instance the current armed/locked snapshot belongs to - see {@code tickLockState}. */
    private Object armedScreenIdentity;

    /** The frozen snapshot currently being drawn independent of hover, or empty if nothing is locked. */
    private ItemStack lockedStack = ItemStack.EMPTY;
    private ShulkerTooltipData lockedData;
    private int lockedX;
    private int lockedY;

    private ShulkerBoxTooltipModule() {}

    public static ShulkerBoxTooltipModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "shulker_box_tooltip"; }
    @Override public String getDisplayName() { return "Shulker Box Tooltip"; }
    @Override public String getDescription() { return "Hold a key while hovering a shulker box (or any container item) to preview its contents as a real icon grid."; }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public List<String> getSearchKeywords() {
        return List.of("shulker preview", "container preview", "inventory tooltip", "shulkerboxtooltip");
    }

    @Override protected boolean defaultEnabled() { return false; }

    @Override
    public List<ModuleSetting> getSettings() {
        return List.of(
                new ModuleSetting.KeySetting(
                        "Compact Preview Key",
                        "Hold this while hovering a container item to show a compact preview (identical items merged, empty slots skipped). Hold it together with the Full Preview Key for the full grid.",
                        () -> keyLabel(previewKeyCode),
                        this::rebindPreview),
                new ModuleSetting.KeySetting(
                        "Full Preview Key",
                        "Hold with the Compact Preview Key for the full grid (every slot, including empty ones). Also hold the Lock Key to freeze the preview in place.",
                        () -> keyLabel(fullPreviewKeyCode),
                        this::rebindFullPreview),
                new ModuleSetting.KeySetting(
                        "Lock Key",
                        "Hold together with the other two keys to lock the full preview in place, so the mouse can move away without it disappearing.",
                        () -> keyLabel(lockKeyCode),
                        this::rebindLock)
        );
    }

    @Override
    protected void init() {
        ConfigManager cfg = ConfigManager.getInstance();
        previewKeyCode = cfg.getInt(PREVIEW_KEY_CONFIG, GLFW.GLFW_KEY_LEFT_SHIFT);
        fullPreviewKeyCode = cfg.getInt(FULL_PREVIEW_KEY_CONFIG, GLFW.GLFW_KEY_LEFT_CONTROL);
        lockKeyCode = cfg.getInt(LOCK_KEY_CONFIG, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private void rebindPreview(int keyCode) {
        previewKeyCode = keyCode;
        ConfigManager.getInstance().set(PREVIEW_KEY_CONFIG, keyCode);
    }

    private void rebindFullPreview(int keyCode) {
        fullPreviewKeyCode = keyCode;
        ConfigManager.getInstance().set(FULL_PREVIEW_KEY_CONFIG, keyCode);
    }

    private void rebindLock(int keyCode) {
        lockKeyCode = keyCode;
        ConfigManager.getInstance().set(LOCK_KEY_CONFIG, keyCode);
    }

    public boolean isPreviewKeyHeld() {
        return isKeyHeld(previewKeyCode);
    }

    public boolean isFullPreviewKeyHeld() {
        return isKeyHeld(fullPreviewKeyCode);
    }

    public boolean isLockKeyHeld() {
        return isKeyHeld(lockKeyCode);
    }

    /**
     * Called from {@code ShulkerTooltipDataMixin} every frame the full
     * (Shift+Control) preview is actually showing over a real container item -
     * keeps a live snapshot so a lock (also holding the Lock Key) has
     * something recent to freeze.
     */
    public void recordFullPreviewFrame(ItemStack stack, ShulkerTooltipData data) {
        this.armedStack = stack;
        this.armedData = data;
        this.armedThisFrame = true;
    }

    /**
     * Called every frame from {@code HandledScreenLockOverlayMixin}
     * (which has real screen-space mouse coordinates, unlike the
     * tooltip-data Mixin), passing the identity of the currently
     * rendering screen. Releasing the Lock Key clears everything, and so
     * does the screen identity changing - without this, closing the
     * shulker box that was being hover-previewed and opening a
     * *different* one (or the same one for real) while still holding the
     * lock combo would keep the old, now-stale snapshot locked and draw
     * it on top of whatever new screen just opened, including a real
     * container's own slot grid (confirmed in-game: it rendered as extra
     * empty rows glued directly under a genuinely-opened shulker box's
     * real 3-row inventory). Otherwise, as long as the Lock Key is held
     * (together with the other two, which is what actually arms a
     * snapshot in the first place), the last armed snapshot is frozen and
     * kept on screen the moment the normal hover-driven preview stops
     * updating on its own - whether because the mouse left the item or
     * because a key was released - so holding all three together already
     * behaves as "locked" the instant the mouse wanders off, with no
     * extra key release required.
     */
    public void tickLockState(Object screenIdentity, int mouseX, int mouseY) {
        if (screenIdentity != armedScreenIdentity) {
            armedScreenIdentity = screenIdentity;
            armedStack = ItemStack.EMPTY;
            armedData = null;
            armedThisFrame = false;
            lockedStack = ItemStack.EMPTY;
            lockedData = null;
        }
        if (!isLockKeyHeld()) {
            lockedStack = ItemStack.EMPTY;
            lockedData = null;
            armedStack = ItemStack.EMPTY;
            armedData = null;
            armedThisFrame = false;
            return;
        }
        if (armedThisFrame) {
            // The normal hover-driven preview is rendering fine this frame - no need for the frozen overlay yet.
            lockedStack = ItemStack.EMPTY;
            lockedData = null;
        } else if (lockedStack.isEmpty() && !armedStack.isEmpty()) {
            lockedStack = armedStack;
            lockedData = armedData;
            lockedX = mouseX;
            lockedY = mouseY;
        }
        armedThisFrame = false;
    }

    public Optional<LockedPreview> getLockedPreview() {
        if (lockedStack.isEmpty() || lockedData == null) {
            return Optional.empty();
        }
        return Optional.of(new LockedPreview(lockedData, lockedX, lockedY));
    }

    public record LockedPreview(ShulkerTooltipData data, int x, int y) {}

    /**
     * The orange hover hint shown alongside the "Contains N Stacks"
     * summary (see {@code ContainerComponentAppendTooltipMixin}), guiding
     * the user through the states: nothing held, compact preview, full
     * preview, locked.
     */
    public Text getHoverHintText() {
        boolean compactHeld = isPreviewKeyHeld();
        boolean fullHeld = isFullPreviewKeyHeld();
        boolean lockHeld = isLockKeyHeld();
        String previewLabel = keyLabel(previewKeyCode);
        String fullLabel = keyLabel(fullPreviewKeyCode);
        String lockLabel = keyLabel(lockKeyCode);

        String message;
        if (compactHeld && fullHeld && lockHeld) {
            message = lockLabel + " : Locked";
        } else if (compactHeld && fullHeld) {
            message = lockLabel + " : Lock Preview";
        } else if (compactHeld) {
            message = previewLabel + " + " + fullLabel + " : View Full Content";
        } else if (lockHeld && !lockedStack.isEmpty()) {
            message = lockLabel + " : Locked";
        } else {
            message = previewLabel + " : Preview Content";
        }
        return Text.literal(message).formatted(Formatting.GOLD);
    }

    private static String keyLabel(int keyCode) {
        return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
    }

    /**
     * Polls the raw GLFW key state directly, the same way vanilla's own
     * {@code MinecraftClient.isShiftPressed()}/{@code isAltPressed()} do -
     * confirmed via decompile that those bypass {@code KeyBinding}'s own
     * tracked press state entirely because it doesn't reliably update
     * while a {@code Screen} (an inventory, in this case) has focus, which
     * is exactly when this needs to work.
     */
    private static boolean isKeyHeld(int keyCode) {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), keyCode);
    }
}

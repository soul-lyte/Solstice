package com.example.solstice.qol.shulker;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
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
 * see NOTICE.md), then reworked into a two-key combo per direct request
 * (a separate third "Lock Key" defaulted to Right Control, which - held
 * together with Left Shift and Left Control - was physically unreachable
 * with one hand): holding {@link #previewKeyCode} (default Left Shift)
 * alone shows a compact, merged-and-deduplicated preview; holding it
 * together with {@link #fullPreviewKeyCode} (default Left Control) shows
 * the full grid (every slot in its real position, including empty ones)
 * AND arms it for locking - the moment the live hover-driven preview stops
 * updating on its own (mouse moves off the item, or Shift is released)
 * while Control is still held, the last full-grid snapshot freezes in
 * place, no third key needed. Releasing Control clears the lock. See
 * {@code ShulkerTooltipDataMixin} for where the compact/full data is
 * built, and {@code HandledScreenLockOverlayMixin} for where the locked
 * preview actually gets drawn once the mouse is no longer over the
 * original item.
 *
 * <p>Deliberately NOT real vanilla {@code KeyBinding}s - registering one
 * via {@code KeyBindingHelper} makes Fabric API list it in vanilla's own
 * Controls screen automatically, and per explicit request these hold-keys
 * should only ever be rebindable from Solstice's own settings (leaving
 * just "Open Solstice Settings" and "Zoom" in vanilla's Controls menu).
 * Instead, each is a raw GLFW keycode persisted directly through {@link
 * ConfigManager}, polled the same way vanilla itself polls Shift/Alt (see
 * {@link #isKeyHeld(int)}) - no {@code KeyBinding} object, no accessor, no
 * vanilla options.txt entry at all.</p>
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
     * Config key bumped to "_v2" - Left Alt (the old Full Preview default) held
     * together with Left Shift (Compact) is the exact Windows "switch keyboard
     * layout" shortcut, confirmed by direct report. Since this is a raw GLFW
     * code persisted straight through ConfigManager (not a real KeyBinding),
     * changing the coded default alone would never migrate an already-persisted
     * value - same class of gotcha as the vanilla-KeyBinding case documented
     * elsewhere in this project, just needing a config-key bump instead of a
     * translation-key bump. The new default doesn't use Alt at all, so this
     * combo can never match that shortcut again.
     */
    private static final String FULL_PREVIEW_KEY_CONFIG = "shulker.full_preview_key_code_v2";

    private int previewKeyCode = GLFW.GLFW_KEY_LEFT_SHIFT;
    private int fullPreviewKeyCode = GLFW.GLFW_KEY_LEFT_CONTROL;

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

    /**
     * The mouse position from the most recent frame the live preview was
     * genuinely showing ({@link #armedThisFrame} true) - captured every such
     * frame, not just at the moment a freeze actually triggers. A freeze can
     * only be detected on the frame *after* the live preview stops (there's
     * no way to know "this was the last live frame" until the next one
     * fails to re-arm), and the mouse can keep moving in between - anchoring
     * to the live position rather than whatever the mouse has drifted to by
     * the detection frame is what keeps the locked box exactly where the
     * real preview actually was.
     */
    private int lastArmedMouseX;
    private int lastArmedMouseY;

    /**
     * The stack currently being tooltip-processed, captured by {@code
     * ItemStackAppendComponentTooltipMixin} at the head of {@code
     * ItemStack.appendComponentTooltip} - the real vanilla call chain that
     * leads into {@code ContainerComponent.appendTooltip}, which has no
     * reference back to the {@link ItemStack}/{@link net.minecraft.item.Item}
     * it belongs to (its own {@code ComponentsAccess} parameter is the
     * stack's internal {@code MergedComponentMap}, not the stack itself -
     * confirmed via decompile, a plain cast would have thrown {@code
     * ClassCastException}). Re-set every time any component's tooltip is
     * appended for a stack, so by the time {@code
     * ContainerComponentAppendTooltipMixin} runs it always reflects the
     * stack currently being processed.
     */
    private ItemStack currentTooltipStack = ItemStack.EMPTY;

    public void setCurrentTooltipStack(ItemStack stack) {
        this.currentTooltipStack = stack;
    }

    public boolean isCurrentStackShulkerBox() {
        return Block.getBlockFromItem(currentTooltipStack.getItem()) instanceof ShulkerBoxBlock;
    }

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
                        "Hold with the Compact Preview Key for the full grid (every slot, including empty ones). Keep holding this alone after the mouse leaves the item (or Compact Preview is released) to lock the preview in place.",
                        () -> keyLabel(fullPreviewKeyCode),
                        this::rebindFullPreview)
        );
    }

    @Override
    protected void init() {
        ConfigManager cfg = ConfigManager.getInstance();
        previewKeyCode = cfg.getInt(PREVIEW_KEY_CONFIG, GLFW.GLFW_KEY_LEFT_SHIFT);
        fullPreviewKeyCode = cfg.getInt(FULL_PREVIEW_KEY_CONFIG, GLFW.GLFW_KEY_LEFT_CONTROL);
    }

    private void rebindPreview(int keyCode) {
        previewKeyCode = keyCode;
        ConfigManager.getInstance().set(PREVIEW_KEY_CONFIG, keyCode);
    }

    private void rebindFullPreview(int keyCode) {
        fullPreviewKeyCode = keyCode;
        ConfigManager.getInstance().set(FULL_PREVIEW_KEY_CONFIG, keyCode);
    }

    public boolean isPreviewKeyHeld() {
        return isKeyHeld(previewKeyCode);
    }

    public boolean isFullPreviewKeyHeld() {
        return isKeyHeld(fullPreviewKeyCode);
    }

    /**
     * Called from {@code ShulkerTooltipDataMixin} every frame the full
     * (Shift+Control) preview is actually showing over a real container item -
     * keeps a live snapshot so a lock (keeping Control held after the live
     * preview stops updating) has something recent to freeze.
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
     * rendering screen. Releasing the Full Preview Key clears everything,
     * and so does the screen identity changing - without this, closing the
     * shulker box that was being hover-previewed and opening a
     * *different* one (or the same one for real) while still holding it
     * would keep the old, now-stale snapshot locked and draw it on top of
     * whatever new screen just opened, including a real container's own
     * slot grid (confirmed in-game: it rendered as extra empty rows glued
     * directly under a genuinely-opened shulker box's real 3-row
     * inventory). Otherwise, as long as the Full Preview Key is held
     * (which is also what arms a snapshot in the first place, together
     * with the Compact Preview Key), the last armed snapshot is frozen and
     * kept on screen the moment the normal hover-driven preview stops
     * updating on its own - whether because the mouse left the item or
     * because the Compact Preview Key was released - so simply keeping
     * Control held after that already behaves as "locked", with no
     * separate key needed.
     *
     * <p>Must run after this frame's deferred tooltip elements have already
     * flushed (see the caller for why) - otherwise {@link #armedThisFrame}
     * here is always reading last frame's value, one frame stale, which
     * both delayed/occasionally-missed the lock trigger on a quick release
     * and used a mouse position that no longer matched where the preview
     * had actually been.</p>
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
        if (armedThisFrame) {
            lastArmedMouseX = mouseX;
            lastArmedMouseY = mouseY;
        }
        if (!isFullPreviewKeyHeld()) {
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
            lockedX = lastArmedMouseX;
            lockedY = lastArmedMouseY;
        }
        armedThisFrame = false;
    }

    public Optional<LockedPreview> getLockedPreview() {
        if (lockedStack.isEmpty() || lockedData == null) {
            return Optional.empty();
        }
        return Optional.of(new LockedPreview(lockedStack, lockedData, lockedX, lockedY));
    }

    public record LockedPreview(ItemStack stack, ShulkerTooltipData data, int x, int y) {}

    /**
     * The orange hover hint shown alongside the "Contains N Stacks"
     * summary (see {@code ContainerComponentAppendTooltipMixin}), guiding
     * the user through the states: nothing held, compact preview, full
     * preview (armed for locking), locked.
     */
    public Text getHoverHintText() {
        boolean compactHeld = isPreviewKeyHeld();
        boolean fullHeld = isFullPreviewKeyHeld();
        String previewLabel = keyLabel(previewKeyCode);
        String fullLabel = keyLabel(fullPreviewKeyCode);

        String message;
        if (compactHeld && fullHeld) {
            message = "Release " + previewLabel + " to Lock";
        } else if (compactHeld) {
            message = previewLabel + " + " + fullLabel + " : View Full Content";
        } else if (fullHeld && !lockedStack.isEmpty()) {
            message = "Release " + fullLabel + " to Unlock";
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

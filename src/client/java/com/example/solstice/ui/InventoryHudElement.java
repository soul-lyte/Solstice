package com.example.solstice.ui;

import com.example.solstice.SolsticeMod;
import com.example.solstice.core.hud.HudElement;
import com.example.solstice.core.hud.HudLayoutManager;
import com.example.solstice.qol.inventoryhud.InventoryHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;

/**
 * Free-drag grid of the player's real inventory contents (hotbar + main
 * storage) - independently reimplemented after studying InventoryHUD+'s own
 * publicly described inventory-grid feature (that mod is closed-source and
 * All-Rights-Reserved, so no code was copied or ported - see NOTICE.md).
 * Deliberately doesn't touch its separate potion/armor HUD sub-features -
 * Solstice already has its own {@link ArmorHudElement}.
 *
 * <p>No "Edit" button and no "Visible" checkbox in {@code HudEditorScreen}
 * (see its {@code hasEditButton}/{@code hasVisibilityToggle}) - only
 * position/size are configurable here. This element's on/off switch is
 * {@link InventoryHudModule}'s own enable state, not a separate flag, and
 * it never draws a background or text color of its own (fully
 * transparent; item-count/durability overlays use vanilla's own fixed
 * colors).</p>
 */
public final class InventoryHudElement implements HudElement {

    private static final InventoryHudElement INSTANCE = new InventoryHudElement();

    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int STORAGE_ROWS = 3;
    private static final int HOTBAR_GAP = 4;

    /** Shown in the HUD editor instead of the real (possibly empty/varied) inventory, so positioning has a consistent full grid to work with. */
    private static final List<ItemStack> PREVIEW_STACKS = buildPreviewStacks();

    private InventoryHudElement() {}

    public static InventoryHudElement getInstance() { return INSTANCE; }

    @Override public String getId()          { return "inventory_hud"; }
    @Override public String getDisplayName() { return "Inventory HUD"; }
    @Override public int getWidth()          { return COLS * SLOT_SIZE; }
    @Override public int getHeight()         { return STORAGE_ROWS * SLOT_SIZE + HOTBAR_GAP + SLOT_SIZE; }

    @Override public boolean isVisibleByDefault() { return true; }

    @Override
    public int getDefaultX(int screenWidth, int screenHeight) {
        return screenWidth - getWidth() - 4;
    }

    @Override
    public int getDefaultY(int screenWidth, int screenHeight) {
        return 4;
    }

    /** Last-logged combined visibility, so gate state is only logged on an actual change, not every frame. */
    private Boolean lastLoggedVisible;

    /**
     * Called from {@code SolsticeClient} via {@code HudRenderCallback.EVENT}.
     *
     * <p>Deliberately does NOT also gate on {@code HudLayoutManager.isVisible(getId(), ...)} -
     * {@link InventoryHudModule}'s own enable state is the one real on/off switch for this
     * feature (see its Javadoc); a second, independently-persisted "Visible" flag risked going
     * stale from before this element's default visibility changed, silently keeping it hidden
     * even with the module on and no way to tell why. The HUD editor's per-element checkbox
     * still exists for every element uniformly, it just has no effect on this one.</p>
     */
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        boolean masterVisible = HudLayoutManager.getInstance().isMasterVisible();
        boolean noScreenOpen = client.currentScreen == null;
        boolean moduleEnabled = InventoryHudModule.getInstance().isEnabled();
        boolean visible = masterVisible && noScreenOpen && moduleEnabled;

        if (lastLoggedVisible == null || lastLoggedVisible != visible) {
            lastLoggedVisible = visible;
            SolsticeMod.LOGGER.info("[Solstice] Inventory HUD: masterVisible={}, noScreenOpen={}, moduleEnabled={} -> visible={}",
                    masterVisible, noScreenOpen, moduleEnabled, visible);
        }

        if (!visible) {
            return;
        }

        int defaultX = getDefaultX(context.getScaledWindowWidth(), context.getScaledWindowHeight());
        int defaultY = getDefaultY(context.getScaledWindowWidth(), context.getScaledWindowHeight());
        int x = HudLayoutManager.getInstance().getX(getId(), defaultX);
        int y = HudLayoutManager.getInstance().getY(getId(), defaultY);
        render(context, x, y);
    }

    @Override
    public void render(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<ItemStack> stacks = client.player == null || client.currentScreen instanceof HudEditorScreen
                ? PREVIEW_STACKS
                : orderedStacks(client.player.getInventory());

        int boxW = HudLayoutManager.getInstance().getWidth(getId(), getWidth());
        int boxH = HudLayoutManager.getInstance().getHeight(getId(), getHeight());

        // A single uniform scale (not withContentScale's independent X/Y stretch) - resizing
        // this element should zoom the grid in/out, never distort its square item icons.
        float scale = Math.min((float) boxW / getWidth(), (float) boxH / getHeight());

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-x, -y);
        drawGrid(context, client, x, y, stacks);
        context.getMatrices().popMatrix();
    }

    private void drawGrid(DrawContext context, MinecraftClient client, int x, int y, List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int row = i / COLS;
            int col = i % COLS;
            int extraGap = row >= STORAGE_ROWS ? HOTBAR_GAP : 0;
            int slotX = x + col * SLOT_SIZE + 1;
            int slotY = y + row * SLOT_SIZE + extraGap + 1;
            context.drawItem(stack, slotX, slotY);
            context.drawStackOverlay(client.textRenderer, stack, slotX, slotY);
        }
    }

    /** Main storage (27 slots) first, hotbar (9 slots) last on its own row - mirrors vanilla's own inventory screen layout. */
    private static List<ItemStack> orderedStacks(PlayerInventory inventory) {
        DefaultedList<ItemStack> main = inventory.getMainStacks();
        int hotbarSize = PlayerInventory.getHotbarSize();
        List<ItemStack> ordered = new ArrayList<>(main.size());
        for (int i = hotbarSize; i < main.size(); i++) {
            ordered.add(main.get(i));
        }
        for (int i = 0; i < hotbarSize; i++) {
            ordered.add(main.get(i));
        }
        return ordered;
    }

    /** A diamond "pot pvp" kit - armor, gapples, ender pearls, blocks, and a wall of splash healing potions. */
    private static List<ItemStack> buildPreviewStacks() {
        ItemStack healingSplash = PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.HEALING);
        List<ItemStack> preview = new ArrayList<>(36);

        preview.add(new ItemStack(Items.DIAMOND_HELMET));
        preview.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
        preview.add(new ItemStack(Items.DIAMOND_LEGGINGS));
        preview.add(new ItemStack(Items.DIAMOND_BOOTS));
        preview.add(new ItemStack(Items.DIAMOND_SWORD));
        preview.add(new ItemStack(Items.GOLDEN_APPLE, 16));
        preview.add(new ItemStack(Items.ENDER_PEARL, 16));
        preview.add(new ItemStack(Items.OBSIDIAN, 64));
        preview.add(new ItemStack(Items.COBWEB, 64));
        while (preview.size() < 36) {
            preview.add(healingSplash.copy());
        }

        return List.copyOf(preview);
    }
}

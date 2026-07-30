package com.example.solstice.ui;

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
 * <p>No "Edit" button in {@code HudEditorScreen} (see its {@code
 * hasEditButton}) - same treatment as {@link BossBarHudElement}: only
 * position/size and visibility are configurable, no background or text
 * color, since this element never draws a background of its own (fully
 * transparent) and its item-count/durability overlays use vanilla's own
 * fixed colors.</p>
 */
public final class InventoryHudElement implements HudElement {

    private static final InventoryHudElement INSTANCE = new InventoryHudElement();

    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int STORAGE_ROWS = 3;
    private static final int HOTBAR_GAP = 3;

    /** Shown in the HUD editor instead of the real (possibly empty/varied) inventory, so positioning has a consistent full grid to work with. */
    private static final List<ItemStack> PREVIEW_STACKS = buildPreviewStacks();

    private InventoryHudElement() {}

    public static InventoryHudElement getInstance() { return INSTANCE; }

    @Override public String getId()          { return "inventory_hud"; }
    @Override public String getDisplayName() { return "Inventory HUD"; }
    @Override public int getWidth()          { return COLS * SLOT_SIZE; }
    @Override public int getHeight()         { return STORAGE_ROWS * SLOT_SIZE + HOTBAR_GAP + SLOT_SIZE; }
    @Override public boolean isVisibleByDefault() { return false; }

    @Override
    public int getDefaultX(int screenWidth, int screenHeight) {
        return screenWidth - getWidth() - 4;
    }

    @Override
    public int getDefaultY(int screenWidth, int screenHeight) {
        return 4;
    }

    /** Called from {@code SolsticeClient} via {@code HudRenderCallback.EVENT}. */
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        boolean visible = HudLayoutManager.getInstance().isMasterVisible()
                && client.currentScreen == null
                && InventoryHudModule.getInstance().isEnabled()
                && HudLayoutManager.getInstance().isVisible(getId(), isVisibleByDefault());
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
        HudLayoutManager.withContentScale(context, x, y, getWidth(), getHeight(), boxW, boxH,
                () -> drawGrid(context, client, x, y, stacks));
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

    private static List<ItemStack> buildPreviewStacks() {
        ItemStack healingPotion = PotionContentsComponent.createStack(Items.POTION, Potions.HEALING);
        List<ItemStack> preview = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) {
            preview.add(healingPotion.copy());
        }
        return List.copyOf(preview);
    }
}

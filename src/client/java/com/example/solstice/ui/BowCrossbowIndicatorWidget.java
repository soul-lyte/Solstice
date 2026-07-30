package com.example.solstice.ui;

import com.example.solstice.qol.visuals.VisualsModule;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Draws a small charge-percentage bar under the crosshair while actively
 * drawing a bow or crossbow - vanilla gives no feedback at all for how far
 * along the pull is, just the bow's own itch/twang animation frames.
 *
 * <p>Not a {@link com.example.solstice.core.hud.HudElement} - it's only ever
 * visible for the few seconds a bow/crossbow is being drawn, so there's
 * nothing meaningful to reposition from the HUD editor.</p>
 */
public final class BowCrossbowIndicatorWidget {

    private static final BowCrossbowIndicatorWidget INSTANCE = new BowCrossbowIndicatorWidget();

    private static final int BAR_W = 60;
    private static final int BAR_H = 4;
    private static final int Y_OFFSET = 24;

    private BowCrossbowIndicatorWidget() {}

    public static BowCrossbowIndicatorWidget getInstance() { return INSTANCE; }

    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) return;
        if (!VisualsModule.getInstance().isFeatureActive(VisualsModule.bowCrossbowIndicator)) return;
        if (!player.isUsingItem()) return;

        ItemStack stack = player.getActiveItem();
        float progress;
        if (stack.isOf(Items.BOW)) {
            progress = BowItem.getPullProgress(player.getItemUseTime());
        } else if (stack.isOf(Items.CROSSBOW) && !CrossbowItem.isCharged(stack)) {
            // CrossbowItem.getPullProgress(int, ItemStack, LivingEntity) is private -
            // reimplement its ratio (useTicks / pullTime) from the public getPullTime.
            int pullTime = CrossbowItem.getPullTime(stack, player);
            progress = pullTime <= 0 ? 0f : Math.min(1f, player.getItemUseTime() / (float) pullTime);
        } else {
            return;
        }

        int centerX = context.getScaledWindowWidth() / 2;
        int centerY = context.getScaledWindowHeight() / 2;
        int x = centerX - BAR_W / 2;
        int y = centerY + Y_OFFSET;

        SolsticeTheme.fillRect(context, x, y, BAR_W, BAR_H, 0x80000000);
        int fillW = Math.round(BAR_W * Math.max(0f, Math.min(1f, progress)));
        if (fillW > 0) {
            SolsticeTheme.fillRect(context, x, y, fillW, BAR_H, ColorPalette.ACCENT_NEUTRAL);
        }
    }
}

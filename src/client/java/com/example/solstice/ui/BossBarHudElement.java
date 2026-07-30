package com.example.solstice.ui;

import com.example.solstice.core.hud.HudElement;
import com.example.solstice.core.hud.HudLayoutManager;
import com.example.solstice.mixin.hud.BossBarHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repositions vanilla's real boss bar rendering (Wither/Ender Dragon/server
 * custom bars) instead of reimplementing it - {@code BossBarHudMixin}
 * cancels {@code BossBarHud.render}'s own fixed, screen-centered layout;
 * this draws the exact same vanilla textures via the same private per-bar
 * method (exposed through {@link BossBarHudAccessor}), just anchored at
 * Solstice's own stored position instead of the screen center.
 *
 * <p>Confirmed via decompile: vanilla stacks additional bars 19px apart
 * (10 for the name line + 9 gap) and centers each bar's name above it -
 * replicated here, just centered on this element's own x instead of the
 * screen's, and stopping once bars would run past a third of the screen
 * height, matching vanilla's own overflow guard.</p>
 *
 * <p>When there's no real boss bar active, {@link #render} substitutes a
 * fake example bar instead of drawing nothing - real gameplay (via {@link
 * #onHudRender}) still hides the whole element when there's genuinely
 * nothing to show, since that method's own early-return happens before
 * ever calling {@link #render}; only the HUD editor's direct {@code
 * render} call ever sees the example, which is the point - it gives the
 * editor something real to position instead of an empty box.</p>
 */
public final class BossBarHudElement implements HudElement {

    private static final BossBarHudElement INSTANCE = new BossBarHudElement();

    private static final int BAR_WIDTH  = 182;
    private static final int STACK_STEP = 19;

    private static final BossBar EXAMPLE_BAR = new BossBar(UUID.randomUUID(),
            Text.literal("Example Boss Bar"), BossBar.Color.PINK, BossBar.Style.PROGRESS) {};

    static {
        EXAMPLE_BAR.setPercent(0.7f);
    }

    private BossBarHudElement() {}

    public static BossBarHudElement getInstance() { return INSTANCE; }

    @Override public String getId()          { return "boss_bar"; }
    @Override public String getDisplayName() { return "Boss Bar"; }
    @Override public int getWidth()          { return BAR_WIDTH; }
    @Override public int getHeight()         { return STACK_STEP; }

    @Override
    public int getDefaultX(int screenWidth, int screenHeight) {
        return screenWidth / 2 - BAR_WIDTH / 2;
    }

    @Override
    public int getDefaultY(int screenWidth, int screenHeight) {
        return 12;
    }

    /** Called from {@link com.example.solstice.SolsticeClient} via {@code HudRenderCallback.EVENT}. */
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        boolean visible = HudLayoutManager.getInstance().isMasterVisible()
                && client.currentScreen == null
                && HudLayoutManager.getInstance().isVisible(getId(), true);
        if (!visible) {
            return;
        }

        BossBarHudAccessor accessor = (BossBarHudAccessor) client.inGameHud.getBossBarHud();
        if (accessor.solstice$getBossBars().isEmpty()) {
            return;
        }

        int defaultX = getDefaultX(context.getScaledWindowWidth(), context.getScaledWindowHeight());
        int defaultY = getDefaultY(context.getScaledWindowWidth(), context.getScaledWindowHeight());
        int x = HudLayoutManager.getInstance().getX(getId(), defaultX);
        int y = HudLayoutManager.getInstance().getY(getId(), defaultY);
        render(context, x, y);
    }

    /**
     * Draws the widget at the given position - used both by real gameplay and the HUD
     * editor's live preview.
     */
    @Override
    public void render(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        BossBarHud bossBarHud = client.inGameHud.getBossBarHud();
        BossBarHudAccessor accessor = (BossBarHudAccessor) bossBarHud;
        Collection<ClientBossBar> realBars = accessor.solstice$getBossBars().values();
        Collection<? extends BossBar> bars = realBars.isEmpty() ? List.of(EXAMPLE_BAR) : realBars;

        int boxW = HudLayoutManager.getInstance().getWidth(getId(), getWidth());
        int boxH = HudLayoutManager.getInstance().getHeight(getId(), getHeight());

        context.createNewRootLayer();
        HudLayoutManager.withContentScale(context, x, y, getWidth(), getHeight(), boxW, boxH, () -> {
            int barY = y;
            for (BossBar bar : bars) {
                accessor.solstice$renderBossBar(context, x, barY, bar);

                Text name = bar.getName();
                int textWidth = client.textRenderer.getWidth(name);
                int textX = x + (BAR_WIDTH - textWidth) / 2;
                context.drawTextWithShadow(client.textRenderer, name, textX, barY - 9, -1);

                barY += STACK_STEP;
                if (barY - y >= context.getScaledWindowHeight() / 3) {
                    break;
                }
            }
        });
    }
}

package com.example.solstice.ui.widget;

import com.example.solstice.core.module.Module;
import com.example.solstice.ui.EditableModule;
import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * A card widget representing one Solstice module in the settings screen.
 *
 * <p>Layout (width = parent-defined, height = {@link #HEIGHT} px):
 * <pre>
 * ┌──────────────────────────────────────────────────┐
 * │  Module Display Name (bold)         [Toggle]      │
 * │  Description text (muted, wraps)                  │
 * │                                                    │
 * │ [                 Edit (if editable)             ]│
 * └──────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Left-click toggles the module (or opens settings for always-on
 * placeholder modules, which have nothing to toggle). Right-click opens the
 * module's dedicated settings screen - or, for a module implementing
 * {@link EditableModule}, jumps straight to its edit screen instead, same as
 * clicking the bottom edit bar would. The bottom edit bar spans the full card
 * width (not just a small corner button) for modules implementing
 * {@link EditableModule}; a small gear icon in the bottom-right corner marks
 * any other card with a real (non-empty) settings list - left-clicking that
 * icon specifically opens settings directly, without toggling the module
 * underneath it.</p>
 */
public class ModuleCardWidget extends SolsticeClickableWidget {

    public static final int HEIGHT = 66;
    private static final int EDIT_BAR_H = 14;
    private static final int PADDING = 8;
    private static final int TOGGLE_W = 26;
    private static final int GEAR_SIZE = 10;
    private static final int GEAR_TEX_SIZE = 32;
    private static final Identifier GEAR_ICON = Identifier.of("solstice", "textures/gui/icons/settings_gear.png");

    private final Module module;
    private final TextRenderer textRenderer;
    private final Consumer<Module> onOpenSettings;

    /**
     * When set, this card is the partial "peek" row overhanging past the
     * module grid's footer line ({@code SolsticeScreen.footerButtonY}) - drawn
     * fully but visually cut off at this Y via a scissor scoped entirely to
     * this one widget's own {@link #renderWidget} call (opened and closed
     * within the same method, never wrapping a shared {@code super.render()}
     * call the way the earlier, reverted attempt at this did - see
     * {@code SolsticeScreen}'s own history note on why that broke the
     * category tabs). Hit-testing is clipped the same way, so the hidden
     * portion isn't clickable either.
     */
    private Integer clipBottomY;

    public ModuleCardWidget(int x, int y, int width, Module module, TextRenderer textRenderer,
                             Consumer<Module> onOpenSettings) {
        super(x, y, width, HEIGHT, Text.of(module.getDisplayName()));
        this.module = module;
        this.textRenderer = textRenderer;
        this.onOpenSettings = onOpenSettings;
        setTooltip(Tooltip.of(Text.of(module.getDescription() + " (right-click for settings)")));
    }

    /** See {@link #clipBottomY}. Pass {@code null} to render/click normally, uncut. */
    public void setClipBottomY(Integer y) {
        this.clipBottomY = y;
    }

    private boolean isClippedAt(int y) {
        return clipBottomY != null && y >= clipBottomY;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (clipBottomY != null && mouseY >= clipBottomY) {
            return false;
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean scissoring = clipBottomY != null && clipBottomY < getY() + HEIGHT;
        if (scissoring) {
            context.enableScissor(getX(), getY(), getX() + getWidth(), clipBottomY);
        }
        try {
            renderContent(context, mouseX, mouseY);
        } finally {
            if (scissoring) {
                context.disableScissor();
            }
        }
    }

    private void renderContent(DrawContext context, int mouseX, int mouseY) {
        boolean hovered = isHovered();

        SolsticeTheme.drawCard(context, getX(), getY(), getWidth(), HEIGHT, hovered);

        // Toggle (or always-on badge) - top right
        int tx = getX() + getWidth() - TOGGLE_W - PADDING;
        int ty = getY() + PADDING;
        if (module.isAlwaysOn()) {
            SolsticeTheme.drawAlwaysOnBadge(context, tx, ty, textRenderer);
        } else {
            SolsticeTheme.drawToggle(context, tx, ty, module.isEnabled());
        }

        // Module name
        context.drawText(textRenderer, module.getDisplayName(),
                getX() + PADDING, getY() + 22,
                module.isEnabled() ? ColorPalette.TEXT_PRIMARY : ColorPalette.TEXT_DISABLED,
                false);

        // Description (truncated if too long - right-click for the full text)
        String desc = module.getDescription();
        int maxDescW = getWidth() - PADDING * 2 - TOGGLE_W - 4;
        if (textRenderer.getWidth(desc) > maxDescW) {
            desc = textRenderer.trimToWidth(desc, maxDescW - textRenderer.getWidth("…")) + "…";
        }
        context.drawText(textRenderer, desc,
                getX() + PADDING, getY() + 34,
                ColorPalette.TEXT_SECONDARY, false);

        // "Edit" bar - full card width, bottom edge, only for modules with a
        // dedicated edit screen (e.g. Visuals' sub-toggle list).
        if (module instanceof EditableModule) {
            int[] r = editButtonRect();
            boolean editHovered = mouseX >= r[0] && mouseX <= r[0] + r[2]
                    && mouseY >= r[1] && mouseY <= r[1] + r[3];
            SolsticeTheme.fillRect(context, r[0], r[1], r[2], r[3], editHovered ? ColorPalette.BG_HOVER : ColorPalette.BG_PANEL);
            SolsticeTheme.drawBorder(context, r[0], r[1], r[2], r[3], ColorPalette.BORDER_DEFAULT);
            String label = "Edit";
            int tw = textRenderer.getWidth(label);
            context.drawText(textRenderer, label, r[0] + (r[2] - tw) / 2, r[1] + (r[3] - 8) / 2,
                    ColorPalette.TEXT_SECONDARY, false);
        } else if (!module.getSettings().isEmpty()) {
            // Small gear icon, bottom-right - both a visual hint that this card
            // has a real settings list, and directly clickable (left-click) to
            // open it without toggling the module underneath it.
            int[] r = gearIconRect();
            // The 10-arg overload defaults its sample region to the on-screen draw size
            // (r[2]/r[3], 10x10) rather than the real texture size, so it only ever
            // sampled a 10x10 corner of the actual 32x32 icon - the same drawTexture
            // region-size bug already found and fixed a few times elsewhere in this
            // project. The 12-arg overload samples the whole texture and scales it down.
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GEAR_ICON, r[0], r[1], 0f, 0f,
                    r[2], r[3], GEAR_TEX_SIZE, GEAR_TEX_SIZE, GEAR_TEX_SIZE, GEAR_TEX_SIZE);
        }
    }

    private int[] editButtonRect() {
        return new int[]{getX(), getY() + HEIGHT - EDIT_BAR_H, getWidth(), EDIT_BAR_H};
    }

    private int[] gearIconRect() {
        return new int[]{getX() + getWidth() - PADDING - GEAR_SIZE, getY() + HEIGHT - PADDING - GEAR_SIZE, GEAR_SIZE, GEAR_SIZE};
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        if (module instanceof EditableModule editable) {
            int[] r = editButtonRect();
            if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3] && !isClippedAt(my)) {
                SolsticeSounds.playClick();
                MinecraftClient.getInstance().setScreen(editable.createEditScreen(MinecraftClient.getInstance().currentScreen));
                return true;
            }
        } else if (!module.getSettings().isEmpty() && click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int[] r = gearIconRect();
            if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3] && !isClippedAt(my)) {
                SolsticeSounds.playClick();
                onOpenSettings.accept(module);
                return true;
            }
        }
        if (isHovered() && click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            SolsticeSounds.playClick();
            if (module instanceof EditableModule editable) {
                MinecraftClient.getInstance().setScreen(editable.createEditScreen(MinecraftClient.getInstance().currentScreen));
            } else {
                onOpenSettings.accept(module);
            }
            return true;
        }
        if (module.isAlwaysOn()) {
            // Nothing to toggle - left-click opens the settings screen too.
            if (super.mouseClicked(click, doubled)) {
                SolsticeSounds.playClick();
                onOpenSettings.accept(module);
                return true;
            }
            return false;
        }
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            module.setEnabled(!module.isEnabled());
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}

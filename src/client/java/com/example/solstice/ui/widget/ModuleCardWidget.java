package com.example.solstice.ui.widget;

import com.example.solstice.core.module.Module;
import com.example.solstice.ui.EditableModule;
import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
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
 * placeholder modules, which have nothing to toggle). Right-click always
 * opens the module's dedicated settings screen. The bottom edit bar spans
 * the full card width (not just a small corner button) for modules
 * implementing {@link EditableModule}.</p>
 */
public class ModuleCardWidget extends SolsticeClickableWidget {

    public static final int HEIGHT = 66;
    private static final int EDIT_BAR_H = 14;
    private static final int PADDING = 8;
    private static final int TOGGLE_W = 26;

    private final Module module;
    private final TextRenderer textRenderer;
    private final Consumer<Module> onOpenSettings;

    public ModuleCardWidget(int x, int y, int width, Module module, TextRenderer textRenderer,
                             Consumer<Module> onOpenSettings) {
        super(x, y, width, HEIGHT, Text.of(module.getDisplayName()));
        this.module = module;
        this.textRenderer = textRenderer;
        this.onOpenSettings = onOpenSettings;
        setTooltip(Tooltip.of(Text.of(module.getDescription() + " (right-click for settings)")));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
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
        }
    }

    private int[] editButtonRect() {
        return new int[]{getX(), getY() + HEIGHT - EDIT_BAR_H, getWidth(), EDIT_BAR_H};
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        if (module instanceof EditableModule editable) {
            int[] r = editButtonRect();
            if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                SolsticeSounds.playClick();
                MinecraftClient.getInstance().setScreen(editable.createEditScreen(MinecraftClient.getInstance().currentScreen));
                return true;
            }
        }
        if (isHovered() && click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            SolsticeSounds.playClick();
            onOpenSettings.accept(module);
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

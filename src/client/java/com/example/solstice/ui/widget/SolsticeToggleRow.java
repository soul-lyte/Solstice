package com.example.solstice.ui.widget;

import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A labeled row with a toggle pill on the right, for {@code BooleanSetting}s
 * on the module settings screen. Mirrors the toggle look used on module
 * cards ({@link SolsticeTheme#drawToggle}).
 */
public class SolsticeToggleRow extends SolsticeClickableWidget {

    private static final int TOGGLE_W = 26;
    private static final int PADDING = 6;

    private final TextRenderer textRenderer;
    private final String label;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public SolsticeToggleRow(int x, int y, int width, int height, TextRenderer textRenderer,
                              String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, width, height, Text.of(label));
        this.textRenderer = textRenderer;
        this.label = label;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean on = getter.getAsBoolean();

        SolsticeTheme.fillRect(context, getX(), getY(), getWidth(), getHeight(),
                isHovered() ? ColorPalette.BG_HOVER : ColorPalette.BG_CARD);
        SolsticeTheme.drawBorder(context, getX(), getY(), getWidth(), getHeight(), ColorPalette.BORDER_DEFAULT);

        context.drawText(textRenderer, label,
                getX() + PADDING, getY() + (getHeight() - 8) / 2,
                ColorPalette.TEXT_PRIMARY, false);

        int tx = getX() + getWidth() - TOGGLE_W - PADDING;
        int ty = getY() + (getHeight() - 14) / 2;
        SolsticeTheme.drawToggle(context, tx, ty, on);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            setter.accept(!getter.getAsBoolean());
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}

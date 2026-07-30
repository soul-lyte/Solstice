package com.example.solstice.ui.widget;

import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * A generic numeric slider for the module settings screen. Works on the
 * normalized [0, 1] range vanilla {@link SliderWidget} expects internally,
 * mapping to/from a caller-supplied [min, max] range.
 */
public class SolsticeSliderWidget extends SliderWidget {

    private final TextRenderer textRenderer;
    private final String label;
    private final double min;
    private final double max;
    private final DoubleFunction<String> formatter;
    private final DoubleConsumer onChange;

    public SolsticeSliderWidget(int x, int y, int width, int height, TextRenderer textRenderer,
                                 String label, double min, double max, double current,
                                 DoubleFunction<String> formatter, DoubleConsumer onChange) {
        super(x, y, width, height, Text.empty(), normalize(current, min, max));
        this.textRenderer = textRenderer;
        this.label = label;
        this.min = min;
        this.max = max;
        this.formatter = formatter;
        this.onChange = onChange;
        updateMessage();
    }

    private static double normalize(double v, double min, double max) {
        if (max <= min) return 0;
        return Math.min(1.0, Math.max(0.0, (v - min) / (max - min)));
    }

    private double currentValue() {
        return min + (max - min) * this.value;
    }

    @Override
    protected void updateMessage() {
        // SliderWidget's own constructor calls this hook before this subclass's
        // fields are assigned - guard against that first, harmless call.
        if (formatter == null) return;
        setMessage(Text.of(label + ": " + formatter.apply(currentValue())));
    }

    @Override
    protected void applyValue() {
        if (onChange == null) return;
        onChange.accept(currentValue());
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        // Solstice's own click sound on grab, instead of vanilla's - matches every
        // other Solstice widget (see SolsticeClickableWidget, which this can't extend
        // directly since it needs SliderWidget's own drag machinery instead).
        SolsticeSounds.playClick();
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.fillRect(context, getX(), getY(), getWidth(), getHeight(), ColorPalette.BG_CARD);

        int fillW = (int) (getWidth() * this.value);
        if (fillW > 0) {
            SolsticeTheme.fillRect(context, getX(), getY(), fillW, getHeight(),
                    isHovered() ? ColorPalette.ACCENT_NEUTRAL : ColorPalette.ACCENT_DIM);
        }
        SolsticeTheme.drawBorder(context, getX(), getY(), getWidth(), getHeight(), ColorPalette.BORDER_DEFAULT);

        int textW = textRenderer.getWidth(getMessage());
        context.drawText(textRenderer, getMessage(),
                getX() + (getWidth() - textW) / 2, getY() + (getHeight() - 8) / 2,
                ColorPalette.TEXT_PRIMARY, false);
    }
}

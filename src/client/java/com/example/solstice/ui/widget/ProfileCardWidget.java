package com.example.solstice.ui.widget;

import com.example.solstice.profiles.Profile;
import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

/**
 * A card for one {@link Profile} in a Profiles-tab row - the profile's own
 * name, and a click applies it immediately (no confirmation - every effect
 * a profile has is just enabling modules/settings that are freely
 * changeable again afterward, nothing destructive). Shows an accent
 * border when it's the profile currently matching live settings (see
 * {@code ProfileManager#getActiveProfile}). Saved Custom profiles also get
 * a small "Rename" hotzone in the corner - built-in profiles don't.
 *
 * <p>The description is a hover tooltip, not inline text - at the card's
 * real width most descriptions got truncated with an ellipsis and had no
 * way to be read in full.</p>
 */
public class ProfileCardWidget extends SolsticeClickableWidget {

    public static final int HEIGHT = 48;
    private static final int PADDING = 8;
    private static final int RENAME_W = 40;
    private static final int RENAME_H = 10;

    private final Profile profile;
    private final TextRenderer textRenderer;
    private final boolean selected;
    private final Runnable onRename;
    private final Runnable onApplied;

    public ProfileCardWidget(int x, int y, int width, Profile profile, TextRenderer textRenderer,
                              boolean selected, Runnable onRename, Runnable onApplied) {
        super(x, y, width, HEIGHT, Text.of(profile.getName()));
        this.profile = profile;
        this.textRenderer = textRenderer;
        this.selected = selected;
        this.onRename = onRename;
        this.onApplied = onApplied;
        setTooltip(Tooltip.of(Text.of(profile.getDescription())));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.drawCard(context, getX(), getY(), getWidth(), HEIGHT, isHovered());
        if (selected) {
            SolsticeTheme.drawBorder(context, getX(), getY(), getWidth(), HEIGHT, ColorPalette.ACCENT_NEUTRAL);
        }

        context.drawText(textRenderer, profile.getName(), getX() + PADDING, getY() + 6, ColorPalette.TEXT_PRIMARY, false);

        String hint = selected ? "Selected" : "Click to apply";
        context.drawText(textRenderer, hint, getX() + PADDING, getY() + HEIGHT - 12,
                selected ? ColorPalette.ACCENT_NEUTRAL : ColorPalette.ACCENT_DIM, false);

        if (onRename != null) {
            int rx = getX() + getWidth() - RENAME_W - 4;
            int ry = getY() + 4;
            boolean renameHovered = mouseX >= rx && mouseX < rx + RENAME_W && mouseY >= ry && mouseY < ry + RENAME_H;
            context.drawText(textRenderer, "Rename", rx, ry,
                    renameHovered ? ColorPalette.TEXT_PRIMARY : ColorPalette.TEXT_SECONDARY, false);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!isMouseOver(click.x(), click.y())) {
            return false;
        }
        if (onRename != null) {
            int rx = getX() + getWidth() - RENAME_W - 4;
            int ry = getY() + 4;
            if (click.x() >= rx && click.x() < rx + RENAME_W && click.y() >= ry && click.y() < ry + RENAME_H) {
                SolsticeSounds.playClick();
                onRename.run();
                return true;
            }
        }
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            profile.apply();
            if (onApplied != null) {
                onApplied.run();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}

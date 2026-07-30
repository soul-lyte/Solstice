package com.example.solstice.ui;

import com.example.solstice.profiles.Renameable;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Small popup to rename a saved Custom profile. */
public class ProfileRenameScreen extends Screen {

    private static final int PANEL_W = 200;
    private static final int PANEL_H = 90;
    private static final int PADDING = 12;
    private static final int MAX_NAME_LENGTH = 24;

    private final Screen parent;
    private final Renameable target;
    private final Runnable onRenamed;
    private int panelX, panelY;
    private TextFieldWidget nameField;

    public ProfileRenameScreen(Screen parent, Renameable target, Runnable onRenamed) {
        super(Text.literal("Rename Profile"));
        this.parent = parent;
        this.target = target;
        this.onRenamed = onRenamed;
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;

        int x = panelX + PADDING;
        int innerW = PANEL_W - PADDING * 2;

        nameField = new TextFieldWidget(textRenderer, x, panelY + 24, innerW, 18, Text.of("Profile name"));
        nameField.setMaxLength(MAX_NAME_LENGTH);
        nameField.setText(target.getName());
        addDrawableChild(nameField);
        setInitialFocus(nameField);

        int halfW = (innerW - 4) / 2;
        int by = panelY + PANEL_H - 24;
        addDrawableChild(new SolsticeButton(x, by, halfW, 18, textRenderer, "Save", () -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                target.rename(name);
                onRenamed.run();
            }
            close();
        }));
        addDrawableChild(new SolsticeButton(x + halfW + 4, by, halfW, 18, textRenderer, "Cancel", this::close));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, ColorPalette.OVERLAY_DARK);
        SolsticeTheme.drawPanel(context, panelX, panelY, PANEL_W, PANEL_H);
        context.drawText(textRenderer, "Rename Profile", panelX + PADDING, panelY + 8, ColorPalette.TEXT_PRIMARY, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // No shouldPause() override - see SolsticeScreen's identical note.
}

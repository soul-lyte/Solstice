package com.example.solstice.ui;

import com.example.solstice.core.hud.HudElement;
import com.example.solstice.core.hud.HudLayoutManager;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import com.example.solstice.ui.widget.SolsticeToggleRow;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * HUD editor: shows a draggable, resizable, labeled box for each registered
 * {@link HudElement}. Drag the box to move it, drag its bottom-right corner
 * handle to resize it, click the small checkbox above its box to show/hide
 * it without opening anything else, and click "Edit" (where present) to
 * open {@link HudWidgetConfigScreen} for its text color, background color,
 * and exact size. {@link BossBarHudElement} has no "Edit" button - its
 * look is meant to always match vanilla's own boss bar, only whether it
 * shows and where are configurable.
 *
 * <p>Deliberately does not blur or dim the background like {@link SolsticeScreen}
 * does - you need to see the real game behind it while positioning things.
 * Built to support any number of registered elements without changes.</p>
 */
public class HudEditorScreen extends Screen {

    private static final int RESIZE_HANDLE_SIZE = 10;
    private static final int EDIT_BUTTON_W = 32;
    private static final int EDIT_BUTTON_H = 12;
    private static final int TOGGLE_SIZE = 12;

    private final Screen parent;

    private HudElement draggingElement;
    private int dragOffsetX, dragOffsetY;
    private int dragX, dragY;

    private HudElement resizingElement;
    private int resizeStartMouseX, resizeStartMouseY, resizeStartW, resizeStartH;
    private int liveW, liveH;

    public HudEditorScreen(Screen parent) {
        super(Text.of("Edit HUD Layout"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(new SolsticeToggleRow(8, 8, 200, 16, textRenderer, "Show Solstice HUD",
                () -> HudLayoutManager.getInstance().isMasterVisible(),
                HudLayoutManager.getInstance()::setMasterVisible));

        addDrawableChild(new SolsticeButton(width / 2 - 50, height - 28, 100, 20,
                textRenderer, "Done", this::close));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        HudLayoutManager layout = HudLayoutManager.getInstance();
        for (HudElement element : layout.getElements()) {
            boolean dragging = element == draggingElement;
            boolean resizing = element == resizingElement;
            String id = element.getId();

            int defaultX = element.getDefaultX(width, height);
            int defaultY = element.getDefaultY(width, height);
            int x = dragging ? dragX : layout.getX(id, defaultX);
            int y = dragging ? dragY : layout.getY(id, defaultY);
            int w = resizing ? liveW : layout.getWidth(id, element.getWidth());
            int h = resizing ? liveH : layout.getHeight(id, element.getHeight());
            boolean visible = !hasVisibilityToggle(element) || layout.isVisible(id, element.isVisibleByDefault());

            if (visible) {
                element.render(context, x, y);
            } else {
                SolsticeTheme.fillRect(context, x, y, w, h, 0x40000000);
            }

            SolsticeTheme.drawBorder(context, x, y, w, h,
                    dragging || resizing ? ColorPalette.ACCENT_NEUTRAL : ColorPalette.BORDER_ACCENT);
            context.drawText(textRenderer, element.getDisplayName(), x, y - 10,
                    visible ? ColorPalette.TEXT_PRIMARY : ColorPalette.TEXT_DISABLED, true);

            if (hasVisibilityToggle(element)) {
                int[] toggleRect = toggleRect(element, x, y, w);
                SolsticeTheme.fillRect(context, toggleRect[0], toggleRect[1], toggleRect[2], toggleRect[3],
                        visible ? ColorPalette.ACCENT_NEUTRAL : ColorPalette.BG_CARD);
                SolsticeTheme.drawBorder(context, toggleRect[0], toggleRect[1], toggleRect[2], toggleRect[3], ColorPalette.BORDER_DEFAULT);
            }

            if (hasEditButton(element)) {
                int[] editRect = editButtonRect(x, y, w);
                SolsticeTheme.fillRect(context, editRect[0], editRect[1], editRect[2], editRect[3], ColorPalette.BG_CARD);
                SolsticeTheme.drawBorder(context, editRect[0], editRect[1], editRect[2], editRect[3], ColorPalette.BORDER_DEFAULT);
                context.drawText(textRenderer, "Edit", editRect[0] + 4, editRect[1] + 2, ColorPalette.TEXT_PRIMARY, false);
            }

            int[] handleRect = resizeHandleRect(x, y, w, h);
            SolsticeTheme.fillRect(context, handleRect[0], handleRect[1], handleRect[2], handleRect[3],
                    resizing ? ColorPalette.ACCENT_NEUTRAL : ColorPalette.ACCENT_DIM);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        HudLayoutManager layout = HudLayoutManager.getInstance();
        int mx = (int) click.x();
        int my = (int) click.y();
        for (HudElement element : layout.getElements()) {
            String id = element.getId();
            int x = layout.getX(id, element.getDefaultX(width, height));
            int y = layout.getY(id, element.getDefaultY(width, height));
            int w = layout.getWidth(id, element.getWidth());
            int h = layout.getHeight(id, element.getHeight());

            if (hasVisibilityToggle(element) && contains(toggleRect(element, x, y, w), mx, my)) {
                boolean current = layout.isVisible(id, element.isVisibleByDefault());
                layout.setVisible(id, !current);
                return true;
            }
            if (hasEditButton(element) && contains(editButtonRect(x, y, w), mx, my)) {
                assert client != null;
                client.setScreen(new HudWidgetConfigScreen(this, element));
                return true;
            }
            if (contains(resizeHandleRect(x, y, w, h), mx, my)) {
                resizingElement = element;
                resizeStartMouseX = mx;
                resizeStartMouseY = my;
                resizeStartW = w;
                resizeStartH = h;
                liveW = w;
                liveH = h;
                return true;
            }
            if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
                draggingElement = element;
                dragOffsetX = mx - x;
                dragOffsetY = my - y;
                dragX = x;
                dragY = y;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (resizingElement != null) {
            int mx = (int) click.x();
            int my = (int) click.y();
            liveW = Math.max(40, resizeStartW + (mx - resizeStartMouseX));
            liveH = Math.max(10, resizeStartH + (my - resizeStartMouseY));
            return true;
        }
        if (draggingElement != null) {
            dragX = (int) click.x() - dragOffsetX;
            dragY = (int) click.y() - dragOffsetY;
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (resizingElement != null) {
            HudLayoutManager.getInstance().setSize(resizingElement.getId(), liveW, liveH);
            resizingElement = null;
            return true;
        }
        if (draggingElement != null) {
            HudLayoutManager.getInstance().setPosition(draggingElement.getId(), dragX, dragY);
            draggingElement = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    /**
     * Boss Bar's look is meant to always match vanilla's own, and Inventory HUD never
     * draws a background or any configurable text - only visibility and position/size
     * are configurable for either, not colors.
     */
    private boolean hasEditButton(HudElement element) {
        return !(element instanceof BossBarHudElement) && !(element instanceof InventoryHudElement);
    }

    /**
     * Inventory HUD's own module enable state is the one real on/off switch for it (see its
     * Javadoc) - a separate, independently-persisted "Visible" checkbox here would just be a
     * second flag that can silently go stale and keep it hidden with no way to tell why.
     */
    private boolean hasVisibilityToggle(HudElement element) {
        return !(element instanceof InventoryHudElement);
    }

    private int[] editButtonRect(int x, int y, int w) {
        return new int[]{x + w - EDIT_BUTTON_W, y - 12, EDIT_BUTTON_W, EDIT_BUTTON_H};
    }

    /** Sits just left of the Edit button, or takes its spot entirely on elements with no Edit button. */
    private int[] toggleRect(HudElement element, int x, int y, int w) {
        int right = hasEditButton(element) ? x + w - EDIT_BUTTON_W - 4 : x + w;
        return new int[]{right - TOGGLE_SIZE, y - 12, TOGGLE_SIZE, TOGGLE_SIZE};
    }

    private int[] resizeHandleRect(int x, int y, int w, int h) {
        return new int[]{x + w - RESIZE_HANDLE_SIZE, y + h - RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE};
    }

    private boolean contains(int[] rect, int px, int py) {
        return px >= rect[0] && px <= rect[0] + rect[2] && py >= rect[1] && py <= rect[1] + rect[3];
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // No shouldPause() override - see SolsticeScreen's identical note.
}

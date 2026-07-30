package com.example.solstice.qol.shulker;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a container item's tooltip as a real icon grid (item icons +
 * count/durability overlays), instead of vanilla's own plain-text "and N
 * more" listing - see {@code ContainerComponentAppendTooltipMixin} for how
 * that plain-text listing gets replaced with a one-line summary while this
 * preview is showing.
 *
 * <p>Two modes, matching MisterPeModder/ShulkerBoxTooltip's own Compact/
 * Full distinction: full mode shows every slot in its real position,
 * including empty ones, padded out to {@link ShulkerTooltipData#totalSlots()}
 * (not just however many entries the container's own stored list happens
 * to have - see that record's Javadoc for why); compact mode merges
 * identical items (matched by {@link ItemStack#areItemsAndComponentsEqual},
 * summing counts - a merged stack's count can validly exceed 64, {@code
 * drawStackOverlay} just stringifies whatever {@code getCount()} returns
 * either way) and skips empty slots entirely.</p>
 *
 * <p>The background is a single colored 9-slice panel (bundled directly
 * from the original mod, MIT, see NOTICE.md) tinted to the shulker box's
 * own dye color, matching the original mod's real look - not a per-slot
 * box behind every item, which the original mod itself only draws for a
 * hovered slot.</p>
 */
public class ShulkerTooltipComponent implements TooltipComponent {

    private static final Identifier BACKGROUND_SPRITE = Identifier.of("solstice", "shulker_box_tooltip");
    private static final int SLOTS_PER_ROW = 9;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_OFFSET = 8;
    private static final int PADDING = 14;

    private final List<ItemStack> contents;
    private final int backgroundColor;

    public ShulkerTooltipComponent(ShulkerTooltipData data) {
        this.backgroundColor = data.backgroundColor();
        this.contents = data.compact() ? mergeCompact(data.contents()) : padToSize(data.contents(), data.totalSlots());
    }

    private static List<ItemStack> mergeCompact(List<ItemStack> raw) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : raw) {
            if (stack.isEmpty()) {
                continue;
            }
            boolean found = false;
            for (ItemStack existing : merged) {
                if (ItemStack.areItemsAndComponentsEqual(existing, stack)) {
                    existing.increment(stack.getCount());
                    found = true;
                    break;
                }
            }
            if (!found) {
                merged.add(stack.copy());
            }
        }
        return merged;
    }

    private static List<ItemStack> padToSize(List<ItemStack> raw, int totalSlots) {
        if (raw.size() >= totalSlots) {
            return raw;
        }
        List<ItemStack> padded = new ArrayList<>(raw);
        while (padded.size() < totalSlots) {
            padded.add(ItemStack.EMPTY);
        }
        return padded;
    }

    private int cols() {
        return Math.min(SLOTS_PER_ROW, Math.max(1, contents.size()));
    }

    private int rows() {
        return (int) Math.ceil(Math.max(1, contents.size()) / (double) SLOTS_PER_ROW);
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return PADDING + cols() * SLOT_SIZE;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return PADDING + rows() * SLOT_SIZE;
    }

    /**
     * Hit-tests a real screen position against this grid's item slots -
     * used by {@code HandledScreenLockOverlayMixin} so the locked preview
     * can show a nested vanilla tooltip for whatever slot the mouse is
     * actually over, since the grid isn't backed by real {@code Slot}s
     * vanilla's own hover system would otherwise recognize.
     */
    public ItemStack getHoveredStack(int gridX, int gridY, int mouseX, int mouseY) {
        for (int i = 0; i < contents.size(); i++) {
            int col = i % SLOTS_PER_ROW;
            int row = i / SLOTS_PER_ROW;
            int slotX = gridX + SLOT_OFFSET + col * SLOT_SIZE;
            int slotY = gridY + SLOT_OFFSET + row * SLOT_SIZE;
            if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                return contents.get(i);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        // The width/height parameters here are the AGGREGATE size of the whole
        // tooltip (every text line plus this component combined), not this
        // component's own size - vanilla passes the same two values to every
        // component in the tooltip so each one COULD span the full tooltip if
        // it wanted to. Using them directly stretched this background panel
        // down through the space occupied by the "Contains N Stacks"/hint text
        // lines and beyond, and the 9-slice's tiled (non-stretched) interior
        // filled that extra height with repeating slot-look tiles - exactly
        // the "extra empty rows" bug reported in-game. Must use this
        // component's own real size instead.
        int panelWidth = getWidth(textRenderer);
        int panelHeight = getHeight(textRenderer);
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, x, y, panelWidth, panelHeight, backgroundColor);

        for (int i = 0; i < contents.size(); i++) {
            int col = i % SLOTS_PER_ROW;
            int row = i / SLOTS_PER_ROW;
            int slotX = x + SLOT_OFFSET + col * SLOT_SIZE;
            int slotY = y + SLOT_OFFSET + row * SLOT_SIZE;

            ItemStack stack = contents.get(i);
            if (!stack.isEmpty()) {
                context.drawItem(stack, slotX, slotY);
                context.drawStackOverlay(textRenderer, stack, slotX, slotY);
            }
        }
    }
}

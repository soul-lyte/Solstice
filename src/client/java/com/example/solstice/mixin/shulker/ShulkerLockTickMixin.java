package com.example.solstice.mixin.shulker;

import com.example.solstice.qol.shulker.ShulkerBoxTooltipModule;
import com.example.solstice.qol.shulker.ShulkerTooltipComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Ticks the shulker lock state and draws the frozen preview - deliberately
 * hooked on {@code Screen.renderWithTooltip}'s TAIL, not {@code
 * HandledScreen.render}'s (where this used to live). Confirmed via decompile:
 * {@code renderWithTooltip} calls {@code render(...)} first, then {@code
 * DrawContext.drawDeferredElements()} - and that deferred flush is what
 * actually invokes {@code Item.getTooltipData()} for the hovered stack (the
 * real vanilla tooltip system defers its draw, it doesn't run it inline
 * inside {@code render()}). {@code ShulkerTooltipDataMixin}'s {@code
 * recordFullPreviewFrame} call - the thing that sets {@code
 * ShulkerBoxTooltipModule}'s "armed this frame" flag - only ever fires
 * during that deferred flush. Ticking from {@code HandledScreen.render}'s
 * own TAIL, which runs *before* the deferred flush for the very same frame,
 * meant every read of that flag was one frame stale: real, observed
 * symptoms were the lock sometimes not triggering at all on a quick release
 * (the stale read landed on the wrong side of a fast key-then-mouse
 * transition) and the frozen box appearing at a slightly different position
 * than the live preview had actually been (the stale frame's mouse
 * coordinate no longer matched). Hooking after the deferred flush instead
 * means {@link ShulkerBoxTooltipModule#tickLockState} always sees this exact
 * frame's real arm state.
 *
 * <p>Only acts on {@code HandledScreen} instances (player inventory, chests,
 * shulker boxes, creative inventory, ...) - {@code renderWithTooltip} is
 * called for every {@code Screen} each frame, but a container item's
 * tooltip can only ever have been showing on one of these.</p>
 */
@Mixin(Screen.class)
public abstract class ShulkerLockTickMixin {

    @Inject(method = "renderWithTooltip", at = @At("TAIL"), require = 1)
    private void solstice$tickAndDrawLockedShulkerPreview(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!(((Object) this) instanceof HandledScreen<?>)) return;

        ShulkerBoxTooltipModule module = ShulkerBoxTooltipModule.getInstance();
        if (!module.isEnabled()) return;

        module.tickLockState(this, mouseX, mouseY);
        module.getLockedPreview().ifPresent(locked -> {
            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer textRenderer = client.textRenderer;

            List<Text> lines = Screen.getTooltipFromItem(client, locked.stack());
            ShulkerTooltipComponent grid = new ShulkerTooltipComponent(locked.data());

            List<TooltipComponent> components = new ArrayList<>(lines.size() + 1);
            for (Text line : lines) {
                components.add(TooltipComponent.of(line.asOrderedText()));
            }
            components.add(grid);

            context.drawTooltipImmediately(textRenderer, components, locked.x(), locked.y(),
                    HoveredTooltipPositioner.INSTANCE, null);

            // Independently re-derive the same box position drawTooltipImmediately
            // just computed (same positioner, same inputs - confirmed via decompile
            // this is exactly what it does internally) so the grid's own real
            // on-screen rect is known here too, for the nested per-slot tooltip below.
            int totalWidth = 0;
            int totalHeight = 0;
            for (TooltipComponent component : components) {
                totalWidth = Math.max(totalWidth, component.getWidth(textRenderer));
                totalHeight += component.getHeight(textRenderer);
            }
            Vector2ic boxPos = HoveredTooltipPositioner.INSTANCE.getPosition(
                    client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(),
                    locked.x(), locked.y(), totalWidth, totalHeight);

            int gridX = boxPos.x();
            int gridY = boxPos.y();
            for (int i = 0; i < components.size() - 1; i++) {
                // Matches vanilla's own draw-loop exactly: the first line (the item's
                // own name) gets an extra 2px gap below it that no other line does.
                gridY += components.get(i).getHeight(textRenderer) + (i == 0 ? 2 : 0);
            }

            ItemStack hovered = grid.getHoveredStack(gridX, gridY, mouseX, mouseY);
            if (!hovered.isEmpty()) {
                context.drawItemTooltip(textRenderer, hovered, mouseX, mouseY);
            }
        });
    }
}

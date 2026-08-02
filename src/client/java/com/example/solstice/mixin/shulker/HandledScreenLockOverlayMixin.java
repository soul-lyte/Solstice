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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the locked shulker preview (see {@link ShulkerBoxTooltipModule#tickLockState})
 * independent of whatever the mouse currently hovers - the whole point of
 * locking is that the mouse can move away while the preview stays put.
 * {@code HandledScreen} covers every screen with real item slots (player
 * inventory, chests, shulker boxes, creative inventory), which is the only
 * place a container item's tooltip could have been showing in the first
 * place.
 *
 * <p>Draws the full tooltip (item name + the "Contains N Stacks"/hint text
 * lines, same as the live hover preview) via the same real code path
 * vanilla's own {@code DrawContext.drawTooltip(TextRenderer, List, Optional,
 * int, int)} funnels into ({@code drawTooltipImmediately} with {@code
 * HoveredTooltipPositioner.INSTANCE}, confirmed via decompile) instead of a
 * hand-rolled component-only draw with a guessed offset - the earlier
 * version only drew the icon grid at a flat {@code mouse+12,+12}, which
 * didn't account for the name/summary/hint lines stacked above the grid in
 * the live preview, so the locked box appeared ~20px higher than where the
 * live one had been.</p>
 *
 * <p>Also blocks every mouse input method on the whole screen while a
 * locked preview is showing (not just within the overlay's own bounds) -
 * per explicit request that the locked preview sit "on top of everything
 * else": before this, nothing here ever intercepted clicks at all, so
 * clicking anywhere - including through the overlay's own empty grid
 * cells - always reached the real inventory slot underneath. Releasing the
 * Full Preview Key (which also unlocks, see the module) restores normal
 * interaction immediately.</p>
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenLockOverlayMixin {

    @Inject(method = "render", at = @At("TAIL"), require = 1)
    private void solstice$drawLockedShulkerPreview(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
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

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 1)
    private void solstice$blockClickWhileLocked(CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerBoxTooltipModule.getInstance().getLockedPreview().isPresent()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true, require = 1)
    private void solstice$blockDragWhileLocked(CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerBoxTooltipModule.getInstance().getLockedPreview().isPresent()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, require = 1)
    private void solstice$blockReleaseWhileLocked(CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerBoxTooltipModule.getInstance().getLockedPreview().isPresent()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 1)
    private void solstice$blockScrollWhileLocked(CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerBoxTooltipModule.getInstance().getLockedPreview().isPresent()) {
            cir.setReturnValue(true);
        }
    }
}

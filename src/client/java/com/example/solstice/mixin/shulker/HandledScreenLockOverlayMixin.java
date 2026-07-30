package com.example.solstice.mixin.shulker;

import com.example.solstice.qol.shulker.ShulkerBoxTooltipModule;
import com.example.solstice.qol.shulker.ShulkerTooltipComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Draws the locked shulker preview (see {@link ShulkerBoxTooltipModule#tickLockState})
 * independent of whatever the mouse currently hovers - the whole point of
 * locking is that the mouse can move away while the preview stays put.
 * {@code HandledScreen} covers every screen with real item slots (player
 * inventory, chests, shulker boxes, creative inventory), which is the only
 * place a container item's tooltip could have been showing in the first
 * place.
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
            ShulkerTooltipComponent component = new ShulkerTooltipComponent(locked.data());
            int gridX = locked.x() + 12;
            int gridY = locked.y() + 12;
            context.drawTooltipImmediately(client.textRenderer, List.of(component), locked.x(), locked.y(),
                    (screenWidth, screenHeight, x, y, width, height) -> new Vector2i(x + 12, y + 12), null);

            ItemStack hovered = component.getHoveredStack(gridX, gridY, mouseX, mouseY);
            if (!hovered.isEmpty()) {
                context.drawItemTooltip(client.textRenderer, hovered, mouseX, mouseY);
            }
        });
    }
}

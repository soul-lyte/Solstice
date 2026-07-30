package com.example.solstice.mixin.ui;

import com.example.solstice.ui.SolsticeVideoSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opens {@link SolsticeVideoSettingsScreen} instead of vanilla's real
 * {@code VideoOptionsScreen} whenever the game tries to open one - except
 * for the one legitimate case, Solstice's own "More Options..." button,
 * which sets {@link SolsticeVideoSettingsScreen#allowVanillaScreen} around
 * its own direct construction of the real screen so it isn't redirected
 * right back to itself.
 *
 * <p>Hooks {@code MinecraftClient.setScreen} globally rather than the
 * specific button/lambda inside vanilla's {@code OptionsScreen} that
 * constructs {@code VideoOptionsScreen} - that lambda is a synthetic,
 * unnamed method whose exact identity isn't reliably pinned down without
 * decompiled source, while {@code setScreen} is a simple, already
 * extensively-used, stable target in this project.</p>
 */
@Mixin(MinecraftClient.class)
public abstract class VideoOptionsRedirectMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void solstice$redirectVideoOptions(Screen screen, CallbackInfo ci) {
        if (!(screen instanceof VideoOptionsScreen) || SolsticeVideoSettingsScreen.allowVanillaScreen) return;

        MinecraftClient self = (MinecraftClient) (Object) this;
        Screen parent = self.currentScreen;
        ci.cancel();
        self.setScreen(new SolsticeVideoSettingsScreen(parent));
    }
}

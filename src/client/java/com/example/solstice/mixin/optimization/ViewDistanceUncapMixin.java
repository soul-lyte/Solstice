package com.example.solstice.mixin.optimization;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Adapted from C2ME's {@code c2me-client-uncapvd} {@code MixinGameOptions} -
 * RelativityMC, MIT, see NOTICE.md. Vanilla's render-distance slider caps at
 * 32 chunks (16 on low-memory systems - confirmed via decompile:
 * {@code new ValidatingIntSliderCallbacks(2, hasEnoughMemory ? 32 : 16, false)}
 * inside {@code GameOptions}'s own constructor). Widened here to a flat 64
 * regardless of memory tier, matching this mod's own performance-first
 * framing - a player on a low-memory machine simply won't drag the slider
 * that high, no forced cap needed.
 *
 * <p>Deliberately does NOT port C2ME's companion networking piece
 * ({@code sendClientSettings}/{@code ClientExtNetworking}, which announces
 * the uncapped value to a C2ME-aware server) - real benefit here is
 * singleplayer/self-hosted only, per the same scoping already applied to
 * the rest of this C2ME batch. A remote server that doesn't
 * run C2ME's own server-side counterpart would never honor an extended
 * value anyway, so that piece would be dead weight for Solstice's actual
 * use case.</p>
 *
 * <p>Preserves vanilla's own {@code applyValueImmediately=false} (C2ME's
 * source uses the 2-arg {@code ValidatingIntSliderCallbacks} constructor,
 * which defaults that to {@code true} - a real, if minor, behavior change
 * from vanilla's current slider feel that isn't part of what this port is
 * for).</p>
 */
@Mixin(GameOptions.class)
public abstract class ViewDistanceUncapMixin {

    private static final int MAX_VIEW_DISTANCE = 64;

    @Shadow
    @Final
    private SimpleOption<Integer> viewDistance;

    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/GameOptions;load()V", shift = At.Shift.BEFORE))
    private void solstice$uncapViewDistance(MinecraftClient client, File optionsFile, CallbackInfo ci) {
        SimpleOption.ValidatingIntSliderCallbacks callbacks =
                new SimpleOption.ValidatingIntSliderCallbacks(2, MAX_VIEW_DISTANCE, false);
        SimpleOptionAccessor accessor = (SimpleOptionAccessor) (Object) this.viewDistance;
        accessor.solstice$setCallbacks(callbacks);
        accessor.solstice$setCodec(callbacks.codec());
    }
}

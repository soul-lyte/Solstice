package com.example.solstice.mixin.viewdistance;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code ClientWorld}'s private {@code networkHandler} field, needed
 * by {@link com.example.solstice.viewdistance.FakeChunkManager} to derive a
 * per-server chunk-cache folder name. Ported from Johni0702/bobby's
 * equivalent accessor (LGPL-3.0-only, see NOTICE.md).
 */
@Mixin(ClientWorld.class)
public interface ClientWorldAccessor {
    @Accessor("networkHandler")
    ClientPlayNetworkHandler solstice$getNetworkHandler();
}

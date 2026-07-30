package com.example.solstice.mixin.viewdistance;

import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md). View distance
 * is announced to a real (non-integrated) server as a single byte - a
 * defensive clamp so a heavily-raised render distance can never overflow it
 * when actually connecting to a remote server.
 */
@Mixin(SyncedClientOptions.class)
public abstract class ClientSettingsC2SPacketMixin {
    @ModifyArg(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;writeByte(I)Lnet/minecraft/network/PacketByteBuf;", ordinal = 0))
    private int solstice$clampMaxValue(int viewDistance) {
        return Math.min(viewDistance, Byte.MAX_VALUE);
    }
}

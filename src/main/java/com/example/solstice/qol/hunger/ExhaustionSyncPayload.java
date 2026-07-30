package com.example.solstice.qol.hunger;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-client sync of the local player's real exhaustion value.
 *
 * <p>Vanilla never networks {@code HungerManager}'s exhaustion field to the
 * client at all (it's a server-only implementation detail) - the client and
 * integrated/remote server each hold their own separate {@code PlayerEntity},
 * so without this, the client-side hunger overlay has no way to know the
 * real value. Only sent when it's changed by a meaningful amount, matching
 * AppleSkin's own threshold.</p>
 */
public record ExhaustionSyncPayload(float exhaustion) implements CustomPayload {

    public static final CustomPayload.Id<ExhaustionSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of("solstice", "exhaustion_sync"));

    public static final PacketCodec<RegistryByteBuf, ExhaustionSyncPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.FLOAT, ExhaustionSyncPayload::exhaustion, ExhaustionSyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

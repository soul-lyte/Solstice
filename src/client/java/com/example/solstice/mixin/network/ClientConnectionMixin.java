package com.example.solstice.mixin.network;

import com.example.solstice.performance.network.NetworkModule;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies socket-level transport tuning when a connection is established.
 * No packet content is altered - purely OS-level buffer and latency settings.
 */
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Shadow private Channel channel;

    @Inject(method = "setCompressionThreshold", at = @At("RETURN"))
    private void solstice$tuneSocket(int compressionThreshold, boolean rejectBad, CallbackInfo ci) {
        applySocketOptions();
    }

    private void applySocketOptions() {
        if (channel == null || !NetworkModule.getInstance().isEnabled()) return;

        if (NetworkModule.tcpNoDelay) {
            channel.config().setOption(ChannelOption.TCP_NODELAY, true);
        }
        int snd = NetworkModule.sendBufferBytes;
        int rcv = NetworkModule.receiveBufferBytes;
        if (snd > 0) channel.config().setOption(ChannelOption.SO_SNDBUF, snd);
        if (rcv > 0) channel.config().setOption(ChannelOption.SO_RCVBUF, rcv);
    }
}

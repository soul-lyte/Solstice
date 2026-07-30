package com.example.solstice.mixin.network;

import com.example.solstice.performance.network.NetworkModule;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.AbstractEventExecutor;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies socket-level transport tuning when a connection is established
 * (no packet content is altered - purely OS-level buffer and latency
 * settings), plus two flush-related micro-optimizations ported from
 * RelativityMC/VMP-fabric's own {@code no_flush} mixin (MIT, see NOTICE.md):
 * skip the redundant {@code Channel.flush()} call {@code tick()} makes every
 * single tick regardless of whether anything is actually pending (Netty's
 * own {@code writeAndFlush}-driven writes already flush themselves when
 * there's real data to send), and avoid waking the event loop for a
 * non-flushing send when a lazy submission is available instead. Both are
 * pure I/O scheduling changes - never touch packet content, ordering, or
 * timing semantics the server can observe.
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

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lio/netty/channel/Channel;flush()Lio/netty/channel/Channel;"))
    private Channel solstice$skipRedundantTickFlush(Channel instance) {
        if (!NetworkModule.getInstance().isEnabled()) {
            return instance.flush();
        }
        return instance; // no-op - nothing forces a flush here that Netty wouldn't already do on its own
    }

    @Redirect(method = "sendImmediately", at = @At(value = "INVOKE", target = "Lio/netty/channel/EventLoop;execute(Ljava/lang/Runnable;)V"))
    private void solstice$avoidUnnecessaryEventLoopWakeup(EventLoop instance, Runnable task,
                                                           Packet<?> packet, ChannelFutureListener callback, boolean flush) {
        if (NetworkModule.getInstance().isEnabled() && !flush && instance instanceof AbstractEventExecutor executor) {
            executor.lazyExecute(task);
        } else {
            instance.execute(task);
        }
    }
}

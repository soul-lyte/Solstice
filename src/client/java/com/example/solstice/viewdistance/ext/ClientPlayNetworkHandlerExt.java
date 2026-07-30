package com.example.solstice.viewdistance.ext;

import net.minecraft.client.network.ClientPlayNetworkHandler;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md), implemented by
 * {@code ClientPlayNetworkHandlerMixin}. Queues clearing a fake chunk's
 * shadow light data until after a real replacement chunk's own light has
 * fully processed, avoiding a one-frame flicker back to unlit.
 */
public interface ClientPlayNetworkHandlerExt {
    void solstice$queueUnloadFakeLightDataTask(Runnable runnable);

    static ClientPlayNetworkHandlerExt get(ClientPlayNetworkHandler handler) {
        return (ClientPlayNetworkHandlerExt) handler;
    }
}

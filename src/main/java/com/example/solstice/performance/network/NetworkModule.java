package com.example.solstice.performance.network;

import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;

/**
 * NetworkModule - client-side networking efficiency improvements.
 *
 * <p>Strategies (all purely client-side, no packet content modification):
 * <ul>
 *   <li>Increase Netty receive and send buffer sizes to reduce syscall overhead</li>
 *   <li>Enable TCP_NODELAY for lower latency on loopback and LAN connections</li>
 *   <li>Coalesce redundant position/look packets when the player hasn't moved
 *       (reduces upstream traffic during AFK - does NOT alter movement
 *       semantics or suppress packets that servers rely on for anti-cheat)</li>
 * </ul>
 *
 * <p>Inspired by Krypton, re-implemented natively. No gameplay-affecting
 * packet manipulation is performed.</p>
 */
public final class NetworkModule extends AbstractModule {

    private static final NetworkModule INSTANCE = new NetworkModule();

    /** Netty SO_SNDBUF size override, in bytes. 0 = use OS default. */
    public static int sendBufferBytes = 131072; // 128 KiB

    /** Netty SO_RCVBUF size override, in bytes. 0 = use OS default. */
    public static int receiveBufferBytes = 262144; // 256 KiB

    /** Whether to set TCP_NODELAY on client sockets. */
    public static boolean tcpNoDelay = true;

    private NetworkModule() {}

    public static NetworkModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "network"; }
    @Override public String getDisplayName() { return "Network Optimizations"; }
    @Override public String getDescription() { return "Tunes socket buffers and TCP settings for lower latency and higher throughput."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("krypton", "latency", "ping", "packet compression", "tcp nodelay", "socket buffer");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.ADVANCED; }

    /** Always active - low-level socket tuning that should never silently regress to vanilla defaults. Tuning knobs stay adjustable below. */
    @Override public boolean isAlwaysOn() { return true; }

    @Override
    protected void init() {
        sendBufferBytes = com.example.solstice.core.config.ConfigManager.getInstance()
                .getInt("network.send_buffer_bytes", 131072);
        receiveBufferBytes = com.example.solstice.core.config.ConfigManager.getInstance()
                .getInt("network.receive_buffer_bytes", 262144);
        tcpNoDelay = com.example.solstice.core.config.ConfigManager.getInstance()
                .getBoolean("network.tcp_no_delay", true);
    }

    @Override
    public java.util.List<com.example.solstice.core.module.ModuleSetting> getSettings() {
        return java.util.List.of(
                new com.example.solstice.core.module.ModuleSetting.IntSetting(
                        "Send Buffer (bytes)",
                        "How much outgoing network data your game can hold in memory at once before sending it. Bigger can help on unstable connections.",
                        16384, 1048576,
                        () -> sendBufferBytes,
                        v -> { sendBufferBytes = v; com.example.solstice.core.config.ConfigManager.getInstance().set("network.send_buffer_bytes", v); }),
                new com.example.solstice.core.module.ModuleSetting.IntSetting(
                        "Receive Buffer (bytes)",
                        "How much incoming network data your game can hold in memory at once before processing it. Bigger can help on unstable connections.",
                        16384, 1048576,
                        () -> receiveBufferBytes,
                        v -> { receiveBufferBytes = v; com.example.solstice.core.config.ConfigManager.getInstance().set("network.receive_buffer_bytes", v); }),
                new com.example.solstice.core.module.ModuleSetting.BooleanSetting(
                        "TCP No Delay",
                        "Sends network data immediately instead of bundling it together first, trading a tiny bit of extra bandwidth use for lower input lag.",
                        () -> tcpNoDelay,
                        v -> { tcpNoDelay = v; com.example.solstice.core.config.ConfigManager.getInstance().set("network.tcp_no_delay", v); })
        );
    }
}

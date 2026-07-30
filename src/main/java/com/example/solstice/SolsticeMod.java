package com.example.solstice;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.ModuleRegistry;
import com.example.solstice.mixin.hunger.HungerManagerAccessor;
import com.example.solstice.performance.memory.MemoryModule;
import com.example.solstice.performance.network.NetworkModule;
import com.example.solstice.performance.startup.StartupModule;
import com.example.solstice.qol.hunger.ExhaustionSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Solstice - common-side initializer.
 *
 * <p>Only registers modules that have no client-only dependencies.
 * Client-only modules (rendering, UI) are registered in
 * {@code SolsticeClient} which lives in src/client/java.</p>
 */
public class SolsticeMod implements ModInitializer {

    public static final String MOD_ID   = "solstice";
    public static final String MOD_NAME = "Solstice";
    public static final String VERSION  = "1.2.6";
    public static final Logger LOGGER   = LoggerFactory.getLogger(MOD_ID);

    private static SolsticeMod instance;

    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("[{}] Initializing v{}", MOD_NAME, VERSION);

        // Config must be loaded first - modules read it in their constructors
        ConfigManager.getInstance().load();

        // Register common-side modules (no net.minecraft.client.* imports).
        // Note: ChunkModule is intentionally NOT registered - its build-
        // priority logic was never actually wired to a real chunk-builder
        // hook (both candidate Mixins were placeholders/removed; see its
        // class Javadoc), so exposing it as a settings toggle would mislead
        // users into thinking it's doing something. The class remains in
        // the codebase for future work.
        ModuleRegistry registry = ModuleRegistry.getInstance();
        registry.register(MemoryModule.getInstance());
        registry.register(NetworkModule.getInstance());
        registry.register(StartupModule.getInstance());

        // Sync each player's real exhaustion to their own client - vanilla never
        // networks this value at all (see ExhaustionSyncPayload Javadoc).
        PayloadTypeRegistry.playS2C().register(ExhaustionSyncPayload.ID, ExhaustionSyncPayload.CODEC);
        Map<UUID, Float> lastSentExhaustion = new HashMap<>();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                float exhaustion = ((HungerManagerAccessor) player.getHungerManager()).solstice$getExhaustion();
                float last = lastSentExhaustion.getOrDefault(player.getUuid(), -1f);
                if (Math.abs(exhaustion - last) >= 0.01f) {
                    ServerPlayNetworking.send(player, new ExhaustionSyncPayload(exhaustion));
                    lastSentExhaustion.put(player.getUuid(), exhaustion);
                }
            }
        });

        LOGGER.info("[{}] Common initialization complete ({} modules so far).",
                MOD_NAME, registry.getModuleCount());
    }

    public static SolsticeMod getInstance() {
        return instance;
    }
}

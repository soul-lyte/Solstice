package com.example.solstice;

import com.example.solstice.core.event.EventBus;
import com.example.solstice.core.event.events.ClientTickEvent;
import com.example.solstice.core.hud.HudLayoutManager;
import com.example.solstice.core.module.ModuleRegistry;
import com.example.solstice.mixin.hunger.HungerManagerAccessor;
import com.example.solstice.performance.render.EntityCullingModule;
import com.example.solstice.performance.render.ParticleLimiterModule;
import com.example.solstice.performance.render.RenderModule;
import com.example.solstice.qol.crosshair.CrosshairModule;
import com.example.solstice.qol.armor.ArmorHudModule;
import com.example.solstice.qol.chatheads.ChatHeadsModule;
import com.example.solstice.qol.combathitbox.CombatHitboxModule;
import com.example.solstice.qol.combathitbox.CombatHitboxRenderer;
import com.example.solstice.qol.inventoryhud.InventoryHudModule;
import com.example.solstice.qol.locatorheads.LocatorHeadsModule;
import com.example.solstice.qol.shulker.ShulkerBoxTooltipModule;
import com.example.solstice.qol.effects.FullbrightModule;
import com.example.solstice.qol.effects.StatusEffectTimerModule;
import com.example.solstice.qol.effects.StatusEffectVisionModule;
import com.example.solstice.qol.hunger.ExhaustionSyncPayload;
import com.example.solstice.qol.hunger.HungerHudModule;
import com.example.solstice.qol.viewmodel.ViewModelModule;
import com.example.solstice.qol.visuals.VisualsModule;
import com.example.solstice.qol.zoom.ZoomModule;
import com.example.solstice.textures.TexturePackManager;
import com.example.solstice.textures.TexturePresetManager;
import com.example.solstice.textures.TextureSlots;
import com.example.solstice.ui.BossBarHudElement;
import com.example.solstice.ui.BowCrossbowIndicatorWidget;
import com.example.solstice.ui.FpsWidget;
import com.example.solstice.ui.RamWidget;
import com.example.solstice.ui.GuiScaleOverride;
import com.example.solstice.ui.ScoreboardHudElement;
import com.example.solstice.ui.SolsticeScreen;
import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.InventoryHudElement;
import com.example.solstice.ui.WatermarkHudElement;
import com.example.solstice.viewdistance.ViewDistanceModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Solstice client-side initializer.
 * Lives in src/client/java - may freely use net.minecraft.client.*.
 */
public class SolsticeClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("solstice-client");

    private static SolsticeClient instance;

    /** Keybinding to open the Solstice settings screen (default: Right Shift). */
    public static KeyBinding openGuiKey;

    /** Hold-to-zoom keybinding for {@link ZoomModule} (default: C). */
    public static KeyBinding zoomKey;

    // Shulker Box Tooltip's three hold-keys are deliberately NOT real vanilla
    // KeyBindings - see ShulkerBoxTooltipModule's own Javadoc for why (keeping
    // them out of vanilla's Controls screen). The module manages its own raw
    // GLFW keycodes internally; nothing to register here.

    /**
     * Category grouping for Solstice's keybindings, registered once.
     * Since Minecraft 1.21.9, KeyBinding no longer accepts a raw translation-key
     * String for its category - it requires a registered KeyBinding.Category.
     */
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("solstice", "general"));

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("[Solstice] Client systems starting...");

        // Must run before the first resource load - registers every bundled texture-pack
        // option as a Fabric builtin resource pack. Doesn't touch MinecraftClient.getInstance()
        // (safe this early - the singleton isn't guaranteed to exist yet at this point in
        // Fabric's bootstrap). Applying the persisted selection needs the resource pack
        // manager, so that part waits for ClientLifecycleEvents.CLIENT_STARTED below.
        TexturePackManager.getInstance().registerBuiltinPacks(TextureSlots.ALL);
        TexturePackManager.getInstance().registerBuiltinPack("preset_tiny_tools");
        TexturePackManager.getInstance().registerBuiltinPack("preset_vanillaplus");
        TexturePackManager.getInstance().registerBuiltinPack("preset_tournament");
        TexturePackManager.getInstance().registerBuiltinPack("preset_smpessentials");
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            // Both sources apply their enable/disable calls first, then ONE reload covers
            // both - each used to reload on its own, so two separate (and fully redundant)
            // async resource-manager rebuilds could run back to back. Deliberately still
            // async, NOT blocked on: an earlier attempt at blocking with
            // reloadResources().join() here deadlocked the render thread solid - completing
            // a reload apparently requires the client's own main loop to keep pumping
            // (confirmed by testing, not assumed), and CLIENT_STARTED's callback runs
            // before that loop starts, so nothing could ever finish the join(). Async
            // still means the title screen can flash vanilla textures for a moment before
            // this lands, but that's a real, much smaller cost than hanging the game.
            boolean texturesChanged = TexturePackManager.getInstance().applyPersistedSelections(TextureSlots.ALL);
            boolean presetChanged = TexturePresetManager.getInstance().applyPersistedSelection();
            boolean visualsChanged = VisualsModule.getInstance().applyPersistedTexturePacks();
            if (texturesChanged || presetChanged || visualsChanged) {
                TexturePackManager.getInstance().reload();
            }
        });

        // Register keybindings
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.solstice.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.solstice.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                CATEGORY
        ));
        ZoomModule.getInstance().bindKeybinding(zoomKey);

        // Register client-only modules (render modules use MinecraftClient).
        ModuleRegistry registry = ModuleRegistry.getInstance();
        registry.register(RenderModule.getInstance());
        registry.register(ParticleLimiterModule.getInstance());
        registry.register(EntityCullingModule.getInstance());
        registry.register(HungerHudModule.getInstance());
        registry.register(CrosshairModule.getInstance());
        registry.register(ZoomModule.getInstance());
        registry.register(VisualsModule.getInstance());
        registry.register(ViewModelModule.getInstance());
        registry.register(StatusEffectVisionModule.getInstance());
        registry.register(StatusEffectTimerModule.getInstance());
        registry.register(FullbrightModule.getInstance());
        registry.register(ArmorHudModule.getInstance());
        registry.register(ShulkerBoxTooltipModule.getInstance());
        registry.register(ChatHeadsModule.getInstance());
        registry.register(LocatorHeadsModule.getInstance());
        registry.register(InventoryHudModule.getInstance());
        registry.register(CombatHitboxModule.getInstance());
        registry.register(ViewDistanceModule.getInstance());

        // Initialize all modules (both common ones registered earlier + client ones just added)
        registry.getAllModules().forEach(m -> m.onClientInit());

        SolsticeSounds.init();

        // Register HUD widgets
        HudRenderCallback.EVENT.register(FpsWidget.getInstance()::onHudRender);
        HudRenderCallback.EVENT.register(RamWidget.getInstance()::onHudRender);
        HudRenderCallback.EVENT.register(BowCrossbowIndicatorWidget.getInstance()::onHudRender);
        HudRenderCallback.EVENT.register(BossBarHudElement.getInstance()::onHudRender);
        HudRenderCallback.EVENT.register(StatusEffectTimerModule.getInstance()::onHudRender);
        HudRenderCallback.EVENT.register(WatermarkHudElement.getInstance()::onHudRender);
        HudRenderCallback.EVENT.register(InventoryHudElement.getInstance()::onHudRender);
        HudLayoutManager.getInstance().register(FpsWidget.getInstance());
        HudLayoutManager.getInstance().register(RamWidget.getInstance());
        HudLayoutManager.getInstance().register(BossBarHudElement.getInstance());
        HudLayoutManager.getInstance().register(ScoreboardHudElement.getInstance());
        HudLayoutManager.getInstance().register(WatermarkHudElement.getInstance());
        HudLayoutManager.getInstance().register(InventoryHudElement.getInstance());

        CombatHitboxRenderer.register();

        // Receive each player's real exhaustion value from the server (see ExhaustionSyncPayload Javadoc)
        // and write it straight into the local player's own HungerManager via the accessor.
        ClientPlayNetworking.registerGlobalReceiver(ExhaustionSyncPayload.ID, (payload, context) -> {
            ClientPlayerEntity player = context.player();
            ((HungerManagerAccessor) player.getHungerManager()).solstice$setExhaustion(payload.exhaustion());
        });

        // Tick: open GUI on keybind press, feed zoom key state, fire tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                SolsticeSounds.playClick();
                client.setScreen(new SolsticeScreen(client.currentScreen));
            }
            ZoomModule.getInstance().setKeyHeld(zoomKey.isPressed());
            GuiScaleOverride.tick(client);
            EventBus.getInstance().fire(new ClientTickEvent(client));
        });

        LOGGER.info("[Solstice] Client initialization complete. {} total modules loaded.",
                registry.getModuleCount());
    }

    public static SolsticeClient getInstance() {
        return instance;
    }
}

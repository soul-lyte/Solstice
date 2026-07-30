package com.example.solstice.performance.startup;

import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.SolsticeMod;

/**
 * StartupModule - reduces game launch time.
 *
 * <p>Active strategies:
 * <ul>
 *   <li>Skip the splash overlay's 2-second fade-out once loading finishes
 *       (see {@code SplashScreenMixin}) - adapted from quick-pack's own
 *       {@code removeFadeOut} technique (MIT, see NOTICE.md), retargeted
 *       onto 1.21.11's real timing logic. Confirmed via decompile that
 *       {@code SplashOverlay} has no separate "ready to fade out" method
 *       to redirect (unlike the reference mod's own Mojmap-mapped
 *       version) - completion is an inline {@code render()} local
 *       (an internal 2-second countdown starting once {@code
 *       reloadCompleteTime} is set in {@code tick()}), so this instead
 *       calls {@code MinecraftClient.setOverlay(null)} itself the moment
 *       {@code tick()} first observes the reload/load has completed,
 *       skipping that countdown entirely.</li>
 * </ul>
 *
 * <p><b>Real bug fixed alongside this</b>: this module used to expose a
 * "Splash Screen Min Duration" setting ({@code splashMinMs}) that had
 * never actually been wired to any Mixin - {@code SplashScreenMixin}
 * (referenced by this class's own Javadoc and {@link #onSplashComplete()})
 * did not exist anywhere in the project, confirmed by a full source
 * search. The setting was pure dead UI, doing nothing. Removed rather
 * than kept as a placeholder (unlike {@link #lazyDfu}, which is a
 * deliberate, documented reserved flag) - it was never intended to be
 * non-functional, just never finished.</p>
 *
 * <p><b>Not currently implemented:</b> a LazyDFU-style deferral of the
 * DataFixerUpper bootstrap. An earlier draft attempted this by mixing into
 * a guessed {@code DataFixerBuilder.buildUnoptimized()} method that does
 * not exist on the real API (the actual class is
 * {@code com.mojang.datafixers.DataFixerBuilder}, built via a package-private
 * static field in {@code net.minecraft.datafixer.Schemas}). Because the DFU
 * is exercised on every legacy-world load, a mistaken Mixin here risks
 * silently corrupting world-upgrade behavior rather than just failing to
 * compile - that's a correctness risk this project won't take on a
 * best-guess implementation. The {@link #lazyDfu} flag is kept as a
 * reserved config key for a future, properly-verified implementation and
 * currently has no effect.</p>
 */
public final class StartupModule extends AbstractModule {

    private static final StartupModule INSTANCE = new StartupModule();

    /**
     * Reserved for a future LazyDFU-equivalent implementation.
     * Currently has NO effect - see class Javadoc.
     */
    public static boolean lazyDfu = true;

    /** Skip the splash overlay's ~2-second fade-out once loading completes. */
    public static boolean skipSplashFadeOut = true;

    private long startupBeginNs;
    private long startupEndNs;

    private StartupModule() {}

    public static StartupModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "startup"; }
    @Override public String getDisplayName() { return "Startup Optimizations"; }
    @Override public String getDescription() { return "Reduces splash-screen minimum display time to shorten time-to-main-menu."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("lazydfu", "fast startup", "splash screen", "boot time", "faster launch");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.ADVANCED; }

    /** Always active - see class Javadoc. Ignores persisted config and UI toggles. */
    @Override public boolean isEnabled() { return true; }
    @Override public void setEnabled(boolean enabled) { /* always on - no-op */ }
    @Override public boolean isAlwaysOn() { return true; }

    /**
     * {@code lazyDfu} is deliberately NOT exposed here - it currently has no
     * effect (see class Javadoc) and showing it as a control would mislead
     * the user the same way an {@code EntityCullingModule}/{@code ChunkModule}
     * toggle would.
     */
    @Override
    public java.util.List<com.example.solstice.core.module.ModuleSetting> getSettings() {
        return java.util.List.of(
                new com.example.solstice.core.module.ModuleSetting.BooleanSetting(
                        "Skip Splash Fade-Out",
                        "Closes the Minecraft splash screen immediately once loading finishes, instead of waiting through its usual fade-out.",
                        () -> skipSplashFadeOut,
                        v -> { skipSplashFadeOut = v; com.example.solstice.core.config.ConfigManager.getInstance().set("startup.skip_splash_fade_out", v); })
        );
    }

    @Override
    protected void init() {
        lazyDfu = com.example.solstice.core.config.ConfigManager.getInstance()
                .getBoolean("startup.lazy_dfu", true);
        skipSplashFadeOut = com.example.solstice.core.config.ConfigManager.getInstance()
                .getBoolean("startup.skip_splash_fade_out", true);
        startupBeginNs = System.nanoTime();
    }

    /** Called by {@code SplashScreenMixin} the moment the splash overlay's reload/load first completes. */
    public void onSplashComplete() {
        if (startupEndNs != 0) return;
        startupEndNs = System.nanoTime();
        double seconds = (startupEndNs - startupBeginNs) / 1_000_000_000.0;
        SolsticeMod.LOGGER.info("[Solstice/Startup] Game ready in {}s.", String.format("%.2f", seconds));
    }

    public double getStartupSeconds() {
        if (startupEndNs == 0) return -1;
        return (startupEndNs - startupBeginNs) / 1_000_000_000.0;
    }
}

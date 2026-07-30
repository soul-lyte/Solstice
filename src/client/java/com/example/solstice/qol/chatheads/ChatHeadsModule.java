package com.example.solstice.qol.chatheads;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Docks a real player's head to the specific chat message they sent,
 * ported to match dzwdz/chat_heads' actual "before line" system (MPL-2.0,
 * see NOTICE.md) - not a separate strip of recent senders (the module's
 * previous shape). Real sender detection mirrors the original mod's own
 * two-path approach: a real signed chat message's sender ({@code
 * ChatSenderTrackerMixin}, hooking {@code MessageHandler.onChatMessage})
 * takes priority; system/other messages fall back to a heuristic scan of
 * the message text for a currently-online player's name as a whole word
 * (see {@link #scanForSender}), matching the spirit of the original mod's
 * {@code scanForPlayerName} (simplified - no name aliasing).
 *
 * <p>The actual "shift text right, draw head in the margin" mechanism
 * lives in {@code ChatHudLineMixin}/{@code ChatHudLineVisibleMixin}/{@code
 * ChatHudMixin}/{@code ChatLineConsumerMixin} - this class only holds the
 * module toggle, the pending-sender hand-off state those Mixins share, and
 * the actual head-icon draw call. Deliberately not ported from the
 * original mod: {@code BEFORE_NAME} mode (inserting a custom glyph
 * directly into the message text via a custom font/GlyphProvider - a much
 * deeper, riskier piece of engine surgery than this "before line" mode,
 * even though it's the original mod's own default), the 3D head-tilt
 * effect, and name aliasing.</p>
 */
public final class ChatHeadsModule extends AbstractModule {

    private static final ChatHeadsModule INSTANCE = new ChatHeadsModule();

    /** Pixels the head + its padding take up, shifting chat text right by this much. */
    public static final int HEAD_WIDTH = 10;
    private static final int HEAD_SIZE = 8;

    /** Set by {@code ChatSenderTrackerMixin} right before the corresponding {@code ChatHudLine} is built. */
    private static UUID pendingMessageSender;
    /** Set by {@code ChatHudMixin} at the start of {@code addVisibleMessage}, consumed by the first wrapped line only. */
    private static UUID pendingLineSender;
    /** Captured by {@code ChatHudMixin} for the duration of {@code ChatHud.render(...)} so the line-consumer Mixin can draw. */
    public static DrawContext currentRenderContext;

    private boolean heuristicDetection = true;

    private ChatHeadsModule() {}

    public static ChatHeadsModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "chat_heads"; }
    @Override public String getDisplayName() { return "Chat Heads"; }
    @Override public String getDescription() { return "Shows the sender's real head next to their chat message, shifting the text over to make room."; }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public List<String> getSearchKeywords() {
        return List.of("player heads in chat", "chat avatars", "chatheads");
    }

    @Override protected boolean defaultEnabled() { return false; }

    /** Not default anywhere, including PVP - per explicit request. */
    @Override public boolean excludeFromPvpProfile() { return true; }

    @Override
    public List<ModuleSetting> getSettings() {
        return List.of(
                new ModuleSetting.BooleanSetting(
                        "Detect Senders By Name",
                        "For messages without a real signed sender (system messages, some servers' custom chat formats), scan the text for a currently-online player's name.",
                        () -> heuristicDetection,
                        v -> { heuristicDetection = v; ConfigManager.getInstance().set("chat_heads.heuristic_detection", v); })
        );
    }

    @Override
    protected void init() {
        heuristicDetection = ConfigManager.getInstance().getBoolean("chat_heads.heuristic_detection", true);
    }

    /** Called from {@code ChatSenderTrackerMixin} at {@code MessageHandler.onChatMessage}'s HEAD. */
    public static void setPendingMessageSender(UUID uuid) {
        pendingMessageSender = uuid;
    }

    /** Called from {@code ChatHudLineMixin}'s constructor tail-inject - consumes and clears. */
    public static UUID consumePendingMessageSender() {
        UUID sender = pendingMessageSender;
        pendingMessageSender = null;
        return sender;
    }

    /** Called from {@code ChatHudMixin}'s {@code addVisibleMessage} head-inject. */
    public static void setPendingLineSender(UUID uuid) {
        pendingLineSender = uuid;
    }

    /** Called from {@code ChatHudLineVisibleMixin}'s constructor tail-inject - consumes and clears (only the first wrapped line gets it). */
    public static UUID consumePendingLineSender() {
        UUID sender = pendingLineSender;
        pendingLineSender = null;
        return sender;
    }

    /**
     * Scans a message's plain text for a currently-online player's name as a
     * whole word - the fallback path for messages with no real signed sender.
     * Minecraft usernames are restricted to {@code [a-zA-Z0-9_]}, so no regex
     * escaping is ever needed for the name itself.
     */
    public UUID scanForSender(String text) {
        if (!heuristicDetection || text.isBlank()) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler network = client.getNetworkHandler();
        if (network == null) {
            return null;
        }
        for (PlayerListEntry entry : network.getPlayerList()) {
            String name = entry.getProfile().name();
            if (name.isEmpty()) {
                continue;
            }
            if (Pattern.compile("\\b" + name + "\\b").matcher(text).find()) {
                return entry.getProfile().id();
            }
        }
        return null;
    }

    /** Draws the sender's head (body + hat layer) at the given position - called from {@code ChatLineConsumerMixin}. */
    public void drawHead(DrawContext context, UUID sender, int x, int y, float opacity) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler network = client.getNetworkHandler();
        if (network == null) {
            return;
        }
        PlayerListEntry entry = network.getPlayerListEntry(sender);
        if (entry == null) {
            return;
        }

        Identifier skin = entry.getSkinTextures().body().texturePath();
        int color = ColorHelper.getWhite(opacity);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, HEAD_SIZE, HEAD_SIZE, 64, 64, color);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, HEAD_SIZE, HEAD_SIZE, 64, 64, color);
    }
}

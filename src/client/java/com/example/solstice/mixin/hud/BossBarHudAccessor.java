package com.example.solstice.mixin.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;
import java.util.UUID;

/**
 * Exposes {@link BossBarHud}'s package-private {@code bossBars} map and its
 * private per-bar draw method, so {@code BossBarHudElement} can draw the
 * exact same vanilla bar textures at Solstice's own stored position instead
 * of reimplementing them. {@code renderBossBar} has two overloads
 * (confirmed via javap) - the invoker method below's own 4-arg descriptor
 * disambiguates to the simpler one (background+notch+progress for one bar),
 * matching the real signature vanilla's own {@code render(DrawContext)}
 * calls per bar.
 */
@Mixin(BossBarHud.class)
public interface BossBarHudAccessor {

    @Accessor("bossBars")
    Map<UUID, ClientBossBar> solstice$getBossBars();

    @Invoker("renderBossBar")
    void solstice$renderBossBar(DrawContext context, int x, int y, BossBar bossBar);
}

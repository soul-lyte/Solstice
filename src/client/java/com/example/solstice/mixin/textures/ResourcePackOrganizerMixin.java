package com.example.solstice.mixin.textures;

import com.example.solstice.SolsticeMod;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

/**
 * Hides every Solstice-bundled builtin pack from vanilla's own resource pack
 * selection screen ({@code Options > Resource Packs}) - they're meant to be
 * switched only from the Textures tab, not fiddled with there too.
 *
 * <p>Deliberately filters only the two <em>display</em> streams
 * ({@code getEnabledPacks}/{@code getDisabledPacks}, consumed by
 * {@code PackListWidget} to build the visible rows) and not the organizer's
 * internal {@code enabledPacks}/{@code disabledPacks} lists. Those internal
 * lists are what {@code apply()} actually commits back to the real
 * {@link net.minecraft.resource.ResourcePackManager} via
 * {@code setEnabledProfiles} - filtering them too would mean simply opening
 * and closing the vanilla pack screen silently disables whichever Solstice
 * packs were active, which is not "hidden," that's "broken."</p>
 */
@Mixin(ResourcePackOrganizer.class)
public abstract class ResourcePackOrganizerMixin {

    @Inject(method = "getEnabledPacks", at = @At("RETURN"), cancellable = true)
    private void solstice$hideFromEnabled(CallbackInfoReturnable<Stream<ResourcePackOrganizer.Pack>> cir) {
        cir.setReturnValue(cir.getReturnValue().filter(solstice$notOurs()));
    }

    @Inject(method = "getDisabledPacks", at = @At("RETURN"), cancellable = true)
    private void solstice$hideFromDisabled(CallbackInfoReturnable<Stream<ResourcePackOrganizer.Pack>> cir) {
        cir.setReturnValue(cir.getReturnValue().filter(solstice$notOurs()));
    }

    private static java.util.function.Predicate<ResourcePackOrganizer.Pack> solstice$notOurs() {
        return pack -> !pack.getName().startsWith(SolsticeMod.MOD_ID + ":");
    }
}

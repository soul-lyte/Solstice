package com.example.solstice.core.module;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A single adjustable control exposed by a {@link Module} on its settings
 * screen. Modules build these directly around their own static fields and
 * existing {@code ConfigManager} keys - there is no reflection or separate
 * schema, each module just declares what it wants shown.
 */
public sealed interface ModuleSetting {

    String label();

    /** Plain-language explanation shown as a hover tooltip - no jargon, translates the technical label. */
    String tooltip();

    record IntSetting(String label, String tooltip, int min, int max,
                       IntSupplier getter, IntConsumer setter) implements ModuleSetting {}

    record DoubleSetting(String label, String tooltip, double min, double max,
                          DoubleSupplier getter, DoubleConsumer setter) implements ModuleSetting {}

    record BooleanSetting(String label, String tooltip,
                           BooleanSupplier getter, Consumer<Boolean> setter) implements ModuleSetting {}

    /** Cycles through a fixed list of named options (e.g. a style/mode picker). */
    record ChoiceSetting(String label, String tooltip, List<String> options,
                         IntSupplier indexGetter, IntConsumer indexSetter) implements ModuleSetting {}

    /** A rebindable keybinding, editable directly from the settings screen. */
    record KeySetting(String label, String tooltip,
                      java.util.function.Supplier<String> displayNameGetter, IntConsumer keyCodeSetter) implements ModuleSetting {}

    /** A packed {@code 0xRRGGBB} color, edited via a real saturation/value + hue picker (not R/G/B sliders). */
    record ColorSetting(String label, String tooltip,
                        IntSupplier getter, IntConsumer setter) implements ModuleSetting {}
}

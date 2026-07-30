package com.example.solstice.core.event.events;

import com.example.solstice.core.event.SolsticeEvent;

/**
 * Fired at the end of every client tick.
 * The payload is untyped to avoid a client-only import in the common source set.
 * Cast to {@code MinecraftClient} on the client side.
 */
public record ClientTickEvent(Object client) implements SolsticeEvent {}

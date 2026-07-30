package com.example.solstice.core.event;

import java.util.*;
import java.util.function.Consumer;

/**
 * Lightweight synchronous event bus for Solstice internal events.
 *
 * <p>Kept in the common source set - contains no client-only imports.
 * Client-specific fire helpers live in {@code SolsticeClient}.</p>
 */
public final class EventBus {

    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<Consumer<SolsticeEvent>>> listeners = new HashMap<>();

    private EventBus() {}

    public static EventBus getInstance() { return INSTANCE; }

    @SuppressWarnings("unchecked")
    public <T extends SolsticeEvent> void subscribe(Class<T> type, Consumer<T> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>())
                 .add((Consumer<SolsticeEvent>) listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends SolsticeEvent> void fire(T event) {
        List<Consumer<SolsticeEvent>> list = listeners.get(event.getClass());
        if (list == null) return;
        for (Consumer<SolsticeEvent> c : list) {
            c.accept(event);
        }
    }
}

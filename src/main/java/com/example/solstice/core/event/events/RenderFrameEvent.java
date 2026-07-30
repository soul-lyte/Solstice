package com.example.solstice.core.event.events;

import com.example.solstice.core.event.SolsticeEvent;

/** Fired once per rendered frame, before the world is drawn. */
public record RenderFrameEvent(float tickDelta) implements SolsticeEvent {}

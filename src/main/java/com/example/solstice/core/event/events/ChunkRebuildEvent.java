package com.example.solstice.core.event.events;

import com.example.solstice.core.event.SolsticeEvent;

/** Fired when a chunk section schedules a mesh rebuild. */
public record ChunkRebuildEvent(int chunkX, int chunkY, int chunkZ) implements SolsticeEvent {}

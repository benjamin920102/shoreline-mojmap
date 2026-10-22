package net.shoreline.client.impl.event.world;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.shoreline.eventbus.event.Event;

public class LoadChunkEvent extends Event {
   private final ChunkAccess chunk;

   public LoadChunkEvent(ChunkAccess chunk) {
      this.chunk = chunk;
   }

   public ChunkAccess getChunk() {
      return this.chunk;
   }
}

package net.shoreline.client.impl.event.world;

import net.minecraft.world.level.ChunkPos;
import net.shoreline.eventbus.event.StageEvent;

public class ChunkLoadEvent extends StageEvent {
   private final ChunkPos pos;

   public ChunkLoadEvent(ChunkPos pos) {
      this.pos = pos;
   }

   public ChunkPos getPos() {
      return this.pos;
   }
}

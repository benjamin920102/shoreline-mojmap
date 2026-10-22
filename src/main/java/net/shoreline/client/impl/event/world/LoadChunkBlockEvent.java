package net.shoreline.client.impl.event.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.eventbus.event.Event;

public class LoadChunkBlockEvent extends Event {
   private final BlockPos pos;
   private final BlockState state;

   public LoadChunkBlockEvent(BlockPos pos, BlockState state) {
      this.pos = pos;
      this.state = state;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getState() {
      return this.state;
   }
}

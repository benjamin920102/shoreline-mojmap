package net.shoreline.client.impl.event.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class SetBlockStateEvent extends Event {
   private final int flags;
   private final BlockPos pos;
   private final BlockState state;

   public SetBlockStateEvent(int flags, BlockPos pos, BlockState state) {
      this.flags = flags;
      this.pos = pos;
      this.state = state;
   }

   public int getFlags() {
      return this.flags;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getState() {
      return this.state;
   }
}

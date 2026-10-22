package net.shoreline.client.impl.event.render.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderBlockEvent extends Event {
   private final BlockState state;
   private final BlockPos pos;

   public RenderBlockEvent(BlockState state, BlockPos pos) {
      this.state = state;
      this.pos = pos;
   }

   public BlockState getState() {
      return this.state;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public Block getBlock() {
      return this.state.getBlock();
   }
}

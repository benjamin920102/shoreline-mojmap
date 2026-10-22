package net.shoreline.client.impl.event.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class AttackBlockEvent extends Event {
   private final BlockPos pos;
   private final BlockState state;
   private final Direction direction;

   public AttackBlockEvent(BlockPos pos, BlockState state, Direction direction) {
      this.pos = pos;
      this.state = state;
      this.direction = direction;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getState() {
      return this.state;
   }

   public Direction getDirection() {
      return this.direction;
   }
}

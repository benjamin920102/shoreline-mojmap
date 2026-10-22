package net.shoreline.client.impl.event.entity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class VelocityMultiplierEvent extends Event {
   private final BlockState state;

   public VelocityMultiplierEvent(BlockState state) {
      this.state = state;
   }

   public Block getBlock() {
      return this.state.getBlock();
   }
}

package net.shoreline.client.impl.event.entity;

import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class SlowMovementEvent extends Event {
   private final BlockState state;
   private float multiplier = 1.0F;

   public SlowMovementEvent(BlockState state) {
      this.state = state;
   }

   public BlockState getState() {
      return this.state;
   }

   public float getMultiplier() {
      return this.multiplier;
   }

   public void setMultiplier(float multiplier) {
      this.multiplier = multiplier;
   }
}

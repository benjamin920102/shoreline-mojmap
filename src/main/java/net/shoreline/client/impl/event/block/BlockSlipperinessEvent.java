package net.shoreline.client.impl.event.block;

import net.minecraft.world.level.block.Block;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class BlockSlipperinessEvent extends Event {
   private final Block block;
   private float slipperiness;

   public BlockSlipperinessEvent(Block block, float slipperiness) {
      this.block = block;
      this.slipperiness = slipperiness;
   }

   public Block getBlock() {
      return this.block;
   }

   public float getSlipperiness() {
      return this.slipperiness;
   }

   public void setSlipperiness(float slipperiness) {
      this.slipperiness = slipperiness;
   }
}

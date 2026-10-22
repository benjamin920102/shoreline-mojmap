package net.shoreline.client.impl.event.render.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.eventbus.event.Event;

public class RenderBlockEntityEvent extends Event {
   private final BlockEntity blockEntity;

   public RenderBlockEntityEvent(BlockEntity blockEntity) {
      this.blockEntity = blockEntity;
   }

   public BlockPos getPos() {
      return this.blockEntity.getBlockPos();
   }

   public BlockEntity getBlockEntity() {
      return this.blockEntity;
   }

   public BlockState getCachedState() {
      return this.blockEntity.getBlockState();
   }
}

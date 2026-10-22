package net.shoreline.client.impl.event.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class BlockCollisionEvent extends Event {
   private final BlockPos pos;
   private final BlockState state;
   private VoxelShape voxelShape;

   public BlockCollisionEvent(VoxelShape voxelShape, BlockPos pos, BlockState state) {
      this.pos = pos;
      this.state = state;
      this.voxelShape = voxelShape;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getState() {
      return this.state;
   }

   public Block getBlock() {
      return this.state.getBlock();
   }

   public VoxelShape getVoxelShape() {
      return this.voxelShape;
   }

   public void setVoxelShape(VoxelShape voxelShape) {
      this.voxelShape = voxelShape;
   }
}

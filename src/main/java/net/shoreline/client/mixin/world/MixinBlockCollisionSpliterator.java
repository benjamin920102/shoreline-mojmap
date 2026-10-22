package net.shoreline.client.mixin.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shoreline.client.impl.event.world.BlockCollisionEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BlockCollisions.class})
public class MixinBlockCollisionSpliterator implements Globals {
   @Redirect(
      method = {"computeNext()Ljava/lang/Object;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/state/BlockState;getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
      )
   )
   private VoxelShape hookGetCollisionShape(BlockState instance, BlockGetter blockView, BlockPos blockPos, CollisionContext shapeContext) {
      VoxelShape voxelShape = instance.getCollisionShape(blockView, blockPos, shapeContext);
      if (blockView != mc.level) {
         return voxelShape;
      } else {
         BlockCollisionEvent blockCollisionEvent = new BlockCollisionEvent(voxelShape, blockPos, instance);
         EventBus.INSTANCE.dispatch(blockCollisionEvent);
         return blockCollisionEvent.isCanceled() ? blockCollisionEvent.getVoxelShape() : voxelShape;
      }
   }
}

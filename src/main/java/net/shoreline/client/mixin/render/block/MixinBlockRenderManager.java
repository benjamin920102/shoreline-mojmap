package net.shoreline.client.mixin.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.client.impl.event.render.block.RenderBlockEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockRenderDispatcher.class})
public class MixinBlockRenderManager {
   @Inject(
      method = {"renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderBlock(
      BlockState state,
      BlockPos pos,
      BlockAndTintGetter world,
      PoseStack matrices,
      VertexConsumer vertexConsumer,
      boolean cull,
      RandomSource random,
      CallbackInfo ci
   ) {
      RenderBlockEvent renderBlockEvent = new RenderBlockEvent(state, pos);
      EventBus.INSTANCE.dispatch(renderBlockEvent);
      if (renderBlockEvent.isCanceled()) {
         ci.cancel();
      }
   }
}

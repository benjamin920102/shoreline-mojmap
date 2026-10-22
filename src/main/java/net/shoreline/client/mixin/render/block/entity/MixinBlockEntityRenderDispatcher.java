package net.shoreline.client.mixin.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shoreline.client.impl.event.render.block.entity.RenderBlockEntityEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockEntityRenderDispatcher.class})
public class MixinBlockEntityRenderDispatcher {
   @Inject(
      method = {"render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"},
      at = {@At("HEAD")}
   )
   private void hookRender(BlockEntity blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
      RenderBlockEntityEvent renderBlockEntityEvent = new RenderBlockEntityEvent(blockEntity);
      EventBus.INSTANCE.dispatch(renderBlockEntityEvent);
   }
}

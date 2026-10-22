package net.shoreline.client.mixin.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignText;
import net.shoreline.client.impl.event.render.block.entity.RenderSignTextEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SignRenderer.class})
public class MixinSignBlockEntityRenderer {
   @Inject(
      method = {"renderSignText(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SignText;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIIZ)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderText(
      BlockPos pos,
      SignText signText,
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      int lineHeight,
      int lineWidth,
      boolean front,
      CallbackInfo ci
   ) {
      RenderSignTextEvent renderSignTextEvent = new RenderSignTextEvent();
      EventBus.INSTANCE.dispatch(renderSignTextEvent);
      if (renderSignTextEvent.isCanceled()) {
         ci.cancel();
      }
   }
}

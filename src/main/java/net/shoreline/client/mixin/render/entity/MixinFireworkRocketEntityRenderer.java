package net.shoreline.client.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.FireworkEntityRenderer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.shoreline.client.impl.event.render.entity.RenderFireworkRocketEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FireworkEntityRenderer.class})
public class MixinFireworkRocketEntityRenderer {
   @Inject(
      method = {"render(Lnet/minecraft/world/entity/projectile/FireworkRocketEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRender(
      FireworkRocketEntity fireworkRocketEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci
   ) {
      RenderFireworkRocketEvent renderFireworkRocketEvent = new RenderFireworkRocketEvent();
      EventBus.INSTANCE.dispatch(renderFireworkRocketEvent);
      if (renderFireworkRocketEvent.isCanceled()) {
         ci.cancel();
      }
   }
}

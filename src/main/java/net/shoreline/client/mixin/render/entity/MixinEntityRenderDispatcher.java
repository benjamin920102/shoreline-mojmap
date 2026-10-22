package net.shoreline.client.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.shoreline.client.impl.event.render.entity.RenderEntityWorldEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public class MixinEntityRenderDispatcher {
   @Inject(
      method = {"render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("RETURN")}
   )
   private void hookRender(
      Entity entity,
      double x,
      double y,
      double z,
      float yaw,
      float tickDelta,
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      RenderEntityWorldEvent renderEntityEvent = new RenderEntityWorldEvent(entity, x, y, z, yaw, tickDelta, matrices, vertexConsumers);
      EventBus.INSTANCE.dispatch(renderEntityEvent);
   }
}

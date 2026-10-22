package net.shoreline.client.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.shoreline.client.impl.event.render.entity.ElytraTransformEvent;
import net.shoreline.client.impl.event.render.entity.RenderPlayerEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerRenderer.class})
public class MixinPlayerEntityRenderer {
   @Unique
   private float yaw;
   @Unique
   private float prevYaw;
   @Unique
   private float bodyYaw;
   @Unique
   private float prevBodyYaw;
   @Unique
   private float headYaw;
   @Unique
   private float prevHeadYaw;
   @Unique
   private float pitch;
   @Unique
   private float prevPitch;
   @Unique
   private boolean prevSneaking;
   @Unique
   private boolean prevFallFlying;

   @Inject(
      method = {"render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")}
   )
   private void onRenderHead(
      AbstractClientPlayer abstractClientPlayerEntity,
      float f,
      float g,
      PoseStack matrixStack,
      MultiBufferSource vertexConsumerProvider,
      int i,
      CallbackInfo ci
   ) {
      RenderPlayerEvent renderPlayerEvent = new RenderPlayerEvent(abstractClientPlayerEntity);
      EventBus.INSTANCE.dispatch(renderPlayerEvent);
      this.yaw = abstractClientPlayerEntity.getYRot();
      this.prevYaw = abstractClientPlayerEntity.yRotO;
      this.bodyYaw = abstractClientPlayerEntity.yBodyRot;
      this.prevBodyYaw = abstractClientPlayerEntity.yBodyRotO;
      this.headYaw = abstractClientPlayerEntity.yHeadRot;
      this.prevHeadYaw = abstractClientPlayerEntity.yHeadRotO;
      this.pitch = abstractClientPlayerEntity.getXRot();
      this.prevPitch = abstractClientPlayerEntity.xRotO;
      this.prevSneaking = abstractClientPlayerEntity.isShiftKeyDown();
      if (renderPlayerEvent.isCanceled()) {
         abstractClientPlayerEntity.setYRot(renderPlayerEvent.getYaw());
         abstractClientPlayerEntity.yRotO = renderPlayerEvent.getYaw();
         abstractClientPlayerEntity.setYBodyRot(renderPlayerEvent.getYaw());
         abstractClientPlayerEntity.yBodyRotO = renderPlayerEvent.getYaw();
         abstractClientPlayerEntity.setYHeadRot(renderPlayerEvent.getYaw());
         abstractClientPlayerEntity.yHeadRotO = renderPlayerEvent.getYaw();
         abstractClientPlayerEntity.setXRot(renderPlayerEvent.getPitch());
         abstractClientPlayerEntity.xRotO = renderPlayerEvent.getPitch();
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("TAIL")}
   )
   private void onRenderTail(
      AbstractClientPlayer abstractClientPlayerEntity,
      float f,
      float g,
      PoseStack matrixStack,
      MultiBufferSource vertexConsumerProvider,
      int i,
      CallbackInfo ci
   ) {
      abstractClientPlayerEntity.setYRot(this.yaw);
      abstractClientPlayerEntity.yRotO = this.prevYaw;
      abstractClientPlayerEntity.setYBodyRot(this.bodyYaw);
      abstractClientPlayerEntity.yBodyRotO = this.prevBodyYaw;
      abstractClientPlayerEntity.setYHeadRot(this.headYaw);
      abstractClientPlayerEntity.yHeadRotO = this.prevHeadYaw;
      abstractClientPlayerEntity.setXRot(this.pitch);
      abstractClientPlayerEntity.xRotO = this.prevPitch;
   }

   @Redirect(
      method = {"setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/AbstractClientPlayer;isFallFlying()Z"
      )
   )
   private boolean hookSetupTransforms(AbstractClientPlayer instance) {
      ElytraTransformEvent elytraTransformEvent = new ElytraTransformEvent(instance);
      EventBus.INSTANCE.dispatch(elytraTransformEvent);
      return elytraTransformEvent.isCanceled() ? false : instance.isFallFlying();
   }
}

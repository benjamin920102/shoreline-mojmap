package net.shoreline.client.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.shoreline.client.impl.event.PerspectiveEvent;
import net.shoreline.client.impl.event.render.RenderEntityInWorldEvent;
import net.shoreline.client.impl.event.render.RenderShaderEvent;
import net.shoreline.client.impl.event.render.RenderWorldBorderEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public class MixinWorldRenderer implements Globals {
   @Inject(
      method = {"renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"},
      at = {@At("RETURN")}
   )
   private void hookRender(
      DeltaTracker tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightTexture lightmapTextureManager,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CallbackInfo ci
   ) {
      PoseStack matrixStack = new PoseStack();
      RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.last().pose());
      matrixStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
      matrixStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
      RenderSystem.applyModelViewMatrix();
      RenderWorldEvent renderWorldEvent = new RenderWorldEvent(matrixStack, tickCounter.getGameTimeDeltaPartialTick(true));
      EventBus.INSTANCE.dispatch(renderWorldEvent);
      RenderSystem.getModelViewStack().popMatrix();
      RenderSystem.applyModelViewMatrix();
   }

   @Inject(
      method = {"renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"},
      at = {@At("RETURN")}
   )
   private void hookRender$1(
      DeltaTracker tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightTexture lightmapTextureManager,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CallbackInfo ci
   ) {
      PoseStack matrixStack = new PoseStack();
      matrixStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
      matrixStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
      RenderShaderEvent renderOutlineShaderEvent = new RenderShaderEvent(matrixStack, tickCounter.getGameTimeDeltaPartialTick(true));
      EventBus.INSTANCE.dispatch(renderOutlineShaderEvent);
   }

   @Inject(
      method = {"renderWorldBorder(Lnet/minecraft/client/Camera;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderWorldBorder(Camera camera, CallbackInfo ci) {
      RenderWorldBorderEvent renderWorldBorderEvent = new RenderWorldBorderEvent();
      EventBus.INSTANCE.dispatch(renderWorldBorderEvent);
      if (renderWorldBorderEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Redirect(
      method = {"renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;isDetached()Z"
      )
   )
   public boolean hookRender(Camera instance) {
      PerspectiveEvent perspectiveEvent = new PerspectiveEvent(instance);
      EventBus.INSTANCE.dispatch(perspectiveEvent);
      return perspectiveEvent.isCanceled() ? true : instance.isDetached();
   }

   @Inject(
      method = {"renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderEntity(
      Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci
   ) {
      RenderEntityInWorldEvent renderEntityInWorldEvent = new RenderEntityInWorldEvent(entity);
      EventBus.INSTANCE.dispatch(renderEntityInWorldEvent);
      if (renderEntityInWorldEvent.isCanceled()) {
         ci.cancel();
      }
   }
}

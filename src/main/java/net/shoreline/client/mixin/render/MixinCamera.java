package net.shoreline.client.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.world.level.material.FogType;
import net.shoreline.client.impl.event.camera.CameraPositionEvent;
import net.shoreline.client.impl.event.camera.CameraRotationEvent;
import net.shoreline.client.impl.event.gui.hud.RenderOverlayEvent;
import net.shoreline.client.impl.event.render.CameraClipEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public abstract class MixinCamera {
   @Shadow
   private float partialTickTime;

   @Shadow
   protected abstract void setPosition(double var1, double var3, double var5);

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Inject(
      method = {"getFluidInCamera()Lnet/minecraft/world/level/material/FogType;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetSubmersionType(CallbackInfoReturnable<FogType> cir) {
      RenderOverlayEvent.Water renderOverlayEvent = new RenderOverlayEvent.Water(null);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         cir.setReturnValue(FogType.NONE);
         cir.cancel();
      }
   }

   @Inject(
      method = {"getMaxZoom(F)F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookClipToSpace(float f, CallbackInfoReturnable<Float> cir) {
      CameraClipEvent cameraClipEvent = new CameraClipEvent(f);
      EventBus.INSTANCE.dispatch(cameraClipEvent);
      if (cameraClipEvent.isCanceled()) {
         cir.setReturnValue(cameraClipEvent.getDistance());
         cir.cancel();
      }
   }

   @Redirect(
      method = {"setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"
      )
   )
   private void hookUpdatePosition(Camera instance, double x, double y, double z) {
      CameraPositionEvent cameraPositionEvent = new CameraPositionEvent(x, y, z, this.partialTickTime);
      EventBus.INSTANCE.dispatch(cameraPositionEvent);
      this.setPosition(cameraPositionEvent.getX(), cameraPositionEvent.getY(), cameraPositionEvent.getZ());
   }

   @Redirect(
      method = {"setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;setRotation(FF)V"
      )
   )
   private void hookUpdateRotation(Camera instance, float yaw, float pitch) {
      CameraRotationEvent cameraRotationEvent = new CameraRotationEvent(yaw, pitch, this.partialTickTime);
      EventBus.INSTANCE.dispatch(cameraRotationEvent);
      this.setRotation(cameraRotationEvent.getYaw(), cameraRotationEvent.getPitch());
   }
}

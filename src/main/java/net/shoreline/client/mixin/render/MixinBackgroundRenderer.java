package net.shoreline.client.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.client.renderer.FogRenderer.MobEffectFogFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.render.RenderFogEvent;
import net.shoreline.client.impl.event.world.BlindnessEvent;
import net.shoreline.client.impl.event.world.SkyboxEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FogRenderer.class})
public class MixinBackgroundRenderer {
   @Shadow
   private static float fogRed;
   @Shadow
   private static float fogGreen;
   @Shadow
   private static float fogBlue;

   @Inject(
      method = {"setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V"},
      at = {@At("TAIL")}
   )
   private static void hookApplyFog(Camera camera, FogMode fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
      if (fogType == FogMode.FOG_TERRAIN) {
         RenderFogEvent renderFogEvent = new RenderFogEvent(viewDistance);
         EventBus.INSTANCE.dispatch(renderFogEvent);
         if (renderFogEvent.isCanceled()) {
            RenderSystem.setShaderFogStart(renderFogEvent.getStart());
            RenderSystem.setShaderFogEnd(renderFogEvent.getEnd());
         }
      }
   }

   @Inject(
      method = {"setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookRender(Camera camera, float tickDelta, ClientLevel world, int viewDistance, float skyDarkness, CallbackInfo ci) {
      SkyboxEvent.Fog skyboxEvent = new SkyboxEvent.Fog(tickDelta);
      EventBus.INSTANCE.dispatch(skyboxEvent);
      if (skyboxEvent.isCanceled()) {
         ci.cancel();
         Vec3 vec3d = skyboxEvent.getColorVec();
         fogRed = (float)vec3d.x;
         fogGreen = (float)vec3d.y;
         fogBlue = (float)vec3d.z;
         RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
      }
   }

   @Inject(
      method = {"getPriorityFogFunction(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/FogRenderer$MobEffectFogFunction;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<MobEffectFogFunction> cir) {
      BlindnessEvent blindnessEvent = new BlindnessEvent();
      EventBus.INSTANCE.dispatch(blindnessEvent);
      if (blindnessEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(null);
      }
   }
}

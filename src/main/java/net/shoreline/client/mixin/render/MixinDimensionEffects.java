package net.shoreline.client.mixin.render;

import java.awt.Color;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.shoreline.client.impl.event.world.SkyboxEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({DimensionSpecialEffects.class})
public class MixinDimensionEffects {
   @Inject(
      method = {"getSunriseColor(FF)[F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetFogColorOverride(float skyAngle, float tickDelta, CallbackInfoReturnable<float[]> cir) {
      SkyboxEvent.Fog skyboxEvent = new SkyboxEvent.Fog(tickDelta);
      EventBus.INSTANCE.dispatch(skyboxEvent);
      if (skyboxEvent.isCanceled()) {
         Color color = skyboxEvent.getColor();
         cir.cancel();
         cir.setReturnValue(new float[]{color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F});
      }
   }
}

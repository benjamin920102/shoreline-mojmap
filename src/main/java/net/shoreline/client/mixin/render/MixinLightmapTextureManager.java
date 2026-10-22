package net.shoreline.client.mixin.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Color;
import net.minecraft.client.renderer.LightTexture;
import net.shoreline.client.impl.event.render.AmbientColorEvent;
import net.shoreline.client.impl.event.render.LightmapGammaEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({LightTexture.class})
public class MixinLightmapTextureManager {
   @Shadow
   @Final
   private NativeImage lightPixels;

   @ModifyArgs(
      method = {"updateLightTexture(F)V"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"
      )
   )
   private void hookUpdate(Args args) {
      LightmapGammaEvent lightmapGammaEvent = new LightmapGammaEvent((Integer)args.get(2));
      EventBus.INSTANCE.dispatch(lightmapGammaEvent);
      if (lightmapGammaEvent.isCanceled()) {
         args.set(2, lightmapGammaEvent.getGamma());
      }
   }

   @Inject(
      method = {"updateLightTexture(F)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V",
         shift = Shift.BEFORE
      )}
   )
   private void hookUpdate(float delta, CallbackInfo ci) {
      AmbientColorEvent ambientColorEvent = new AmbientColorEvent();
      EventBus.INSTANCE.dispatch(ambientColorEvent);
      Color c = ambientColorEvent.getColor();
      if (ambientColorEvent.isCanceled()) {
         for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
               int r = c.getRed();
               int g = c.getGreen();
               int b = c.getBlue();
               this.lightPixels.setPixelRGBA(i, j, 0xFF000000 | b << 16 | g << 8 | r);
            }
         }
      }
   }
}

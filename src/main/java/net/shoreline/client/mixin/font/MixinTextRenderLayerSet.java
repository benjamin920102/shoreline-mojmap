package net.shoreline.client.mixin.font;

import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.api.render.layers.RenderLayersClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GlyphRenderTypes.class})
public class MixinTextRenderLayerSet {
   @Inject(
      method = {"createForColorTexture(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/gui/font/GlyphRenderTypes;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookOf(ResourceLocation textureId, CallbackInfoReturnable<GlyphRenderTypes> cir) {
      cir.cancel();
      cir.setReturnValue(
         new GlyphRenderTypes(
            RenderLayersClient.TEXT.apply(textureId),
            RenderLayersClient.TEXT_SEE_THROUGH.apply(textureId),
            RenderLayersClient.TEXT_POLYGON_OFFSET.apply(textureId)
         )
      );
   }

   @Inject(
      method = {"createForIntensityTexture(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/gui/font/GlyphRenderTypes;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookOfIntensity(ResourceLocation textureId, CallbackInfoReturnable<GlyphRenderTypes> cir) {
      cir.cancel();
      cir.setReturnValue(
         new GlyphRenderTypes(
            RenderLayersClient.TEXT_INTENSITY.apply(textureId),
            RenderLayersClient.TEXT_INTENSITY_SEE_THROUGH.apply(textureId),
            RenderLayersClient.TEXT_INTENSITY_POLYGON_OFFSET.apply(textureId)
         )
      );
   }
}

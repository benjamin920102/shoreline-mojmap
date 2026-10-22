package net.shoreline.client.mixin.accessor;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Font.class})
public interface AccessorTextRenderer {
   @Accessor("filterFishyGlyphs")
   boolean hookGetValidateAdvance();

   @Invoker("getFontSet")
   FontSet hookGetFontStorage(ResourceLocation var1);

   @Invoker("renderChar")
   void hookDrawGlyph(
      BakedGlyph var1,
      boolean var2,
      boolean var3,
      float var4,
      float var5,
      float var6,
      Matrix4f var7,
      VertexConsumer var8,
      float var9,
      float var10,
      float var11,
      float var12,
      int var13
   );

   @Invoker("renderText")
   float hookDrawLayer(
      String var1, float var2, float var3, int var4, boolean var5, Matrix4f var6, MultiBufferSource var7, DisplayMode var8, int var9, int var10
   );
}

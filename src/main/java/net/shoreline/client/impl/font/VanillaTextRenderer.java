package net.shoreline.client.impl.font;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.GlyphProvider.Conditional;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.FontOption.Filter;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.font.glyphs.BakedGlyph.Effect;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition.Loader;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition.Shift;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.shoreline.client.mixin.accessor.AccessorTextRenderer;
import net.shoreline.client.util.Globals;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

@Deprecated
public class VanillaTextRenderer implements Globals {
   public static Font getTextRender(String fontName) throws IOException {
      List<Conditional> list = Lists.newArrayList();
      TrueTypeGlyphProviderDefinition loader = new TrueTypeGlyphProviderDefinition(
         ResourceLocation.parse(String.format("shoreline:%s.ttf", fontName)), 11.0F, 20.0F, Shift.NONE, ""
      );
      Loader loadable = (Loader)loader.unpack().orThrow();
      GlyphProvider font = loadable.load(mc.getResourceManager());
      list.add(new Conditional(font, Filter.ALWAYS_PASS));
      FontSet storage = new FontSet(mc.getTextureManager(), ResourceLocation.parse("shoreline:tr"));
      storage.reload(list, Collections.emptySet());
      return new Font(id -> storage, true);
   }

   public void drawWithShadow(PoseStack matrices, String text, float x, float y, int color) {
      this.draw(matrices, text, x + 1.0F, y + 1.0F, color, true);
      this.draw(matrices, text, x, y, color, false);
   }

   public void draw(PoseStack matrices, String text, float x, float y, int color, boolean shadow) {
      this.draw(text, x, y, color, matrices.last().pose(), shadow);
   }

   private void draw(String text, float x, float y, int color, Matrix4f matrix, boolean shadow) {
      if (text != null) {
         BufferSource immediate = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
         this.draw(text, x, y, color, shadow, matrix, immediate, DisplayMode.NORMAL, 0, 15728880);
         immediate.endBatch();
      }
   }

   public void draw(
      String text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      MultiBufferSource vertexConsumers,
      DisplayMode layerType,
      int backgroundColor,
      int light
   ) {
      this.drawInternal(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light);
   }

   private void drawInternal(
      String text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      MultiBufferSource vertexConsumers,
      DisplayMode layerType,
      int backgroundColor,
      int light
   ) {
      Matrix4f matrix4f = new Matrix4f(matrix);
      this.drawLayer(text, x, y, color, shadow, matrix4f, vertexConsumers, layerType, backgroundColor, light);
   }

   private void drawLayer(
      String text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      MultiBufferSource vertexConsumerProvider,
      DisplayMode layerType,
      int underlineColor,
      int light
   ) {
      VanillaTextRenderer.Drawer drawer = new VanillaTextRenderer.Drawer(vertexConsumerProvider, x, y, color, shadow, matrix, layerType, light);
      StringDecomposer.iterateFormatted(text, Style.EMPTY, drawer);
      drawer.drawLayer();
   }

   public static class Drawer implements FormattedCharSink {
      final MultiBufferSource vertexConsumers;
      private final float brightnessMultiplier;
      private final float red;
      private final float green;
      private final float blue;
      private final float alpha;
      private final Matrix4f matrix;
      private final DisplayMode layerType;
      private final int light;
      float x;
      float y;
      @Nullable
      private List<Effect> rectangles;

      public Drawer(MultiBufferSource vertexConsumers, float x, float y, int color, boolean shadow, Matrix4f matrix, DisplayMode layerType, int light) {
         this.vertexConsumers = vertexConsumers;
         this.x = x;
         this.y = y;
         this.brightnessMultiplier = shadow ? 0.25F : 1.0F;
         this.red = (color >> 16 & 0xFF) / 255.0F * this.brightnessMultiplier;
         this.green = (color >> 8 & 0xFF) / 255.0F * this.brightnessMultiplier;
         this.blue = (color & 0xFF) / 255.0F * this.brightnessMultiplier;
         this.alpha = (color >> 24 & 0xFF) / 255.0F;
         this.matrix = matrix;
         this.layerType = layerType;
         this.light = light;
      }

      @Override
      public boolean accept(int i, Style style, int j) {
         FontSet fontStorage = ((AccessorTextRenderer)Globals.mc.font).hookGetFontStorage(style.getFont());
         GlyphInfo glyph = fontStorage.getGlyphInfo(j, ((AccessorTextRenderer)Globals.mc.font).hookGetValidateAdvance());
         BakedGlyph glyphRenderer = style.isObfuscated() && j != 32 ? fontStorage.getRandomGlyph(glyph) : fontStorage.getGlyph(j);
         boolean bl = style.isBold();
         float f = this.alpha;
         TextColor textColor = style.getColor();
         float l;
         float h;
         float g;
         if (textColor != null) {
            int k = textColor.getValue();
            g = (k >> 16 & 0xFF) / 255.0F * this.brightnessMultiplier;
            h = (k >> 8 & 0xFF) / 255.0F * this.brightnessMultiplier;
            l = (k & 0xFF) / 255.0F * this.brightnessMultiplier;
         } else {
            g = this.red;
            h = this.green;
            l = this.blue;
         }

         if (!(glyphRenderer instanceof EmptyGlyph)) {
            float m = bl ? glyph.getBoldOffset() : 0.0F;
            VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(glyphRenderer.renderType(this.layerType));
            ((AccessorTextRenderer)Globals.mc.font)
               .hookDrawGlyph(glyphRenderer, bl, style.isItalic(), m, this.x, this.y, this.matrix, vertexConsumer, g, h, l, f, this.light);
         }

         float m = glyph.getAdvance(bl);
         this.x += m;
         return true;
      }

      public void drawLayer() {
         if (this.rectangles != null) {
            BakedGlyph glyphRenderer = ((AccessorTextRenderer)Globals.mc.font).hookGetFontStorage(Style.DEFAULT_FONT).whiteGlyph();
            VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(glyphRenderer.renderType(this.layerType));

            for (Effect rectangle : this.rectangles) {
               glyphRenderer.renderEffect(rectangle, this.matrix, vertexConsumer, this.light);
            }
         }
      }
   }
}

package net.shoreline.client.impl.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.awt.Color;
import java.awt.Font;
import java.io.Closeable;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.api.font.Glyph;
import net.shoreline.client.api.font.GlyphCache;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.client.FontModule;
import net.shoreline.client.impl.module.client.SocialsModule;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.math.HexRandom;
import org.joml.Matrix4f;

public final class AWTFontRenderer implements Closeable, Globals {
   private Font font;
   private final float size;
   private int scale;
   private int lastScale;
   private static final Pattern PATTERN_CONTROL_CODE = Pattern.compile("(?i)\\u00A7[0-9A-FK-OG]");
   private final ObjectList<GlyphCache> caches = new ObjectArrayList();
   private final Char2ObjectArrayMap<Glyph> glyphs = new Char2ObjectArrayMap();
   private final Map<ResourceLocation, ObjectList<AWTFontRenderer.CharLocation>> cache = new Object2ObjectOpenHashMap();

   public AWTFontRenderer(InputStream inputStream, float size) {
      try {
         this.font = Font.createFont(0, inputStream);
      } catch (Throwable var4) {
         var4.printStackTrace();
         this.font = new Font("Verdana", 0, Math.round(size));
      }

      this.size = size;
      this.createFont(this.font, size);
   }

   public static String stripControlCodes(String text) {
      return PATTERN_CONTROL_CODE.matcher(text).replaceAll("");
   }

   private void createFont(Font font, float size) {
      this.lastScale = (int)mc.getWindow().getGuiScale();
      this.scale = this.lastScale;
      this.font = font.deriveFont(size * this.scale);
   }

   public void drawStringWithShadow(PoseStack stack, String text, double x, double y, int color) {
      this.drawString(stack, text, x + 0.5, y + 0.5, color, true);
      this.drawString(stack, text, x, y, color, false);
   }

   public void drawString(PoseStack stack, String text, double x, double y, int color) {
      this.drawString(stack, text, x, y, color, false);
   }

   private void drawString(PoseStack stack, String text, double x, double y, int color, boolean shadow) {
      float brightnessMultiplier = shadow ? 0.25F : 1.0F;
      float r = (color >> 16 & 0xFF) / 255.0F * brightnessMultiplier;
      float g = (color >> 8 & 0xFF) / 255.0F * brightnessMultiplier;
      float b = (color & 0xFF) / 255.0F * brightnessMultiplier;
      float a = (color >> 24 & 0xFF) / 255.0F;
      this.drawString(stack, text, (float)x, (float)y, r, g, b, a, brightnessMultiplier);
   }

   public void drawString(PoseStack stack, String text, double x, double y, Color color) {
      this.drawString(stack, text, x, y, color, false);
   }

   public void drawString(PoseStack stack, String text, double x, double y, Color color, boolean shadow) {
      float brightnessMultiplier = shadow ? 0.25F : 1.0F;
      this.drawString(
         stack,
         text,
         (float)x,
         (float)y,
         color.getRed() / 255.0F * brightnessMultiplier,
         color.getGreen() / 255.0F * brightnessMultiplier,
         color.getBlue() / 255.0F * brightnessMultiplier,
         color.getAlpha(),
         brightnessMultiplier
      );
   }

   public void drawString(PoseStack stack, String text, float x, float y, float r, float g, float b, float a, float brightnessMultiplier) {
      int currentScale = (int)mc.getWindow().getGuiScale();
      if (currentScale != this.lastScale) {
         this.close();
         this.createFont(this.font, this.size);
      }

      float r2 = r;
      float g2 = g;
      float b2 = b;
      stack.pushPose();
      y -= 3.0F;
      stack.translate(x, y, 0.0F);
      stack.scale(1.0F / this.scale, 1.0F / this.scale, 0.0F);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      Tesselator tessellator = Tesselator.getInstance();
      Matrix4f matrix4f = stack.last().pose();
      char[] chars = text.toCharArray();
      float xOffset = 0.0F;
      float yOffset = 0.0F;
      boolean formatting = false;
      int lineStart = 0;
      this.glyphs.clear();
      synchronized (this.cache) {
         for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (formatting) {
               formatting = false;
               if (c == 'r') {
                  r2 = r;
                  g2 = g;
                  b2 = b;
               } else {
                  int colorCode = this.getColorFromCode(c);
                  int[] col = this.toRgbComponents(colorCode);
                  r2 = col[0] / 255.0F * brightnessMultiplier;
                  g2 = col[1] / 255.0F * brightnessMultiplier;
                  b2 = col[2] / 255.0F * brightnessMultiplier;
               }
            } else if (c == 167) {
               formatting = true;
            } else if (c == '\n') {
               yOffset += this.getStringHeight(text.substring(lineStart, i)) * this.scale;
               xOffset = 0.0F;
               lineStart = i + 1;
            } else {
               Glyph glyph = (Glyph)this.glyphs.computeIfAbsent(c, g1 -> this.getGlyphFromChar(g1));
               if (glyph != null) {
                  if (glyph.value() != ' ') {
                     ResourceLocation i1 = glyph.owner().getId();
                     AWTFontRenderer.CharLocation entry = new AWTFontRenderer.CharLocation(xOffset, yOffset, r2, g2, b2, glyph);
                     this.cache.computeIfAbsent(i1, integer -> new ObjectArrayList()).add(entry);
                  }

                  xOffset += glyph.width();
               }
            }
         }

         for (ResourceLocation identifier : this.cache.keySet()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.texParameter(3553, 10241, 9729);
            RenderSystem.texParameter(3553, 10240, 9729);

            try {
               RenderSystem.setShaderTexture(0, identifier);
            } catch (Exception var42) {
               continue;
            }

            List<AWTFontRenderer.CharLocation> objects = (List<AWTFontRenderer.CharLocation>)this.cache.get(identifier);
            BufferBuilder bufferBuilder = tessellator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            for (AWTFontRenderer.CharLocation object : objects) {
               float xo = object.x;
               float yo = object.y;
               float cr = object.r;
               float cg = object.g;
               float cb = object.b;
               Glyph glyph = object.glyph;
               GlyphCache owner = glyph.owner();
               float w = glyph.width();
               float h = glyph.height();
               float u1 = (float)glyph.textureWidth() / owner.getWidth();
               float v1 = (float)glyph.textureHeight() / owner.getHeight();
               float u2 = (float)(glyph.textureWidth() + glyph.width()) / owner.getWidth();
               float v2 = (float)(glyph.textureHeight() + glyph.height()) / owner.getHeight();
               bufferBuilder.addVertex(matrix4f, xo + 0.0F, yo + h, 0.0F).setColor(cr, cg, cb, a).setUv(u1, v2);
               bufferBuilder.addVertex(matrix4f, xo + w, yo + h, 0.0F).setColor(cr, cg, cb, a).setUv(u2, v2);
               bufferBuilder.addVertex(matrix4f, xo + w, yo + 0.0F, 0.0F).setColor(cr, cg, cb, a).setUv(u2, v1);
               bufferBuilder.addVertex(matrix4f, xo + 0.0F, yo + 0.0F, 0.0F).setColor(cr, cg, cb, a).setUv(u1, v1);
            }

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
            RenderSystem.disableBlend();
         }

         this.cache.clear();
      }

      stack.popPose();
   }

   public void drawCenteredString(PoseStack stack, String text, double x, double y, int color) {
      this.drawString(stack, text, x, y, color, false);
   }

   public void drawCenteredString(PoseStack stack, String text, double x, double y, int color, boolean shadow) {
      float brightnessMultiplier = shadow ? 0.25F : 1.0F;
      float r = (color >> 16 & 0xFF) / 255.0F * brightnessMultiplier;
      float g = (color >> 8 & 0xFF) / 255.0F * brightnessMultiplier;
      float b = (color & 0xFF) / 255.0F * brightnessMultiplier;
      float a = (color & 0xFF000000) != -16777216 ? 1.0F : (color >> 24 & 0xFF) / 255.0F * brightnessMultiplier;
      this.drawString(stack, text, (float)(x - this.getStringWidth(text) / 2.0F), (float)y, r, g, b, a, brightnessMultiplier);
   }

   public void drawCenteredString(PoseStack stack, String text, double x, double y, Color color) {
      this.drawString(stack, text, x, y, color, false);
   }

   public void drawCenteredString(PoseStack stack, String text, double x, double y, Color color, boolean shadow) {
      float brightnessMultiplier = shadow ? 0.25F : 1.0F;
      this.drawString(
         stack,
         text,
         (float)(x - this.getStringWidth(text) / 2.0F),
         (float)y,
         color.getRed() / 255.0F * brightnessMultiplier,
         color.getGreen() / 255.0F * brightnessMultiplier,
         color.getBlue() / 255.0F * brightnessMultiplier,
         color.getAlpha() / 255.0F,
         brightnessMultiplier
      );
   }

   public float getStringWidth(String text) {
      char[] c = stripControlCodes(text).toCharArray();
      float currentLine = 0.0F;
      float maxPreviousLines = 0.0F;

      for (char c1 : c) {
         if (c1 == '\n') {
            maxPreviousLines = Math.max(currentLine, maxPreviousLines);
            currentLine = 0.0F;
         } else {
            Glyph glyph = (Glyph)this.glyphs.computeIfAbsent(c1, g1 -> this.getGlyphFromChar(g1));
            float w = glyph == null ? 0.0F : glyph.width();
            currentLine += w / this.scale;
         }
      }

      return Math.max(currentLine, maxPreviousLines);
   }

   public float getStringHeight(String text) {
      char[] c = stripControlCodes(text).toCharArray();
      if (c.length == 0) {
         c = new char[]{' '};
      }

      float currentLine = 0.0F;
      float previous = 0.0F;

      for (char c1 : c) {
         if (c1 == '\n') {
            if (currentLine == 0.0F) {
               Glyph glyph = (Glyph)this.glyphs.computeIfAbsent(' ', g1 -> this.getGlyphFromChar(g1));
               currentLine = (float)glyph.height() / this.scale;
            }

            previous += currentLine;
            currentLine = 0.0F;
         } else {
            Glyph glyph = (Glyph)this.glyphs.computeIfAbsent(c1, g1 -> this.getGlyphFromChar(g1));
            float h = glyph == null ? 0.0F : glyph.height();
            currentLine = Math.max(h / this.scale, currentLine);
         }
      }

      return currentLine + previous;
   }

   public float getFontHeight() {
      return this.size;
   }

   private Glyph getGlyphFromChar(char c) {
      ObjectListIterator base = this.caches.iterator();

      while (base.hasNext()) {
         GlyphCache map = (GlyphCache)base.next();
         if (map.contains(c)) {
            return map.getGlyph(c);
         }
      }

      int basex = 256 * (int)Math.floor(c / 256.0);
      GlyphCache glyphCache = new GlyphCache(
         (char)basex,
         (char)(basex + 256),
         this.font,
         this.getGlyphIdentifier(),
         5,
         FontModule.getInstance().getAntiAlias(),
         FontModule.getInstance().getFractionalMetrics()
      );
      this.caches.add(glyphCache);
      return glyphCache.getGlyph(c);
   }

   @Override
   public void close() {
      try {
         ObjectListIterator e = this.caches.iterator();

         while (e.hasNext()) {
            GlyphCache cache1 = (GlyphCache)e.next();
            cache1.clear();
         }

         this.caches.clear();
         this.glyphs.clear();
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   public ResourceLocation getGlyphIdentifier() {
      return ResourceLocation.fromNamespaceAndPath("shoreline", "font/storage/" + HexRandom.generateRandomHex(32));
   }

   public int[] toRgbComponents(int color) {
      float r = color >> 16 & 0xFF;
      float g = color >> 8 & 0xFF;
      float b = color & 0xFF;
      float a = (color & 0xFF000000) != -16777216 ? 255.0F : color >> 24 & 0xFF;
      return new int[]{(int)r, (int)g, (int)b, (int)a};
   }

   private int getColorFromCode(char code) {
      return switch (code) {
         case '0' -> Color.BLACK.getRGB();
         case '1' -> -16777046;
         case '2' -> -16733696;
         case '3' -> -16733526;
         case '4' -> -5636096;
         case '5' -> -5635926;
         case '6' -> -22016;
         case '7' -> -5592406;
         case '8' -> -11184811;
         case '9' -> -11184641;
         default -> -1;
         case 'a' -> -11141291;
         case 'b' -> -11141121;
         case 'c' -> -43691;
         case 'd' -> -43521;
         case 'e' -> -171;
         case 'f' -> -1;
         case 'g' -> SocialsModule.getInstance().getFriendRGB();
         case 's' -> ColorsModule.getInstance().getRGB();
      };
   }

   public record CharLocation(float x, float y, float r, float g, float b, Glyph glyph) {
   }
}

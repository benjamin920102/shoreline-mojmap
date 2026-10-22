package net.shoreline.client.api.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.mixin.accessor.AccessorNativeImage;
import net.shoreline.client.util.Globals;
import org.lwjgl.system.MemoryUtil;

public class GlyphCache implements Globals {
   private final char start;
   private final char end;
   private final Font font;
   private final ResourceLocation id;
   private final int padding;
   private int width;
   private int height;
   private final Char2ObjectArrayMap<Glyph> glyphs = new Char2ObjectArrayMap();
   private boolean generated;
   private final boolean antiAlias;
   private final boolean fractionalMetrics;

   public GlyphCache(char from, char to, Font font, ResourceLocation id, int padding, boolean antiAlias, boolean fractionalMetrics) {
      this.start = from;
      this.end = to;
      this.font = font;
      this.id = id;
      this.padding = padding;
      this.antiAlias = antiAlias;
      this.fractionalMetrics = fractionalMetrics;
   }

   public Glyph getGlyph(char c) {
      if (!this.generated) {
         this.createBitmap();
      }

      return (Glyph)this.glyphs.get(c);
   }

   public void clear() {
      mc.getTextureManager().release(this.id);
      this.glyphs.clear();
      this.generated = false;
   }

   public boolean contains(char c) {
      return c >= this.start && c < this.end;
   }

   public void createBitmap() {
      if (!this.generated) {
         List<Glyph> glyphs1 = new ArrayList<>();
         int range = this.end - this.start - 1;
         int ceiling = (int)(Math.ceil(Math.sqrt(range)) * 1.5);
         int cached = 0;
         int charX = 0;
         int maxX = 0;
         int maxY = 0;
         int currX = 0;
         int currY = 0;
         int currentRowMaxY = 0;

         for (FontRenderContext frc = new FontRenderContext(new AffineTransform(), this.antiAlias, this.fractionalMetrics); cached <= range; charX++) {
            char currentChar = (char)(this.start + cached);
            Rectangle2D stringBounds = this.font.getStringBounds(String.valueOf(currentChar), frc);
            int width = (int)Math.ceil(stringBounds.getWidth());
            int height = (int)Math.ceil(stringBounds.getHeight());
            cached++;
            maxX = Math.max(maxX, currX + width);
            maxY = Math.max(maxY, currY + height);
            if (charX >= ceiling) {
               currX = 0;
               currY += currentRowMaxY + this.padding;
               charX = 0;
               currentRowMaxY = 0;
            }

            currentRowMaxY = Math.max(currentRowMaxY, height);
            glyphs1.add(new Glyph(currX, currY, width, height, currentChar, this));
            currX += width + this.padding;
         }

         BufferedImage bufferedImage = new BufferedImage(Math.max(maxX + this.padding, 1), Math.max(maxY + this.padding, 1), 2);
         this.width = bufferedImage.getWidth();
         this.height = bufferedImage.getHeight();
         Graphics2D g2d = bufferedImage.createGraphics();
         g2d.setColor(new Color(255, 255, 255, 0));
         g2d.fillRect(0, 0, this.width, this.height);
         g2d.setColor(Color.WHITE);
         g2d.setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS,
            this.fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF
         );
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, this.antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
         g2d.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, this.antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
         );

         for (Glyph glyph : glyphs1) {
            g2d.setFont(this.font);
            FontMetrics fontMetrics = g2d.getFontMetrics();
            g2d.drawString(String.valueOf(glyph.value()), glyph.textureWidth(), glyph.textureHeight() + fontMetrics.getAscent());
            this.glyphs.put(glyph.value(), glyph);
         }

         this.registerTexture(this.id, bufferedImage);
         this.generated = true;
      }
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public void registerTexture(ResourceLocation identifier, BufferedImage bufferedImage) {
      try {
         int imageWidth = bufferedImage.getWidth();
         int imageHeight = bufferedImage.getHeight();
         NativeImage image = new NativeImage(Format.RGBA, imageWidth, imageHeight, false);
         long ptr = ((AccessorNativeImage)(Object)image).hookGetPointer();
         IntBuffer backingBuffer = MemoryUtil.memIntBuffer(ptr, image.getWidth() * image.getHeight());
         WritableRaster raster = bufferedImage.getRaster();
         ColorModel colorModel = bufferedImage.getColorModel();
         int bands = raster.getNumBands();
         int dataType = raster.getDataBuffer().getDataType();

         Object dataBuffer = switch (dataType) {
            case 0 -> new byte[bands];
            case 1 -> new short[bands];
            default -> throw new IllegalArgumentException("Unknown data buffer type: " + dataType);
            case 3 -> new int[bands];
            case 4 -> new float[bands];
            case 5 -> new double[bands];
         };

         for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
               raster.getDataElements(x, y, dataBuffer);
               int a = colorModel.getAlpha(dataBuffer);
               int r = colorModel.getRed(dataBuffer);
               int g = colorModel.getGreen(dataBuffer);
               int b = colorModel.getBlue(dataBuffer);
               int argb = a << 24 | b << 16 | g << 8 | r;
               backingBuffer.put(argb);
            }
         }

         DynamicTexture texture = new DynamicTexture(image);
         texture.upload();
         texture.setFilter(true, true);
         if (RenderSystem.isOnRenderThread()) {
            mc.getTextureManager().register(identifier, texture);
         } else {
            RenderSystem.recordRenderCall(() -> mc.getTextureManager().register(identifier, texture));
         }
      } catch (Throwable var21) {
         var21.printStackTrace();
      }
   }
}

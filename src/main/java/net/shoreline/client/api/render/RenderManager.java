package net.shoreline.client.api.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.shoreline.client.impl.gui.click.component.ScissorStack;
import net.shoreline.client.impl.module.client.FontModule;
import net.shoreline.client.impl.module.render.NametagsModule;
import net.shoreline.client.init.Fonts;
import net.shoreline.client.mixin.accessor.AccessorTextRenderer;
import net.shoreline.client.mixin.accessor.AccessorWorldRenderer;
import net.shoreline.client.util.Globals;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class RenderManager implements Globals {
   public static final Tesselator TESSELLATOR = RenderSystem.renderThreadTesselator();
   public static final ScissorStack SCISSOR_STACK = new ScissorStack();

   public static void post(Runnable callback) {
      RenderBuffers.post(callback);
   }

   public static void renderBox(PoseStack matrices, BlockPos p, int color) {
      renderBox(matrices, new AABB(p), color);
   }

   public static void renderBox(PoseStack matrices, AABB box, int color) {
      if (isFrustumVisible(box)) {
         matrices.pushPose();
         drawBox(matrices, box, color);
         matrices.popPose();
      }
   }

   public static void drawBox(PoseStack matrices, AABB box, int color) {
      drawBox(matrices, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
   }

   public static void drawBox(PoseStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      RenderBuffers.QUADS.begin(matrices);
      RenderBuffers.QUADS.color(color);
      RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x2, y1, z1).vertex(x2, y1, z2).vertex(x1, y1, z2);
      RenderBuffers.QUADS.vertex(x1, y2, z1).vertex(x1, y2, z2).vertex(x2, y2, z2).vertex(x2, y2, z1);
      RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x1, y2, z1).vertex(x2, y2, z1).vertex(x2, y1, z1);
      RenderBuffers.QUADS.vertex(x2, y1, z1).vertex(x2, y2, z1).vertex(x2, y2, z2).vertex(x2, y1, z2);
      RenderBuffers.QUADS.vertex(x1, y1, z2).vertex(x2, y1, z2).vertex(x2, y2, z2).vertex(x1, y2, z2);
      RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x1, y1, z2).vertex(x1, y2, z2).vertex(x1, y2, z1);
      RenderBuffers.QUADS.end();
   }

   public static void renderSide(PoseStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, Direction direction, int color) {
      matrices.pushPose();
      drawSide(matrices, x1, y1, z1, x2, y2, z2, direction, color);
      matrices.popPose();
   }

   public static void renderSide(PoseStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, Direction direction, int color) {
      matrices.pushPose();
      drawSide(matrices, x1, y1, z1, x2, y2, z2, direction, color);
      matrices.popPose();
   }

   public static void drawSide(PoseStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, Direction direction, int color) {
      RenderBuffers.QUADS.begin(matrices);
      RenderBuffers.QUADS.color(color);
      if (direction.getAxis().isVertical()) {
         RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x2, y1, z1).vertex(x2, y1, z2).vertex(x1, y1, z2);
      } else if (direction != Direction.NORTH && direction != Direction.SOUTH) {
         RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x1, y1, z2).vertex(x1, y2, z2).vertex(x1, y2, z1);
      } else {
         RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x1, y2, z1).vertex(x2, y2, z1).vertex(x2, y1, z1);
      }

      RenderBuffers.QUADS.end();
   }

   public static void renderPlane(PoseStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      matrices.pushPose();
      drawPlane(matrices, x1, y1, z1, x2, y2, z2, color);
      matrices.popPose();
   }

   public static void drawPlane(PoseStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      RenderBuffers.QUADS.begin(matrices);
      RenderBuffers.QUADS.color(color);
      RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x1, y2, z1).vertex(x2, y2, z2).vertex(x2, y1, z2);
      RenderBuffers.QUADS.end();
   }

   public static void renderBoundingCross(PoseStack matrices, BlockPos p, float width, int color) {
      renderBoundingCross(matrices, new AABB(p), width, color);
   }

   public static void renderBoundingCross(PoseStack matrices, AABB box, float width, int color) {
      if (isFrustumVisible(box)) {
         matrices.pushPose();
         RenderSystem.lineWidth(width);
         drawBoundingCross(matrices, box, color);
         matrices.popPose();
      }
   }

   public static void drawBoundingCross(PoseStack matrices, AABB box, int color) {
      drawBoundingCross(matrices, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
   }

   public static void drawBoundingCross(PoseStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      RenderBuffers.LINES.begin(matrices);
      RenderBuffers.LINES.color(color);
      RenderBuffers.LINES.vertexLine(x1, y1, z1, x2, y1, z2);
      RenderBuffers.LINES.vertexLine(x2, y1, z1, x1, y1, z2);
      RenderBuffers.LINES.end();
   }

   public static void renderBoundingBox(PoseStack matrices, BlockPos p, float width, int color) {
      renderBoundingBox(matrices, new AABB(p), width, color);
   }

   public static void renderBoundingBox(PoseStack matrices, AABB box, float width, int color) {
      if (isFrustumVisible(box)) {
         matrices.pushPose();
         RenderSystem.lineWidth(width);
         drawBoundingBox(matrices, box, color);
         matrices.popPose();
      }
   }

   public static void drawBoundingBox(PoseStack matrices, AABB box, int color) {
      drawBoundingBox(matrices, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
   }

   public static void drawBoundingBox(PoseStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      RenderBuffers.LINES.begin(matrices);
      RenderBuffers.LINES.color(color);
      double dx = x2 - x1;
      double dy = y2 - y1;
      double dz = z2 - z1;
      if (dy == 0.0) {
         RenderBuffers.LINES.vertexLine(x1, y1, z1, x2, y1, z1);
         RenderBuffers.LINES.vertexLine(x1, y1, z1, x1, y1, z2);
         RenderBuffers.LINES.vertexLine(x2, y1, z2, x1, y1, z2);
         RenderBuffers.LINES.vertexLine(x2, y1, z2, x2, y1, z1);
         RenderBuffers.LINES.end();
      } else {
         Shapes.box(0.0, 0.0, 0.0, dx, dy, dz)
            .forAllEdges(
               (minX, minY, minZ, maxX, maxY, maxZ) -> RenderBuffers.LINES.vertexLine(minX + x1, minY + y1, minZ + z1, maxX + x1, maxY + y1, maxZ + z1)
            );
         RenderBuffers.LINES.end();
      }
   }

   public static void renderLine(PoseStack matrices, Vec3 s, Vec3 d, float width, int color) {
      renderLine(matrices, s.x, s.y, s.z, d.x, d.y, d.z, width, color);
   }

   public static void renderLine(PoseStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, float width, int color) {
      matrices.pushPose();
      RenderSystem.lineWidth(width);
      drawLine(matrices, x1, y1, z1, x2, y2, z2, color);
      matrices.popPose();
   }

   public static void drawLine(PoseStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      matrices.pushPose();
      RenderBuffers.LINES.begin(matrices);
      RenderBuffers.LINES.color(color);
      RenderBuffers.LINES.vertexLine(x1, y1, z1, x2, y2, z2);
      RenderBuffers.LINES.end();
      matrices.popPose();
   }

   public static void renderSign(String text, Vec3 pos, int color) {
      renderSign(text, pos.x(), pos.y(), pos.z(), color);
   }

   public static void renderSign(String text, Vec3 pos, float scaling, int color) {
      renderSign(text, pos.x(), pos.y(), pos.z(), scaling, color);
   }

   public static void renderSign(String text, double x, double y, double z, int color) {
      Camera camera = mc.gameRenderer.getMainCamera();
      Vec3 pos = camera.getPosition();
      double dist = Math.sqrt(pos.distanceToSqr(x, y, z));
      float scaling = (float)(0.0018F + NametagsModule.getInstance().getScaling() * dist);
      if (dist <= 8.0) {
         scaling = 0.0245F;
      }

      renderSign(text, x, y, z, scaling, color);
   }

   public static void renderSign(String text, double x, double y, double z, float scaling, int color) {
      Camera camera = mc.gameRenderer.getMainCamera();
      Vec3 pos = camera.getPosition();
      PoseStack matrices = new PoseStack();
      matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
      matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
      matrices.translate(x - pos.x(), y - pos.y(), z - pos.z());
      matrices.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
      matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
      matrices.scale(-scaling, -scaling, -1.0F);
      float hwidth = mc.font.width(text) / 2.0F;
      post(
         () -> {
            GL11.glDepthFunc(519);
            if (FontModule.getInstance().isEnabled()) {
               Fonts.CLIENT_UNSCALED.drawStringWithShadow(matrices, text, -hwidth, 0.0, color);
            } else {
               BufferSource vertexConsumers = mc.renderBuffers().bufferSource();
               ((AccessorTextRenderer)mc.font)
                  .hookDrawLayer(
                     text, -hwidth, 0.0F, Font.adjustColor(color), true, matrices.last().pose(), vertexConsumers, DisplayMode.SEE_THROUGH, 0, 15728880
                  );
               vertexConsumers.endBatch();
               ((AccessorTextRenderer)mc.font)
                  .hookDrawLayer(
                     text, -hwidth, 0.0F, Font.adjustColor(color), false, matrices.last().pose(), vertexConsumers, DisplayMode.SEE_THROUGH, 0, 15728880
                  );
               vertexConsumers.endBatch();
            }

            GL11.glDepthFunc(515);
         }
      );
   }

   public static boolean isFrustumVisible(AABB box) {
      return ((AccessorWorldRenderer)mc.levelRenderer).getFrustum().isVisible(box);
   }

   public static void rect(PoseStack matrices, double x1, double y1, double x2, double y2, int color) {
      rect(matrices, x1, y1, x2, y2, 0.0, color);
   }

   public static void borderedRect(PoseStack matrices, double x1, double y1, double x2, double y2, int borderColor, double thickness) {
      rect(matrices, x1 - thickness, y1 - thickness, thickness, y2 + thickness * 2.0, borderColor);
      rect(matrices, x1 + x2, y1 - thickness, thickness, y2 + thickness * 2.0, borderColor);
      rect(matrices, (float)x1, (float)(y1 - thickness - 1.0), (float)x2, (float)thickness, borderColor);
      rect(matrices, (float)x1, (float)(y1 + y2 + 1.0), (float)x2, (float)thickness, borderColor);
   }

   public static void borderedRectLine(PoseStack matrices, double x1, double y1, double x2, double y2, int borderColor) {
      rectLine(matrices, x1, y1, 0.0, y2, borderColor);
      rectLine(matrices, x1 + x2, y1, 0.0, y2, borderColor);
      rectLine(matrices, x1, y1, x2, 0.0, borderColor);
      rectLine(matrices, x1, y1 + y2, x2, 0.0, borderColor);
   }

   public static void rect(PoseStack matrices, double x1, double y1, double x2, double y2, double z, int color) {
      x2 += x1;
      y2 += y1;
      Matrix4f matrix4f = matrices.last().pose();
      if (x1 < x2) {
         double i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         double i = y1;
         y1 = y2;
         y2 = i;
      }

      float f = ARGB32.alpha(color) / 255.0F;
      float g = ARGB32.red(color) / 255.0F;
      float h = ARGB32.green(color) / 255.0F;
      float j = ARGB32.blue(color) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      BufferBuilder bufferBuilder = TESSELLATOR.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertex(matrix4f, (float)x1, (float)y1, (float)z).setColor(g, h, j, f);
      bufferBuilder.addVertex(matrix4f, (float)x1, (float)y2, (float)z).setColor(g, h, j, f);
      bufferBuilder.addVertex(matrix4f, (float)x2, (float)y2, (float)z).setColor(g, h, j, f);
      bufferBuilder.addVertex(matrix4f, (float)x2, (float)y1, (float)z).setColor(g, h, j, f);
      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
      RenderSystem.disableBlend();
   }

   public static void rectLine(PoseStack matrices, double x1, double y1, double x2, double y2, int color) {
      x2 += x1;
      y2 += y1;
      Matrix4f matrix4f = matrices.last().pose();
      if (x1 < x2) {
         double i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         double i = y1;
         y1 = y2;
         y2 = i;
      }

      float f = ARGB32.alpha(color) / 255.0F;
      float g = ARGB32.red(color) / 255.0F;
      float h = ARGB32.green(color) / 255.0F;
      float j = ARGB32.blue(color) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      BufferBuilder bufferBuilder = TESSELLATOR.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertex(matrix4f, (float)x1, (float)y1, 0.0F).setColor(g, h, j, f);
      bufferBuilder.addVertex(matrix4f, (float)x2, (float)y2, 0.0F).setColor(g, h, j, f);
      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
      RenderSystem.disableBlend();
   }

   public static void rectGradient(PoseStack matrices, double x1, double y1, double x2, double y2, int color, int color1) {
      x2 += x1;
      y2 += y1;
      if (x1 < x2) {
         double i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         double i = y1;
         y1 = y2;
         y2 = i;
      }

      fillGradientQuad(matrices, (float)x1, (float)y1, (float)x2, (float)y2, color1, color, true);
   }

   public static void fillGradientQuad(GuiGraphics context, float x1, float y1, float x2, float y2, int startColor, int endColor, boolean sideways) {
      fillGradientQuad(context.pose(), x1, y1, x2, y2, startColor, endColor, sideways);
   }

   public static void fillGradientQuad(PoseStack matrixStack, float x1, float y1, float x2, float y2, int startColor, int endColor, boolean sideways) {
      float f = (startColor >> 24 & 0xFF) / 255.0F;
      float f1 = (startColor >> 16 & 0xFF) / 255.0F;
      float f2 = (startColor >> 8 & 0xFF) / 255.0F;
      float f3 = (startColor & 0xFF) / 255.0F;
      float f4 = (endColor >> 24 & 0xFF) / 255.0F;
      float f5 = (endColor >> 16 & 0xFF) / 255.0F;
      float f6 = (endColor >> 8 & 0xFF) / 255.0F;
      float f7 = (endColor & 0xFF) / 255.0F;
      Matrix4f posMatrix = matrixStack.last().pose();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      if (sideways) {
         bufferBuilder.addVertex(posMatrix, x1, y1, 0.0F).setColor(f1, f2, f3, f);
         bufferBuilder.addVertex(posMatrix, x1, y2, 0.0F).setColor(f1, f2, f3, f);
         bufferBuilder.addVertex(posMatrix, x2, y2, 0.0F).setColor(f5, f6, f7, f4);
         bufferBuilder.addVertex(posMatrix, x2, y1, 0.0F).setColor(f5, f6, f7, f4);
      } else {
         bufferBuilder.addVertex(posMatrix, x2, y1, 0.0F).setColor(f1, f2, f3, f);
         bufferBuilder.addVertex(posMatrix, x1, y1, 0.0F).setColor(f1, f2, f3, f);
         bufferBuilder.addVertex(posMatrix, x1, y2, 0.0F).setColor(f5, f6, f7, f4);
         bufferBuilder.addVertex(posMatrix, x2, y2, 0.0F).setColor(f5, f6, f7, f4);
      }

      BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
      RenderSystem.disableBlend();
   }

   public static void rectTextured(
      PoseStack matrix,
      ResourceLocation identifier,
      float x0,
      float x1,
      float y0,
      float y1,
      float z,
      float u0,
      float u1,
      float v0,
      float v1,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      Matrix4f matrix4f = matrix.last().pose();
      RenderSystem.setShaderTexture(0, identifier);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.enableBlend();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      buffer.addVertex(matrix4f, x0, y0, z).setColor(red, green, blue, alpha).setUv(u0, v0);
      buffer.addVertex(matrix4f, x0, y1, z).setColor(red, green, blue, alpha).setUv(u0, v1);
      buffer.addVertex(matrix4f, x1, y1, z).setColor(red, green, blue, alpha).setUv(u1, v1);
      buffer.addVertex(matrix4f, x1, y0, z).setColor(red, green, blue, alpha).setUv(u1, v0);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      RenderSystem.disableBlend();
   }

   public static void enableScissor(double x1, double y1, double x2, double y2) {
      x1 = Math.floor(x1);
      y1 = Math.floor(y1);
      x2 = Math.ceil(x2);
      y2 = Math.ceil(y2);
      setScissor(SCISSOR_STACK.push(new ScreenRectangle((int)x1, (int)y1, (int)(x2 - x1), (int)(y2 - y1))));
   }

   public static void disableScissor() {
      setScissor(SCISSOR_STACK.pop());
   }

   private static void setScissor(ScreenRectangle rect) {
      if (rect != null) {
         Window window = mc.getWindow();
         int i = window.getHeight();
         double d = window.getGuiScale();
         double e = rect.left() * d;
         double f = i - rect.bottom() * d;
         double g = rect.width() * d;
         double h = rect.height() * d;
         RenderSystem.enableScissor((int)e, (int)f, Math.max(0, (int)g), Math.max(0, (int)h));
      } else {
         RenderSystem.disableScissor();
      }
   }

   public static void renderText(GuiGraphics context, String text, float x, float y, int color) {
      if (FontModule.getInstance().isEnabled() && Fonts.CLIENT != null) {
         Fonts.CLIENT.drawStringWithShadow(context.pose(), text, x, y, color);
      } else {
         context.drawString(mc.font, text, (int)x, (int)y, color, true);
      }
   }

   public static int textWidth(String text) {
      return FontModule.getInstance().isEnabled() && Fonts.CLIENT != null ? (int)Fonts.CLIENT.getStringWidth(text) : mc.font.width(text);
   }

   public static int textHeight() {
      return FontModule.getInstance().isEnabled() && Fonts.CLIENT != null ? (int)Fonts.CLIENT.getFontHeight() : 9;
   }
}

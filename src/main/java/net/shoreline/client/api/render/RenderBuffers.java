package net.shoreline.client.api.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL11;

public class RenderBuffers {
   public static final RenderBuffers.Buffer QUADS = new RenderBuffers.Buffer(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
   public static final RenderBuffers.Buffer TEXTURE_QUADS = new RenderBuffers.Buffer(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
   public static final RenderBuffers.Buffer LINES = new RenderBuffers.Buffer(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
   private static final List<Runnable> postRenderCallbacks = new ArrayList<>();
   private static boolean isSetup = false;

   public static void preRender() {
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.disableDepthTest();
      isSetup = true;
   }

   public static void postRender() {
      isSetup = false;
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      GL11.glDisable(2848);

      for (Runnable callback : postRenderCallbacks) {
         callback.run();
      }

      postRenderCallbacks.clear();
   }

   public static void post(Runnable callback) {
      if (isSetup) {
         postRenderCallbacks.add(callback);
      } else {
         callback.run();
      }
   }

   private static Matrix4d toMatrix4d(Matrix4f matrix4f) {
      return new Matrix4d(
         matrix4f.m00(),
         matrix4f.m01(),
         matrix4f.m02(),
         matrix4f.m03(),
         matrix4f.m10(),
         matrix4f.m11(),
         matrix4f.m12(),
         matrix4f.m13(),
         matrix4f.m20(),
         matrix4f.m21(),
         matrix4f.m22(),
         matrix4f.m23(),
         matrix4f.m30(),
         matrix4f.m31(),
         matrix4f.m32(),
         matrix4f.m33()
      );
   }

   public static class Buffer {
      public BufferBuilder buffer;
      private final Mode drawMode;
      private final VertexFormat vertexFormat;
      private Matrix4d positionMatrix;
      private Matrix3f normalMatrix;
      private int color;

      public Buffer(Mode drawMode, VertexFormat vertexFormat) {
         this.drawMode = drawMode;
         this.vertexFormat = vertexFormat;
         this.color = -1;
      }

      public void begin(PoseStack stack) {
         this.updateMatrices(stack);
         this.buffer = Tesselator.getInstance().begin(this.drawMode, this.vertexFormat);
      }

      public void updateMatrices(PoseStack stack) {
         this.positionMatrix = RenderBuffers.toMatrix4d(stack.last().pose());
         this.normalMatrix = stack.last().normal();
         Vec3 pos = Minecraft.getInstance().getBlockEntityRenderDispatcher().camera.getPosition();
         this.positionMatrix.translate(-pos.x, -pos.y, -pos.z);
      }

      public void end() {
         this.draw();
      }

      public RenderBuffers.Buffer vertex(double x, double y, double z) {
         if (this.buffer == null) {
            return this;
         } else {
            Vector4d vector4d = this.positionMatrix.transform(new Vector4d(x, y, z, 1.0));
            this.buffer
               .addVertex((float)vector4d.x(), (float)vector4d.y(), (float)vector4d.z())
               .setColor(ARGB32.red(this.color), ARGB32.green(this.color), ARGB32.blue(this.color), ARGB32.alpha(this.color));
            return this;
         }
      }

      public RenderBuffers.Buffer vertexTex(double x, double y, double z, float u, float v) {
         if (this.buffer == null) {
            return this;
         } else {
            Vector4d vector4d = this.positionMatrix.transform(new Vector4d(x, y, z, 1.0));
            this.buffer
               .addVertex((float)vector4d.x(), (float)vector4d.y(), (float)vector4d.z())
               .setUv(u, v)
               .setColor(ARGB32.red(this.color), ARGB32.green(this.color), ARGB32.blue(this.color), ARGB32.alpha(this.color));
            return this;
         }
      }

      public RenderBuffers.Buffer vertexLine(double x1, double y1, double z1, double x2, double y2, double z2) {
         if (this.buffer == null) {
            return this;
         } else {
            float k = (float)(x2 - x1);
            float l = (float)(y2 - y1);
            float m = (float)(z2 - z1);
            float n = Mth.sqrt(k * k + l * l + m * m);
            k /= n;
            l /= n;
            m /= n;
            Vector3f vector3f = this.normalMatrix.transform(k, l, m, new Vector3f()).normalize();
            Vector4d vector4d = this.positionMatrix.transform(new Vector4d(x1, y1, z1, 1.0));
            this.buffer
               .addVertex((float)vector4d.x(), (float)vector4d.y(), (float)vector4d.z())
               .setNormal(vector3f.x, vector3f.y, vector3f.z)
               .setColor(ARGB32.red(this.color), ARGB32.green(this.color), ARGB32.blue(this.color), ARGB32.alpha(this.color));
            Vector4d vector4d2 = this.positionMatrix.transform(new Vector4d(x2, y2, z2, 1.0));
            this.buffer
               .addVertex((float)vector4d2.x(), (float)vector4d2.y(), (float)vector4d2.z())
               .setNormal(vector3f.x, vector3f.y, vector3f.z)
               .setColor(ARGB32.red(this.color), ARGB32.green(this.color), ARGB32.blue(this.color), ARGB32.alpha(this.color));
            return this;
         }
      }

      public void color(int color) {
         this.color = color;
      }

      public void draw() {
         if (this.buffer != null) {
            if (this.vertexFormat == DefaultVertexFormat.POSITION_COLOR_NORMAL) {
               RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            } else if (this.vertexFormat == DefaultVertexFormat.POSITION_COLOR) {
               RenderSystem.setShader(GameRenderer::getPositionColorShader);
            } else if (this.vertexFormat == DefaultVertexFormat.POSITION_TEX) {
               RenderSystem.setShader(GameRenderer::getPositionTexShader);
            }

            MeshData builtBuffer = this.buffer.build();
            if (builtBuffer != null) {
               BufferUploader.drawWithShader(builtBuffer);
               this.buffer = null;
            }
         }
      }
   }
}

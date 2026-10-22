package net.shoreline.client.api.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Calling these outside of {@link net.shoreline.client.impl.event.render.RenderWorldEvent} will blow everything up
 */
public class RenderBuffers
{
    public static final Buffer QUADS = new Buffer(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    public static final Buffer TEXTURE_QUADS = new Buffer(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
    public static final Buffer LINES = new Buffer(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
    private static final List<Runnable> postRenderCallbacks = new ArrayList<>();
    private static boolean isSetup = false;

    public static void preRender()
    {
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        isSetup = true;
    }

    public static void postRender()
    {
        // QUADS.draw();
        // LINES.draw();
        isSetup = false;

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        for (Runnable callback : postRenderCallbacks)
        {
            callback.run();
        }
        postRenderCallbacks.clear();
    }

    public static void post(Runnable callback)
    {
        if (isSetup)
        {
            postRenderCallbacks.add(callback);
        }
        else
        {
            callback.run();
        }
    }

    private static Matrix4d toMatrix4d(Matrix4f matrix4f)
    {
        return new Matrix4d(matrix4f.m00(), matrix4f.m01(), matrix4f.m02(), matrix4f.m03(),
                matrix4f.m10(), matrix4f.m11(), matrix4f.m12(), matrix4f.m13(),
                matrix4f.m20(), matrix4f.m21(), matrix4f.m22(), matrix4f.m23(),
                matrix4f.m30(), matrix4f.m31(), matrix4f.m32(), matrix4f.m33());
    }

    public static class Buffer
    {
        public BufferBuilder buffer;
        private final VertexFormat.Mode drawMode;
        private final VertexFormat vertexFormat;
        private Matrix4d positionMatrix;
        private Matrix3f normalMatrix;
        private int color;

        public Buffer(VertexFormat.Mode drawMode, VertexFormat vertexFormat)
        {
            this.drawMode = drawMode;
            this.vertexFormat = vertexFormat;
            this.color = -1;
        }

        public void begin(PoseStack stack)
        {
            updateMatrices(stack);
            buffer = Tesselator.getInstance().begin(drawMode, vertexFormat);
        }

        public void updateMatrices(PoseStack stack)
        {
            this.positionMatrix = toMatrix4d(stack.last().pose());
            this.normalMatrix = stack.last().normal();
            Vec3 pos = Minecraft.getInstance().getBlockEntityRenderDispatcher().camera.getPosition();
            positionMatrix.translate(-pos.x, -pos.y, -pos.z);
        }

        /**
         * Render in immediate mode if we're calling from outside of {@link net.shoreline.client.impl.event.render.RenderWorldEvent}
         */
        public void end()
        {
            draw();
        }

        public Buffer vertex(double x, double y, double z)
        {
            if (buffer == null)
            {
                return this;
            }
            Vector4d vector4d = positionMatrix.transform(new Vector4d(x, y, z, 1.0));
            this.buffer.addVertex((float) vector4d.x(), (float) vector4d.y(), (float) vector4d.z())
                    .setColor(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color));
            return this;
        }

        public Buffer vertexTex(double x, double y, double z, float u, float v)
        {
            if (buffer == null)
            {
                return this;
            }
            Vector4d vector4d = positionMatrix.transform(new Vector4d(x, y, z, 1.0));
            this.buffer.addVertex((float) vector4d.x(), (float) vector4d.y(), (float) vector4d.z()).setUv(u, v)
                    .setColor(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color));
            return this;
        }

        public Buffer vertexLine(double x1, double y1, double z1, double x2, double y2, double z2)
        {
            if (buffer == null)
            {
                return this;
            }
            float k = (float)(x2 - x1);
            float l = (float)(y2 - y1);
            float m = (float)(z2 - z1);
            float n = Mth.sqrt(k * k + l * l + m * m);
            k /= n;
            l /= n;
            m /= n;
            Vector3f vector3f = normalMatrix.transform(k, l, m, new Vector3f()).normalize();
            Vector4d vector4d = positionMatrix.transform(new Vector4d(x1, y1, z1, 1.0));
            this.buffer.addVertex((float) vector4d.x(), (float) vector4d.y(), (float) vector4d.z()).setNormal(vector3f.x, vector3f.y, vector3f.z)
                    .setColor(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color));
            Vector4d vector4d2 = positionMatrix.transform(new Vector4d(x2, y2, z2, 1.0));
            this.buffer.addVertex((float) vector4d2.x(), (float) vector4d2.y(), (float) vector4d2.z()).setNormal(vector3f.x, vector3f.y, vector3f.z)
                    .setColor(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color));
            return this;
        }

        public void color(int color)
        {
            this.color = color;
        }

        public void draw()
        {
            if (buffer == null)
            {
                return;
            }
            if (vertexFormat == DefaultVertexFormat.POSITION_COLOR_NORMAL)
            {
                RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            }
            else if (vertexFormat == DefaultVertexFormat.POSITION_COLOR)
            {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
            }
            else if (vertexFormat == DefaultVertexFormat.POSITION_TEX)
            {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
            }

            MeshData builtBuffer = this.buffer.build();
            if (builtBuffer != null)
            {
                BufferUploader.drawWithShader(builtBuffer);
                buffer = null;
            }
        }
    }
}
package net.shoreline.client.impl.gui.click.component;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.gui.click.ClickGuiScreen;
import net.shoreline.client.impl.module.client.ClickGuiModule;
import net.shoreline.client.util.Globals;
import org.joml.Matrix4f;

import java.util.function.BiConsumer;

/**
 * @author linus
 * @see Drawable
 * @see Interactable
 * @since 1.0
 */
public abstract class Component implements Drawable, Globals
{
    //
    protected float x, y, width, height;

    /**
     * @param context
     * @param mouseX
     * @param mouseY
     * @param delta
     */
    @Override
    public abstract void render(GuiGraphics context, float mouseX, float mouseY, float delta);

    protected void drawStringScaled(GuiGraphics context, String string, float x, float y, int color)
    {
        context.pose().scale(ClickGuiModule.CLICK_GUI_SCALE, ClickGuiModule.CLICK_GUI_SCALE, 0.0f);
        float j = 1.0f / ClickGuiModule.CLICK_GUI_SCALE;
        RenderManager.renderText(context, string, x * j, y * j, color);
        context.pose().scale(j, j, 0.0f);
    }

    /**
     * @param context
     * @param color
     */
    protected void rect(GuiGraphics context, int color)
    {
        fill(context, x, y, width * ClickGuiModule.CLICK_GUI_SCALE, height * ClickGuiModule.CLICK_GUI_SCALE, color);
    }

    /**
     * @param context
     * @param color1
     */
    protected void rectGradient(GuiGraphics context, int color1, int color2)
    {
        fillGradient(context, x, y, width * ClickGuiModule.CLICK_GUI_SCALE, height * ClickGuiModule.CLICK_GUI_SCALE, color1, color2);
    }

    protected void scale(GuiGraphics context, float scale)
    {

    }

    protected void drawRoundedRect(GuiGraphics context, double x1, double y1,
                                   double x2, double y2, int color)
    {
        drawRoundedRect(context, x1, y1, x2, y2, 0, color);
    }

    protected void drawRoundedRect(GuiGraphics context, double x1, double y1,
                                   double x2, double y2, double z, int color)
    {
        fill(context, x1, y1, x2, y2, z, color);
        /*
        Matrix4f matrix4f = context.peek().getPositionMatrix();
        Shader shader = new Shader("rect.vert", "roundedrect.frag");
        shader.bind();
        float f = (float) ColorHelper.Argb.getAlpha(color) / 255.0f;
        float g = (float) ColorHelper.Argb.getRed(color) / 255.0f;
        float h = (float) ColorHelper.Argb.getGreen(color) / 255.0f;
        float j = (float) ColorHelper.Argb.getBlue(color) / 255.0f;
        BufferBuilder buffer = RenderManager.BUFFER;
        RenderSystem.enableBlend();
        // RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        buffer.begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix4f, (float) x1, (float) y1, (float) z)
                .color(g, h, j, f);
        buffer.vertex(matrix4f, (float) x1, (float) y2, (float) z)
                .color(g, h, j, f);
        buffer.vertex(matrix4f, (float) x2, (float) y2, (float) z)
                .color(g, h, j, f);
        buffer.vertex(matrix4f, (float) x2, (float) y1, (float) z)
                .color(g, h, j, f);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        shader.unbind();
         */
    }

    protected void drawCircle(GuiGraphics context, double x, double y,
                              double radius, int color)
    {
        drawCircle(context, x, y, 0, radius, color);
    }

    protected void drawCircle(GuiGraphics context, double x, double y,
                              double z, double radius, int color)
    {
        /*
        Matrix4f matrix4f = context.peek().getPositionMatrix();
        Shader shader = new Shader("rect.vert", "circle.frag");
        shader.bind();
        float f = (float) ColorHelper.Argb.getAlpha(color) / 255.0f;
        float g = (float) ColorHelper.Argb.getRed(color) / 255.0f;
        float h = (float) ColorHelper.Argb.getGreen(color) / 255.0f;
        float j = (float) ColorHelper.Argb.getBlue(color) / 255.0f;
        BufferBuilder buffer = RenderManager.BUFFER;
        RenderSystem.enableBlend();
        // RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        buffer.begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix4f, (float) (x - radius), (float) (y - radius),
                (float) z).color(g, h, j, f);
        buffer.vertex(matrix4f, (float) (x - radius), (float) (y + radius),
                        (float) z).color(g, h, j, f);
        buffer.vertex(matrix4f, (float) (x + radius), (float) (y + radius),
                        (float) z).color(g, h, j, f);
        buffer.vertex(matrix4f, (float) (x + radius), (float) (y - radius),
                (float) z).color(g, h, j, f);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        shader.unbind();
         */
    }

    protected void drawHorizontalLine(GuiGraphics context, double x1, double x2,
                                      double y, int color)
    {
        if (x2 < x1)
        {
            double i = x1;
            x1 = x2;
            x2 = i;
        }
        fill(context, x1, y, x1 + x2 + 1, y + 1, color);
    }

    protected void drawVerticalLine(GuiGraphics context, double x, double y1,
                                    double y2, int color)
    {
        if (y2 < y1)
        {
            double i = y1;
            y1 = y2;
            y2 = i;
        }
        fill(context, x, y1 + 1, x + 1, y1 + y2, color);
    }

    public void fill(GuiGraphics context, double x1, double y1, double x2,
                     double y2, int color)
    {
        fill(context, x1, y1, x2, y2, 0.0, color);
    }

    public void fill(GuiGraphics context, double x1, double y1, double x2,
                     double y2, double z, int color)
    {
        x2 += x1;
        y2 += y1;
        Matrix4f matrix4f = context.pose().last().pose();
        double i;
        if (x1 < x2)
        {
            i = x1;
            x1 = x2;
            x2 = i;
        }
        if (y1 < y2)
        {
            i = y1;
            y1 = y2;
            y2 = i;
        }
        float f = (float) FastColor.ARGB32.alpha(color) / 255.0f;
        float g = (float) FastColor.ARGB32.red(color) / 255.0f;
        float h = (float) FastColor.ARGB32.green(color) / 255.0f;
        float j = (float) FastColor.ARGB32.blue(color) / 255.0f;
        RenderSystem.enableBlend();
        if (RenderSystem.getShader() != GameRenderer.getPositionColorShader())
        {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
        }
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix4f, (float) x1, (float) y1, (float) z)
                .setColor(g, h, j, f);
        buffer.addVertex(matrix4f, (float) x1, (float) y2, (float) z)
                .setColor(g, h, j, f);
        buffer.addVertex(matrix4f, (float) x2, (float) y2, (float) z)
                .setColor(g, h, j, f);
        buffer.addVertex(matrix4f, (float) x2, (float) y1, (float) z)
                .setColor(g, h, j, f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    protected void fillGradient(GuiGraphics context, double startX,
                                double startY, double endX, double endY,
                                int colorStart, int colorEnd)
    {
        fillGradient(context, startX, startY, endX, endY, colorStart, colorEnd, 0);
    }

    protected void fillGradient(GuiGraphics context, double startX,
                                double startY, double endX, double endY,
                                int colorStart, int colorEnd, int z)
    {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillGradient(context.pose().last().pose(), buffer,
                startX, startY, endX, endY, z, colorStart, colorEnd);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    protected void fillGradient(Matrix4f matrix, BufferBuilder builder,
                                double x1, double y1, double x2,
                                double y2, double z, int colorStart,
                                int colorEnd)
    {
        x2 += x1;
        y2 += y1;
        double i2;
        if (x1 < x2)
        {
            i2 = x1;
            x1 = x2;
            x2 = i2;
        }
        if (y1 < y2)
        {
            i2 = y1;
            y1 = y2;
            y2 = i2;
        }
        float f = (float) FastColor.ARGB32.alpha(colorStart) / 255.0f;
        float g = (float) FastColor.ARGB32.red(colorStart) / 255.0f;
        float h = (float) FastColor.ARGB32.green(colorStart) / 255.0f;
        float i = (float) FastColor.ARGB32.blue(colorStart) / 255.0f;
        float j = (float) FastColor.ARGB32.alpha(colorEnd) / 255.0f;
        float k = (float) FastColor.ARGB32.red(colorEnd) / 255.0f;
        float l = (float) FastColor.ARGB32.green(colorEnd) / 255.0f;
        float m = (float) FastColor.ARGB32.blue(colorEnd) / 255.0f;
        builder.addVertex(matrix, (float) x1, (float) y1, (float) z)
                .setColor(k, l, m, j);
        builder.addVertex(matrix, (float) x1, (float) y2, (float) z)
                .setColor(k, l, m, j);
        builder.addVertex(matrix, (float) x2, (float) y2, (float) z)
                .setColor(g, h, i, f);
        builder.addVertex(matrix, (float) x2, (float) y1, (float) z)
                .setColor(g, h, i, f);
    }

    protected void fillGradientQuad(GuiGraphics context, float x1, float y1, float x2, float y2,
                                    int startColor, int endColor, boolean sideways)
    {
        float f = (float) (startColor >> 24 & 255) / 255.0F;
        float f1 = (float) (startColor >> 16 & 255) / 255.0F;
        float f2 = (float) (startColor >> 8 & 255) / 255.0F;
        float f3 = (float) (startColor & 255) / 255.0F;
        float f4 = (float) (endColor >> 24 & 255) / 255.0F;
        float f5 = (float) (endColor >> 16 & 255) / 255.0F;
        float f6 = (float) (endColor >> 8 & 255) / 255.0F;
        float f7 = (float) (endColor & 255) / 255.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f posMatrix = context.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        if (sideways)
        {
            bufferBuilder.addVertex(posMatrix, x1, y1, 0.0F).setColor(f1, f2, f3, f);
            bufferBuilder.addVertex(posMatrix, x1, y2, 0.0F).setColor(f1, f2, f3, f);
            bufferBuilder.addVertex(posMatrix, x2, y2, 0.0F).setColor(f5, f6, f7, f4);
            bufferBuilder.addVertex(posMatrix, x2, y1, 0.0F).setColor(f5, f6, f7, f4);
        }
        else
        {
            bufferBuilder.addVertex(posMatrix, x2, y1, 0.0F).setColor(f1, f2, f3, f);
            bufferBuilder.addVertex(posMatrix, x1, y1, 0.0F).setColor(f1, f2, f3, f);
            bufferBuilder.addVertex(posMatrix, x1, y2, 0.0F).setColor(f5, f6, f7, f4);
            bufferBuilder.addVertex(posMatrix, x2, y2, 0.0F).setColor(f5, f6, f7, f4);
        }
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }


    public void drawWithOutline(int x, int y, BiConsumer<Integer, Integer> renderAction)
    {
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        renderAction.accept(x + 1, y);
        renderAction.accept(x - 1, y);
        renderAction.accept(x, y + 1);
        renderAction.accept(x, y - 1);
        RenderSystem.defaultBlendFunc();
        renderAction.accept(x, y);
    }

    public void drawSprite(GuiGraphics context, int x, int y, int z, int width,
                           int height, TextureAtlasSprite sprite)
    {
        drawTexturedQuad(context.pose().last().pose(), x, x + width,
                y, y + height, z, sprite.getU0(), sprite.getU1(),
                sprite.getV0(), sprite.getV1());
    }

    public void drawSprite(GuiGraphics context, int x, int y, int z, int width,
                           int height, TextureAtlasSprite sprite, float red, float green,
                           float blue, float alpha)
    {
        drawTexturedQuad(context.pose().last().pose(), x, x + width,
                y, y + height, z, sprite.getU0(), sprite.getU1(),
                sprite.getV0(), sprite.getV1(), red, green, blue, alpha);
    }

    public void drawBorder(GuiGraphics context, double x, double y, double width,
                           double height, int color)
    {
        fill(context, x, y, x + width, y + 1, color);
        fill(context, x, y + height - 1, x + width, y + height, color);
        fill(context, x, y + 1, x + 1, y + height - 1, color);
        fill(context, x + width - 1, y + 1, x + width,
                y + height - 1, color);
    }

    public void drawTexture(GuiGraphics context, int x, int y, int u,
                            int v, int width, int height)
    {
        drawTexture(context, x, y, 0, (float) u, (float) v, width, height, 256, 256);
    }

    public void drawTexture(GuiGraphics context, int x, int y, int z,
                            float u, float v, int width, int height,
                            int textureWidth, int textureHeight)
    {
        drawTexture(context, x, x + width, y, y + height, z, width,
                height, u, v, textureWidth, textureHeight);
    }

    public void drawTexture(GuiGraphics context, float x, float y, float width,
                            float height, float u, float v, float regionWidth,
                            float regionHeight, float textureWidth,
                            float textureHeight)
    {
        drawTexture(context, x, x + width, y, y + height, 0,
                regionWidth, regionHeight, u, v, textureWidth, textureHeight);
    }

    public void drawTexture(GuiGraphics context, float x, float y, float u,
                            float v, float width, float height,
                            float textureWidth, float textureHeight)
    {
        drawTexture(context, x, y, width, height, u, v, width, height,
                textureWidth, textureHeight);
    }

    private void drawTexture(GuiGraphics context, float x0, float x1,
                             float y0, float y1, float z, float regionWidth,
                             float regionHeight, float u, float v,
                             float textureWidth, float textureHeight)
    {
        drawTexturedQuad(context.pose().last().pose(), x0, x1, y0, y1, z,
                (u + 0.0F) / (float) textureWidth,
                (u + (float) regionWidth) / (float) textureWidth,
                (v + 0.0F) / (float) textureHeight,
                (v + (float) regionHeight) / (float) textureHeight);
    }

    public void drawTexturedQuad(Matrix4f matrix, float x0, float x1, float y0,
                                  float y1, float z, float u0, float u1,
                                  float v0, float v1)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, (float) x0, (float) y0, (float) z)
                .setUv(u0, v0);
        buffer.addVertex(matrix, (float) x0, (float) y1, (float) z)
                .setUv(u0, v1);
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z)
                .setUv(u1, v1);
        buffer.addVertex(matrix, (float) x1, (float) y0, (float) z)
                .setUv(u1, v0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public void drawTexturedQuad(Matrix4f matrix, float x0, float x1,
                                  float y0, float y1, float z, float u0,
                                  float u1, float v0, float v1,
                                  float red, float green, float blue, float alpha)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(matrix, (float) x0, (float) y0, (float) z)
                .setColor(red, green, blue, alpha).setUv(u0, v0);
        buffer.addVertex(matrix, (float) x0, (float) y1, (float) z)
                .setColor(red, green, blue, alpha).setUv(u0, v1);
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z)
                .setColor(red, green, blue, alpha).setUv(u1, v1);
        buffer.addVertex(matrix, (float) x1, (float) y0, (float) z)
                .setColor(red, green, blue, alpha).setUv(u1, v0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    public float getX()
    {
        return x;
    }

    public float getY()
    {
        return y;
    }

    public float getHeight()
    {
        return height * ClickGuiModule.CLICK_GUI_SCALE;
    }

    public void setHeight(float height)
    {
        this.height = height;
    }

    public float getWidth()
    {
        return width * ClickGuiModule.CLICK_GUI_SCALE;
    }

    public void setWidth(float width)
    {
        this.width = width;
    }

    /**
     * @param xval
     * @param yval
     * @return
     */
    public boolean isWithin(double xval, double yval)
    {
        return isWithin((float) xval, (float) yval);
    }

    /**
     * Checks if a given value is between the width and the height
     *
     * @param xval The x-position of the value
     * @param yval The y-position of the value
     * @return Whether the given value is between the width and the height
     */
    public boolean isWithin(float xval, float yval)
    {
        return isMouseOver(xval, yval, x, y, width * ClickGuiModule.CLICK_GUI_SCALE, height * ClickGuiModule.CLICK_GUI_SCALE);
    }

    public boolean isMouseOver(double mx, double my, double x1, double y1,
                               double x2, double y2)
    {
        return mx >= x1 && mx <= x1 + x2 && my >= y1 && my <= y1 + y2;
    }

    /**
     * @param x
     * @param y
     */
    public void setPos(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * @param width
     * @param height
     */
    public void setDimensions(float width, float height)
    {
        setWidth(width);
        setHeight(height);
    }

    public void enableScissor(double x1, double y1, double x2, double y2)
    {
        x1 = Math.floor(x1);
        y1 = Math.floor(y1);
        x2 = Math.ceil(x2);
        y2 = Math.ceil(y2);
        setScissor(ClickGuiScreen.SCISSOR_STACK.push(new ScreenRectangle((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1))));
    }

    public void disableScissor()
    {
        setScissor(ClickGuiScreen.SCISSOR_STACK.pop());
    }

    private void setScissor(ScreenRectangle rect)
    {
        if (rect != null)
        {
            Window window = mc.getWindow();
            int i = window.getHeight();
            double d = window.getGuiScale();
            double e = (double) rect.left() * d;
            double f = (double) i - (double) rect.bottom() * d;
            double g = (double) rect.width() * d;
            double h = (double) rect.height() * d;
            RenderSystem.enableScissor((int) e, (int) f, Math.max(0, (int) g), Math.max(0, (int) h));
        }
        else
        {
            RenderSystem.disableScissor();
        }
    }
}

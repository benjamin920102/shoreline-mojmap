package net.shoreline.client.api.render.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.api.render.satin.ManagedShaderEffect;
import net.shoreline.client.api.render.satin.ShaderEffectManager;
import net.shoreline.client.impl.imixin.IPostEffectProcessor;
import net.shoreline.client.mixin.accessor.AccessorMultiPhase;
import net.shoreline.client.mixin.accessor.AccessorMultiPhaseParameters;
import net.shoreline.client.mixin.accessor.AccessorTextureBase;
import net.shoreline.client.util.Globals;
import org.lwjgl.opengl.GL30C;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// Thanks ladysnake!
public class ShaderManager implements Globals
{
    private final OutlineBufferSource vertexConsumerProvider = new OutlineBufferSource(MultiBufferSource.immediate(new ByteBufferBuilder(256)));
    private final Function<RenderStateShard.EmptyTextureStateShard, RenderType> layerCreator;
    private final RenderStateShard.OutputStateShard target;

    private ShaderFramebuffer framebuffer;

    public ManagedShaderEffect filledShaderEffect;
    public ManagedShaderEffect gradientShaderEffect;
    public ManagedShaderEffect imageShaderEffect;
    public ManagedShaderEffect glowingShaderEffect;
    public ManagedShaderEffect rainbowShaderEffect;

    public ShaderManager()
    {
        target = new RenderStateShard.OutputStateShard("shader_target", () -> {}, () -> {});
        layerCreator = memoizeTexture(texture -> RenderType.create("shoreline_overlay", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_OUTLINE_SHADER).setCullState(RenderStateShard.NO_CULL).setTextureState(texture).setDepthTestState(RenderStateShard.NO_DEPTH_TEST).setOutputState(target).createCompositeState(RenderType.OutlineProperty.IS_OUTLINE)));
    }

    public void reloadShaders()
    {
        if (framebuffer == null || filledShaderEffect == null || gradientShaderEffect == null || imageShaderEffect == null || glowingShaderEffect == null)
        {
            reloadShadersInternal();
        }
    }

    public void reloadShadersInternal()
    {
        framebuffer = new ShaderFramebuffer(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
        filledShaderEffect = ShaderEffectManager.getInstance().manage(ResourceLocation.fromNamespaceAndPath("shoreline", "shaders/post/outline.json"));
        gradientShaderEffect = ShaderEffectManager.getInstance().manage(ResourceLocation.fromNamespaceAndPath("shoreline", "shaders/post/gradient.json"));
        imageShaderEffect = ShaderEffectManager.getInstance().manage(ResourceLocation.fromNamespaceAndPath("shoreline", "shaders/post/image.json"));
        glowingShaderEffect = ShaderEffectManager.getInstance().manage(ResourceLocation.fromNamespaceAndPath("shoreline", "shaders/post/glowing.json"));
        rainbowShaderEffect = ShaderEffectManager.getInstance().manage(ResourceLocation.fromNamespaceAndPath("shoreline", "shaders/post/rainbow.json"));
    }

    public void applyShader(ManagedShaderEffect shaderEffect, Runnable setup, Runnable runnable)
    {
        RenderTarget mcFramebuffer = mc.getMainRenderTarget();
        RenderSystem.assertOnRenderThreadOrInit();
        if (framebuffer.width != mcFramebuffer.width || framebuffer.height != mcFramebuffer.height)
        {
            framebuffer.resize(mcFramebuffer.width, mcFramebuffer.height, false);
        }

        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, framebuffer.frameBufferId);
        framebuffer.bindWrite(false);
        runnable.run();
        // Render callbacks here
        framebuffer.unbindWrite();
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, mcFramebuffer.frameBufferId);
        mcFramebuffer.bindWrite(false);
        PostChain effect = shaderEffect.getShaderEffect();
        if (effect != null)
        {
            ((IPostEffectProcessor) effect).overwriteBuffer("bufIn", framebuffer);
            RenderTarget bufOut = effect.getTempTarget("bufOut");
            // Setup shader here
            setup.run();
            framebuffer.clear(false);
            mcFramebuffer.bindWrite(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            RenderSystem.backupProjectionMatrix();
            bufOut.blitToScreen(bufOut.width, bufOut.height, false);
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }

    }

    public MultiBufferSource createVertexConsumers(MultiBufferSource parent, Color color)
    {
        return layer ->
        {
            VertexConsumer parentBuffer = parent.getBuffer(layer);
            if (!(layer instanceof RenderType.CompositeRenderType) || ((AccessorMultiPhaseParameters) (Object) ((AccessorMultiPhase) layer).hookGetPhases()).hookGetOutlineMode() == RenderType.OutlineProperty.NONE)
            {
                return parentBuffer;
            }

            vertexConsumerProvider.setColor(color.getRed(), color.getGreen(), color.getBlue(), 255);

            VertexConsumer outlineBuffer = vertexConsumerProvider.getBuffer(layerCreator.apply(((AccessorMultiPhaseParameters) (Object) ((AccessorMultiPhase) layer).hookGetPhases()).hookGetTexture()));
            return outlineBuffer != null ? outlineBuffer : parentBuffer;
        };
    }

    private Function<RenderStateShard.EmptyTextureStateShard, RenderType> memoizeTexture(Function<RenderStateShard.EmptyTextureStateShard, RenderType> function)
    {
        return new Function<>()
        {
            private final Map<ResourceLocation, RenderType> cache = new HashMap<>();

            public RenderType apply(RenderStateShard.EmptyTextureStateShard texture)
            {
                return this.cache.computeIfAbsent(((AccessorTextureBase) texture).hookGetId().get(), id -> function.apply(texture));
            }
        };
    }

    // Instance without overwritten buffers
    public ManagedShaderEffect getFilledShaderEffect()
    {
        return filledShaderEffect;
    }

    public ManagedShaderEffect getGradientShaderEffect()
    {
        return gradientShaderEffect;
    }

    public ManagedShaderEffect getImageShaderEffect()
    {
        return imageShaderEffect;
    }

    public ManagedShaderEffect getGlowingShaderEffect()
    {
        return glowingShaderEffect;
    }

    public ManagedShaderEffect getRainbowShaderEffect()
    {
        return rainbowShaderEffect;
    }
}

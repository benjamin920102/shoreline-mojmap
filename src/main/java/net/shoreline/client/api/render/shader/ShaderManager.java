package net.shoreline.client.api.render.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.OutputStateShard;
import net.minecraft.client.renderer.RenderType.CompositeRenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.RenderType.OutlineProperty;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.api.render.satin.ManagedShaderEffect;
import net.shoreline.client.api.render.satin.ShaderEffectManager;
import net.shoreline.client.impl.imixin.IPostEffectProcessor;
import net.shoreline.client.mixin.accessor.AccessorMultiPhase;
import net.shoreline.client.mixin.accessor.AccessorMultiPhaseParameters;
import net.shoreline.client.mixin.accessor.AccessorTextureBase;
import net.shoreline.client.util.Globals;

public class ShaderManager implements Globals {
   private final OutlineBufferSource vertexConsumerProvider = new OutlineBufferSource(MultiBufferSource.immediate(new ByteBufferBuilder(256)));
   private final Function<EmptyTextureStateShard, RenderType> layerCreator;
   private final OutputStateShard target = new OutputStateShard("shader_target", () -> {}, () -> {});
   private ShaderFramebuffer framebuffer;
   public ManagedShaderEffect filledShaderEffect;
   public ManagedShaderEffect gradientShaderEffect;
   public ManagedShaderEffect imageShaderEffect;
   public ManagedShaderEffect glowingShaderEffect;
   public ManagedShaderEffect rainbowShaderEffect;

   public ShaderManager() {
      this.layerCreator = this.memoizeTexture(
         texture -> RenderType.create(
            "shoreline_overlay",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            Mode.QUADS,
            1536,
            CompositeState.builder()
               .setShaderState(RenderStateShard.RENDERTYPE_OUTLINE_SHADER)
               .setCullState(RenderStateShard.NO_CULL)
               .setTextureState(texture)
               .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
               .setOutputState(this.target)
               .createCompositeState(OutlineProperty.IS_OUTLINE)
         )
      );
   }

   public void reloadShaders() {
      if (this.framebuffer == null
         || this.filledShaderEffect == null
         || this.gradientShaderEffect == null
         || this.imageShaderEffect == null
         || this.glowingShaderEffect == null) {
         this.reloadShadersInternal();
      }
   }

   public void reloadShadersInternal() {
      this.framebuffer = new ShaderFramebuffer(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
      this.filledShaderEffect = ShaderEffectManager.getInstance().manage(ResourceLocation.fromNamespaceAndPath("shoreline", "shaders/post/outline.json"));
      this.gradientShaderEffect = ShaderEffectManager.getInstance().manage(ResourceLocation.fromNamespaceAndPath("shoreline", "shaders/post/gradient.json"));
      this.imageShaderEffect = ShaderEffectManager.getInstance().manage(ResourceLocation.fromNamespaceAndPath("shoreline", "shaders/post/image.json"));
      this.glowingShaderEffect = ShaderEffectManager.getInstance().manage(ResourceLocation.fromNamespaceAndPath("shoreline", "shaders/post/glowing.json"));
      this.rainbowShaderEffect = ShaderEffectManager.getInstance().manage(ResourceLocation.fromNamespaceAndPath("shoreline", "shaders/post/rainbow.json"));
   }

   public void applyShader(ManagedShaderEffect shaderEffect, Runnable setup, Runnable runnable) {
      RenderTarget mcFramebuffer = mc.getMainRenderTarget();
      RenderSystem.assertOnRenderThreadOrInit();
      if (this.framebuffer.width != mcFramebuffer.width || this.framebuffer.height != mcFramebuffer.height) {
         this.framebuffer.resize(mcFramebuffer.width, mcFramebuffer.height, false);
      }

      GlStateManager._glBindFramebuffer(36009, this.framebuffer.frameBufferId);
      this.framebuffer.bindWrite(false);
      runnable.run();
      this.framebuffer.unbindWrite();
      GlStateManager._glBindFramebuffer(36009, mcFramebuffer.frameBufferId);
      mcFramebuffer.bindWrite(false);
      PostChain effect = shaderEffect.getShaderEffect();
      if (effect != null) {
         ((IPostEffectProcessor)effect).overwriteBuffer("bufIn", this.framebuffer);
         RenderTarget bufOut = effect.getTempTarget("bufOut");
         setup.run();
         this.framebuffer.clear(false);
         mcFramebuffer.bindWrite(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ZERO, DestFactor.ONE);
         RenderSystem.backupProjectionMatrix();
         bufOut.blitToScreen(bufOut.width, bufOut.height, false);
         RenderSystem.restoreProjectionMatrix();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   public MultiBufferSource createVertexConsumers(MultiBufferSource parent, Color color) {
      return layer -> {
         VertexConsumer parentBuffer = parent.getBuffer(layer);
         if (layer instanceof CompositeRenderType
            && ((AccessorMultiPhaseParameters)(Object)((AccessorMultiPhase)layer).hookGetPhases()).hookGetOutlineMode() != OutlineProperty.NONE) {
            this.vertexConsumerProvider.setColor(color.getRed(), color.getGreen(), color.getBlue(), 255);
            VertexConsumer outlineBuffer = this.vertexConsumerProvider
               .getBuffer(this.layerCreator.apply(((AccessorMultiPhaseParameters)(Object)((AccessorMultiPhase)layer).hookGetPhases()).hookGetTexture()));
            return outlineBuffer != null ? outlineBuffer : parentBuffer;
         } else {
            return parentBuffer;
         }
      };
   }

   private Function<EmptyTextureStateShard, RenderType> memoizeTexture(Function<EmptyTextureStateShard, RenderType> function) {
      return new Function<EmptyTextureStateShard, RenderType>() {
         private final Map<ResourceLocation, RenderType> cache = new HashMap<>();

         public RenderType apply(EmptyTextureStateShard texture) {
            return this.cache.computeIfAbsent(((AccessorTextureBase)texture).hookGetId().get(), id -> function.apply(texture));
         }
      };
   }

   public ManagedShaderEffect getFilledShaderEffect() {
      return this.filledShaderEffect;
   }

   public ManagedShaderEffect getGradientShaderEffect() {
      return this.gradientShaderEffect;
   }

   public ManagedShaderEffect getImageShaderEffect() {
      return this.imageShaderEffect;
   }

   public ManagedShaderEffect getGlowingShaderEffect() {
      return this.glowingShaderEffect;
   }

   public ManagedShaderEffect getRainbowShaderEffect() {
      return this.rainbowShaderEffect;
   }
}

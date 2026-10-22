package net.shoreline.client.api.render.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;

public class ShaderFramebuffer extends RenderTarget {
   public ShaderFramebuffer(int width, int height) {
      super(false);
      RenderSystem.assertOnRenderThreadOrInit();
      this.resize(width, height, true);
      this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
   }
}

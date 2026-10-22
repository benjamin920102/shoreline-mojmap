package net.shoreline.client.api.render.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;

// Only use this framebuffer if you are rendering shaders
public class ShaderFramebuffer extends RenderTarget
{
    public ShaderFramebuffer(int width, int height)
    {
        super(false);
        RenderSystem.assertOnRenderThreadOrInit();
        resize(width, height, true);
        setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }
}

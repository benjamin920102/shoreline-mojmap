package net.shoreline.client.impl.imixin;

import com.mojang.blaze3d.pipeline.RenderTarget;

@IMixin
public interface IPostEffectProcessor {
   void overwriteBuffer(String var1, RenderTarget var2);
}

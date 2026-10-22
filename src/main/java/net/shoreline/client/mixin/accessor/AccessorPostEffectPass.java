package net.shoreline.client.mixin.accessor;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostPass.class)
public interface AccessorPostEffectPass
{
    @Mutable
    @Accessor("inTarget")
    void hookSetInput(RenderTarget framebuffer);

    @Mutable
    @Accessor("outTarget")
    void hookSetOutput(RenderTarget framebuffer);
}

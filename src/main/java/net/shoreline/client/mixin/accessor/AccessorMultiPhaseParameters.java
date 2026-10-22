package net.shoreline.client.mixin.accessor;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.CompositeState.class)
public interface AccessorMultiPhaseParameters
{
    @Accessor("outlineMode")
    RenderType.OutlineProperty hookGetOutlineMode();

    @Accessor("texture")
    RenderStateShard.EmptyTextureStateShard hookGetTexture();
}

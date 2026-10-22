package net.shoreline.client.mixin.accessor;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;

@Mixin(RenderStateShard.EmptyTextureStateShard.class)
public interface AccessorTextureBase
{
    @Invoker("getId")
    Optional<ResourceLocation> hookGetId();
}

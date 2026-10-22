package net.shoreline.client.mixin.accessor;

import java.util.Optional;
import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({EmptyTextureStateShard.class})
public interface AccessorTextureBase {
   @Invoker("cutoutTexture")
   Optional<ResourceLocation> hookGetId();
}

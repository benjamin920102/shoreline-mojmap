package net.shoreline.client.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;

@Mixin(PostChain.class)
public interface AccessorPostEffectProcessor
{
    @Accessor("passes")
    List<PostPass> hookGetPasses();
}

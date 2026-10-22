package net.shoreline.client.mixin.accessor;

import java.util.List;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PostChain.class})
public interface AccessorPostEffectProcessor {
   @Accessor("passes")
   List<PostPass> hookGetPasses();
}

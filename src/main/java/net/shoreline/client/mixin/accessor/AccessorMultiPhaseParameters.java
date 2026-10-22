package net.shoreline.client.mixin.accessor;

import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.RenderType.OutlineProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CompositeState.class})
public interface AccessorMultiPhaseParameters {
   @Accessor("outlineProperty")
   OutlineProperty hookGetOutlineMode();

   @Accessor("textureState")
   EmptyTextureStateShard hookGetTexture();
}

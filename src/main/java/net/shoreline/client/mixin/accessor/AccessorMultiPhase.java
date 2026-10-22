package net.shoreline.client.mixin.accessor;

import net.minecraft.client.renderer.RenderType.CompositeRenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({CompositeRenderType.class})
public interface AccessorMultiPhase {
   @Invoker("state")
   CompositeState hookGetPhases();
}

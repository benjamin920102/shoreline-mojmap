package net.shoreline.client.mixin.accessor;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.CompositeRenderType.class)
public interface AccessorMultiPhase
{
    @Invoker("getPhases")
    RenderType.CompositeState hookGetPhases();
}

package net.shoreline.client.mixin.accessor;

import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderStateShard.class)
public interface AccessorRenderPhase
{

    @Mutable
    @Accessor("setupState")
    void hookSetBeginAction(Runnable runnable);

    @Mutable
    @Accessor("clearState")
    void hookSetEndAction(Runnable runnable);
}

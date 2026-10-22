package net.shoreline.client.mixin.chunk.light;

import net.minecraft.world.level.lighting.SkyLightEngine;
import net.shoreline.client.impl.event.chunk.light.RenderSkylightEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @see SkyLightEngine
 * @since 1.0
 */
@Mixin(SkyLightEngine.class)
public class MixinChunkSkylightProvider
{
    /**
     * @param blockPos
     * @param l
     * @param lightLevel
     * @param ci
     */
    @Inject(method = "propagateIncrease", at = @At(value = "HEAD"), cancellable = true)
    private void hookRecalculateLevel(long blockPos, long l, int lightLevel, CallbackInfo ci)
    {
        RenderSkylightEvent renderSkylightEvent = new RenderSkylightEvent();
        EventBus.INSTANCE.dispatch(renderSkylightEvent);
        if (renderSkylightEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}

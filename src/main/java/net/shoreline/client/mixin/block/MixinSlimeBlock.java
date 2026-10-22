package net.shoreline.client.mixin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.client.impl.event.block.SteppedOnSlimeBlockEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @see SlimeBlock
 * @since 1.0
 */
@Mixin(SlimeBlock.class)
public class MixinSlimeBlock implements Globals
{
    /**
     * @param world
     * @param pos
     * @param state
     * @param entity
     * @param ci
     */
    @Inject(method = "stepOn", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookOnSteppedOn(Level world, BlockPos pos, BlockState state,
                                 Entity entity, CallbackInfo ci)
    {
        SteppedOnSlimeBlockEvent steppedOnSlimeBlockEvent =
                new SteppedOnSlimeBlockEvent();
        EventBus.INSTANCE.dispatch(steppedOnSlimeBlockEvent);
        if (steppedOnSlimeBlockEvent.isCanceled() && entity == mc.player)
        {
            ci.cancel();
        }
    }
}

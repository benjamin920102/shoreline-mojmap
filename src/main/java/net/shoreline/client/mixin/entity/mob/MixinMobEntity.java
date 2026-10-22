package net.shoreline.client.mixin.entity.mob;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.shoreline.client.impl.event.entity.mob.PigAIEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MixinMobEntity
{
    @Inject(method = "isNoAi", at = @At(value = "HEAD"), cancellable = true)
    private void hookIsAiDisabled(CallbackInfoReturnable<Boolean> cir)
    {
        Mob mobEntity = (Mob) (Object) this;
        if (mobEntity instanceof Pig)
        {
            PigAIEvent pigAIEvent = new PigAIEvent((Pig) mobEntity);
            EventBus.INSTANCE.dispatch(pigAIEvent);
            if (pigAIEvent.isCanceled())
            {
                cir.cancel();
                cir.setReturnValue(true);
            }
        }
    }
}

package net.shoreline.client.mixin.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shoreline.client.impl.event.item.FinishUsingEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class MixinItem
{
    @Inject(method = "finishUsingItem", at = @At(value = "HEAD"))
    private void hookFinishUsing(ItemStack stack, Level world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir)
    {
        FinishUsingEvent finishUsingEvent = new FinishUsingEvent(stack);
        EventBus.INSTANCE.dispatch(finishUsingEvent);
    }
}

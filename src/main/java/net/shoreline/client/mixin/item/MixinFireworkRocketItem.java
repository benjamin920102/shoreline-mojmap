package net.shoreline.client.mixin.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shoreline.client.impl.event.item.FireworkUseEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(FireworkRocketItem.class)
public class MixinFireworkRocketItem
{
    /**
     * @param world
     * @param user
     * @param hand
     * @param cir
     */
    @Inject(method = "use", at = @At(value = "HEAD"))
    private void hookUse(Level world, Player user, InteractionHand hand,
                         CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir)
    {
        FireworkUseEvent fireworkUseEvent = new FireworkUseEvent();
        EventBus.INSTANCE.dispatch(fireworkUseEvent);
    }
}

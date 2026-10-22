package net.shoreline.client.mixin.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.shoreline.client.impl.event.network.ItemDesyncEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({UseOnContext.class})
public final class MixinItemUsageContext implements Globals {
   @Inject(
      method = {"getItemInHand()Lnet/minecraft/world/item/ItemStack;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void hookGetStack(CallbackInfoReturnable<ItemStack> info) {
      ItemDesyncEvent itemDesyncEvent = new ItemDesyncEvent();
      EventBus.INSTANCE.dispatch(itemDesyncEvent);
      if (mc.player != null && ((ItemStack)info.getReturnValue()).equals(mc.player.getMainHandItem()) && itemDesyncEvent.isCanceled()) {
         info.setReturnValue(itemDesyncEvent.getServerItem());
      }
   }
}

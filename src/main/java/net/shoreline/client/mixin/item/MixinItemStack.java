package net.shoreline.client.mixin.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.shoreline.client.impl.event.item.DurabilityEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemStack.class})
public abstract class MixinItemStack {
   @Shadow
   public abstract int getDamageValue();

   @Shadow
   public abstract void setDamageValue(int var1);

   @Inject(
      method = {"<init>(Lnet/minecraft/world/level/ItemLike;I)V"},
      at = {@At("RETURN")}
   )
   private void hookInitItem(ItemLike item, int count, CallbackInfo ci) {
      if (EventBus.INSTANCE != null) {
         DurabilityEvent durabilityEvent = new DurabilityEvent(this.getDamageValue());
         EventBus.INSTANCE.dispatch(durabilityEvent);
         if (durabilityEvent.isCanceled()) {
            this.setDamageValue(durabilityEvent.getDamage());
         }
      }
   }
}

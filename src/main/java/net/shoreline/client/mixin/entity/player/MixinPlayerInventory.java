package net.shoreline.client.mixin.entity.player;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shoreline.client.impl.event.entity.player.SetStackEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Inventory.class})
public class MixinPlayerInventory {
   @Inject(
      method = {"setItem(ILnet/minecraft/world/item/ItemStack;)V"},
      at = {@At("HEAD")}
   )
   private void hookSetStack(int slot, ItemStack stack, CallbackInfo ci) {
      SetStackEvent setStackEvent = new SetStackEvent(slot, stack);
      EventBus.INSTANCE.dispatch(setStackEvent);
   }
}

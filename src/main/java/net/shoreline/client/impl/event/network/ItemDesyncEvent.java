package net.shoreline.client.impl.event.network;

import net.minecraft.world.item.ItemStack;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class ItemDesyncEvent extends Event {
   private ItemStack stack;

   public void setStack(ItemStack stack) {
      this.stack = stack;
   }

   public ItemStack getServerItem() {
      return this.stack;
   }
}

package net.shoreline.client.impl.event.render;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderFloatingItemEvent extends Event {
   private final ItemStack floatingItem;

   public RenderFloatingItemEvent(ItemStack floatingItem) {
      this.floatingItem = floatingItem;
   }

   public Item getFloatingItem() {
      return this.floatingItem.getItem();
   }

   public ItemStack getFloatingItemStack() {
      return this.floatingItem;
   }
}

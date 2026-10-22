package net.shoreline.client.impl.event.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shoreline.eventbus.event.Event;

public class ConsumeItemEvent extends Event {
   private final ItemStack activeItemStack;

   public ConsumeItemEvent(ItemStack activeItemStack) {
      this.activeItemStack = activeItemStack;
   }

   public ItemStack getStack() {
      return this.activeItemStack;
   }

   public Item getItem() {
      return this.activeItemStack.getItem();
   }
}

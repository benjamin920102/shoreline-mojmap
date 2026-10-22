package net.shoreline.client.impl.event.render.entity;

import net.minecraft.world.entity.item.ItemEntity;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderItemEvent extends Event {
   private final ItemEntity itemEntity;

   public RenderItemEvent(ItemEntity itemEntity) {
      this.itemEntity = itemEntity;
   }

   public ItemEntity getItem() {
      return this.itemEntity;
   }
}

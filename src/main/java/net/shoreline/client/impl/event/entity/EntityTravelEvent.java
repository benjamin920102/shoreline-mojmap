package net.shoreline.client.impl.event.entity;

import net.minecraft.world.entity.LivingEntity;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class EntityTravelEvent extends Event {
   private final LivingEntity entity;
   private final boolean pre;

   public EntityTravelEvent(LivingEntity entity, boolean pre) {
      this.entity = entity;
      this.pre = pre;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }

   public boolean isPre() {
      return this.pre;
   }
}

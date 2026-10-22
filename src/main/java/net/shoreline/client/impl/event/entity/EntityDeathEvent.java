package net.shoreline.client.impl.event.entity;

import net.minecraft.world.entity.LivingEntity;
import net.shoreline.eventbus.event.Event;

public class EntityDeathEvent extends Event {
   private final LivingEntity entity;

   public EntityDeathEvent(LivingEntity entity) {
      this.entity = entity;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }
}

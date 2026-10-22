package net.shoreline.client.impl.event.render.entity;

import net.minecraft.world.entity.LivingEntity;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderArmorEvent extends Event {
   private final LivingEntity entity;

   public RenderArmorEvent(LivingEntity entity) {
      this.entity = entity;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }
}

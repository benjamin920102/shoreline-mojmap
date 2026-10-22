package net.shoreline.client.impl.event.entity;

import net.minecraft.world.entity.LivingEntity;
import net.shoreline.eventbus.event.Event;

public class PlayerDamageEvent extends Event {
   private final LivingEntity damaged;

   public PlayerDamageEvent(LivingEntity damaged) {
      this.damaged = damaged;
   }

   public LivingEntity getDamaged() {
      return this.damaged;
   }
}

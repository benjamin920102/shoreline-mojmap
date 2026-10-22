package net.shoreline.client.impl.event.entity;

import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public final class LimbAnimationEvent extends Event {
   float speed;

   public void setSpeed(float speed) {
      this.speed = speed;
   }

   public float getSpeed() {
      return this.speed;
   }
}

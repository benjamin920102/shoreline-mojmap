package net.shoreline.client.impl.event.render;

import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class CameraClipEvent extends Event {
   private float distance;

   public CameraClipEvent(float distance) {
      this.distance = distance;
   }

   public float getDistance() {
      return this.distance;
   }

   public void setDistance(float distance) {
      this.distance = distance;
   }
}

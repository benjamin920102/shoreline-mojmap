package net.shoreline.client.impl.event.network;

import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class TickMovementEvent extends Event {
   private int iterations;

   public int getIterations() {
      return this.iterations;
   }

   public void setIterations(int iterations) {
      this.iterations = iterations;
   }
}

package net.shoreline.client.util.math.timer;

import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class TickTimer implements Timer {
   private long ticks = 0L;

   public TickTimer() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener(
      priority = Integer.MAX_VALUE
   )
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         this.ticks++;
      }
   }

   @Override
   public boolean passed(Number time) {
      return this.ticks >= time.longValue();
   }

   @Override
   public void reset() {
      this.setElapsedTime(0);
   }

   @Override
   public long getElapsedTime() {
      return this.ticks;
   }

   @Override
   public void setElapsedTime(Number time) {
      this.ticks = time.longValue();
   }
}

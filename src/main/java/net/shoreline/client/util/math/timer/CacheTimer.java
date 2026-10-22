package net.shoreline.client.util.math.timer;

import java.util.concurrent.TimeUnit;

public class CacheTimer implements Timer {
   private long time = System.nanoTime();
   private long lastResetTime;

   @Override
   public boolean passed(Number time) {
      return time.longValue() <= 0L ? true : this.getElapsedTime() > time.longValue();
   }

   public boolean passed(Number time, TimeUnit unit) {
      return this.passed(unit.toMillis(time.longValue()));
   }

   @Override
   public long getElapsedTime() {
      return this.toMillis(System.nanoTime() - this.time);
   }

   @Override
   public void setElapsedTime(Number time) {
      this.time = time.longValue() == -255L ? 0L : System.nanoTime() - time.longValue();
   }

   public void setDelay(Number delay) {
      this.time = this.time + delay.longValue();
   }

   public long getElapsedTime(TimeUnit unit) {
      return unit.convert(this.getElapsedTime(), TimeUnit.MILLISECONDS);
   }

   public long getLastResetTime() {
      return this.lastResetTime;
   }

   @Override
   public void reset() {
      long time = System.nanoTime();
      this.lastResetTime = time - this.time;
      this.time = time;
   }

   private long toMillis(long nanos) {
      return nanos / 1000000L;
   }
}

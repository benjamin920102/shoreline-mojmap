package net.shoreline.eventbus.event;

import net.shoreline.eventbus.annotation.Cancelable;

public class Event {
   private final boolean cancelable = this.getClass().isAnnotationPresent(Cancelable.class);
   private boolean canceled;

   public boolean isCancelable() {
      return this.cancelable;
   }

   public boolean isCanceled() {
      return this.canceled;
   }

   public void setCanceled(boolean cancel) {
      if (this.isCancelable()) {
         this.canceled = cancel;
      } else {
         throw new IllegalStateException("Cannot set event canceled");
      }
   }

   public void cancel() {
      this.setCanceled(true);
   }
}

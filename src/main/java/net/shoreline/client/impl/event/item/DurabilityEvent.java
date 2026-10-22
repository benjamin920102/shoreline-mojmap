package net.shoreline.client.impl.event.item;

import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class DurabilityEvent extends Event {
   private int damage;

   public DurabilityEvent(int damage) {
      this.damage = damage;
   }

   public int getItemDamage() {
      return Math.max(0, this.damage);
   }

   public int getDamage() {
      return this.damage;
   }

   public void setDamage(int damage) {
      this.damage = damage;
   }
}

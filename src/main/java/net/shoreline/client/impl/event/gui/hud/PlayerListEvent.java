package net.shoreline.client.impl.event.gui.hud;

import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class PlayerListEvent extends Event {
   private int size;

   public int getSize() {
      return this.size;
   }

   public void setSize(int size) {
      this.size = size;
   }
}

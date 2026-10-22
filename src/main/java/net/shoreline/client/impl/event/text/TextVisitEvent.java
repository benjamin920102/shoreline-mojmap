package net.shoreline.client.impl.event.text;

import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class TextVisitEvent extends Event {
   private String text;

   public TextVisitEvent(String text) {
      this.text = text;
   }

   public String getText() {
      return this.text;
   }

   public void setText(String text) {
      this.text = text;
   }
}

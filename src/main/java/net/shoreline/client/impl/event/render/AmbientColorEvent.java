package net.shoreline.client.impl.event.render;

import java.awt.Color;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class AmbientColorEvent extends Event {
   private Color color;

   public Color getColor() {
      return this.color;
   }

   public void setColor(Color color) {
      this.color = color;
   }
}

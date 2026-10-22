package net.shoreline.client.impl.event.keyboard;

import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class KeyboardInputEvent extends Event {
   private final int keycode;
   private final int action;

   public KeyboardInputEvent(int keycode, int action) {
      this.keycode = keycode;
      this.action = action;
   }

   public int getKeycode() {
      return this.keycode;
   }

   public int getAction() {
      return this.action;
   }
}

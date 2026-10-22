package net.shoreline.client.impl.event;

import net.minecraft.client.gui.screens.Screen;
import net.shoreline.eventbus.event.Event;

public class ScreenOpenEvent extends Event {
   private final Screen screen;

   public ScreenOpenEvent(Screen screen) {
      this.screen = screen;
   }

   public Screen getScreen() {
      return this.screen;
   }
}

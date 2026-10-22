package net.shoreline.client.impl.event.gui.click;

import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.eventbus.event.Event;

public class ToggleGuiEvent extends Event {
   private final ToggleModule module;

   public ToggleGuiEvent(ToggleModule module) {
      this.module = module;
   }

   public ToggleModule getModule() {
      return this.module;
   }

   public boolean isEnabled() {
      return this.module.isEnabled();
   }
}

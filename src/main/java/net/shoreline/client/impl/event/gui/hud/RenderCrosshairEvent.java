package net.shoreline.client.impl.event.gui.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderCrosshairEvent extends Event {
   private final GuiGraphics context;

   public RenderCrosshairEvent(GuiGraphics context) {
      this.context = context;
   }

   public GuiGraphics getContext() {
      return this.context;
   }
}

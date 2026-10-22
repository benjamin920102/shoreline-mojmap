package net.shoreline.client.impl.event.gui.hud;

import net.minecraft.client.GuiMessage.Line;
import net.shoreline.eventbus.event.Event;

public class ChatLineEvent extends Event {
   private final Line chatHudLine;
   private final double width;

   public ChatLineEvent(Line chatHudLine, double width) {
      this.chatHudLine = chatHudLine;
      this.width = width;
   }

   public Line getChatHudLine() {
      return this.chatHudLine;
   }

   public double getWidth() {
      return this.width;
   }
}

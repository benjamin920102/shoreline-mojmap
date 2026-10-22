package net.shoreline.client.impl.event.gui.hud;

import net.minecraft.network.chat.Component;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class ChatMessageEvent extends Event {
   private Component text;

   public ChatMessageEvent(Component text) {
      this.text = text;
   }

   public void setText(Component text) {
      this.text = text;
   }

   public Component getText() {
      return this.text;
   }
}

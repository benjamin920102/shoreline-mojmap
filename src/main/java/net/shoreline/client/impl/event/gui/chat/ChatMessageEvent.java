package net.shoreline.client.impl.event.gui.chat;

import net.minecraft.util.StringUtil;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;
import org.apache.commons.lang3.StringUtils;

@Cancelable
public class ChatMessageEvent extends Event {
   private final String message;

   public ChatMessageEvent(String message) {
      this.message = message;
   }

   public String getMessage() {
      return this.normalize(this.message);
   }

   private String normalize(String chatText) {
      return StringUtil.trimChatMessage(StringUtils.normalizeSpace(chatText.trim()));
   }

   @Cancelable
   public static class Client extends ChatMessageEvent {
      public Client(String message) {
         super(message);
      }
   }

   @Cancelable
   public static class Server extends ChatMessageEvent {
      public Server(String message) {
         super(message);
      }
   }
}

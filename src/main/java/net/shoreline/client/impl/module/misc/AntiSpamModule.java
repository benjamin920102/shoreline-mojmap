package net.shoreline.client.impl.module.misc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class AntiSpamModule extends ToggleModule {
   Config<Boolean> unicodeConfig = this.register(new BooleanConfig("Unicode", "Prevents unicode characters from being rendered in chat", false));
   private final Map<UUID, String> messages = new HashMap<>();

   public AntiSpamModule() {
      super("AntiSpam", "Prevents players from spamming the game chat", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof ClientboundPlayerChatPacket packet) {
            if (this.unicodeConfig.getValue()) {
               String msg = packet.body().content();
               Pattern pattern = Pattern.compile("[^\\x00-\\x7F\\u0400-\\u04FF]+", 2);
               Matcher matcher = pattern.matcher(msg);
               if (matcher.find()) {
                  event.cancel();
                  return;
               }
            }

            UUID sender = packet.sender();
            String chatMessage = packet.body().content();
            String lastMessage = this.messages.get(sender);
            if (chatMessage.equalsIgnoreCase(lastMessage)) {
               event.cancel();
            } else if (lastMessage != null) {
               this.messages.replace(sender, chatMessage);
            } else {
               this.messages.put(sender, chatMessage);
            }
         }
      }
   }
}

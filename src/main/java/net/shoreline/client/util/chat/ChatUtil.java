package net.shoreline.client.util.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.shoreline.client.impl.imixin.IChatHud;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.util.Globals;

public class ChatUtil implements Globals {
   public static final String PREFIX = "§s[Shoreline] §r";

   public static void clientSendMessage(String message) {
      clientSendMessage(message, 0);
   }

   public static void clientSendMessage(String message, int id) {
      GuiMessageTag messageIndicator = new GuiMessageTag(ColorsModule.getInstance().getRGB(), null, Component.empty(), "Shoreline");
      ((IChatHud)mc.gui.getChat()).addMessage(Component.nullToEmpty("§s[Shoreline] §r" + message), messageIndicator, id);
   }

   public static void clientSendMessage(String message, Object... params) {
      clientSendMessage(String.format(message, params));
   }

   public static void clientSendMessageRaw(String message) {
      clientSendMessageRaw(message, 0);
   }

   public static void clientSendMessageRaw(String message, int id) {
      GuiMessageTag messageIndicator = new GuiMessageTag(ColorsModule.getInstance().getRGB(), null, Component.empty(), "Shoreline");
      ((IChatHud)mc.gui.getChat()).addMessage(Component.nullToEmpty(message), messageIndicator, id);
   }

   public static void clientSendMessageRaw(String message, Object... params) {
      clientSendMessageRaw(String.format(message, params));
   }

   public static void serverSendMessage(String message) {
      if (mc.player != null) {
         mc.player.connection.sendChat(message);
      }
   }

   public static void serverSendCommand(String message) {
      if (mc.player != null) {
         mc.player.connection.sendCommand(message);
      }
   }

   public static void serverSendMessage(Player player, String message) {
      if (mc.player != null) {
         String reply = "/msg " + player.getName().getString() + " ";
         mc.player.connection.sendChat(reply + message);
      }
   }

   public static void serverSendMessage(Player player, String message, Object... params) {
      serverSendMessage(player, String.format(message, params));
   }

   public static void error(String message) {
      clientSendMessage(ChatFormatting.RED + message);
   }

   public static void error(String message, Object... params) {
      clientSendMessage(ChatFormatting.RED + message, params);
   }
}

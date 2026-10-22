package net.shoreline.client.impl.module.misc;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.network.GameJoinEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.util.math.HexRandom;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class SpammerModule extends ToggleModule {
   Config<Float> delayConfig = this.register(new NumberConfig<>("Delay", "The chat message delay", 0.0F, 1.5F, 10.0F));
   Config<Boolean> randomConfig = this.register(new BooleanConfig("Random", "Randomizes the spammed messages", false));
   Config<Boolean> antiKickConfig = this.register(new BooleanConfig("AntiKick", "Adds a random suffix to end of messages to prevent kicks", false));
   private final List<String> messages = new ArrayList<>();
   private int messageIndex;
   private final Timer spamTimer = new CacheTimer();

   public SpammerModule() {
      super("Spammer", "Spams messages in the chat", ModuleCategory.MISCELLANEOUS);
   }

   @Override
   public void onEnable() {
      this.loadFile();
      this.messageIndex = 0;
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      this.onEnable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (this.spamTimer.passed(this.delayConfig.getValue() * 1000.0F)) {
            this.sendSpamMessage(this.getSpammerMessage());
            this.spamTimer.reset();
         }
      }
   }

   private void loadFile() {
      File spammerDir = Shoreline.CONFIG.getClientDirectory().resolve("spammer.txt").toFile();
      if (!spammerDir.exists()) {
         this.sendModuleError("The spammer.txt file does not exist! Please create one to enable this module");
         this.disable();
      } else {
         this.messages.clear();

         try {
            for (String line : Files.readAllLines(Path.of(spammerDir.getAbsolutePath()), StandardCharsets.UTF_8)) {
               String[] messages1 = line.split(",");

               for (String message : messages1) {
                  this.messages.add(message.trim());
               }
            }
         } catch (IOException var9) {
         }
      }
   }

   private String getSpammerMessage() {
      String defaultMessage = "Shoreline victory!";
      if (this.messages.isEmpty()) {
         return defaultMessage;
      } else {
         if (this.randomConfig.getValue()) {
            String message = this.messages.get(RANDOM.nextInt(this.messages.size()));
            if (message != null) {
               if (this.antiKickConfig.getValue()) {
                  message = message + " " + HexRandom.generateRandomHex(2);
               }

               return message;
            }
         } else {
            String message = this.messages.get(this.messageIndex);
            this.messageIndex++;
            if (this.messageIndex >= this.messages.size()) {
               this.messageIndex = 0;
            }

            if (message != null) {
               if (this.antiKickConfig.getValue()) {
                  message = message + " " + HexRandom.generateRandomHex(2);
               }

               return message;
            }
         }

         return defaultMessage;
      }
   }

   private void sendSpamMessage(String message) {
      if (message.charAt(0) == '/') {
         message = message.substring(1);
         mc.player.connection.sendUnsignedCommand(message);
      } else if (message.startsWith(Managers.COMMAND.getPrefix())) {
         String literal = message.substring(1);
         mc.gui.getChat().addRecentChat(message);

         try {
            Managers.COMMAND.getDispatcher().execute(Managers.COMMAND.getDispatcher().parse(literal, Managers.COMMAND.getSource()));
         } catch (Exception var4) {
         }
      } else {
         if (mc.isLocalServer()) {
            ChatUtil.clientSendMessageRaw(message);
         } else {
            ChatUtil.serverSendMessage(message);
         }
      }
   }
}

package net.shoreline.client.impl.module.client;

import baritone.api.BaritoneAPI;
import net.minecraft.client.gui.screens.ChatScreen;
import net.shoreline.client.ShorelineMod;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.gui.chat.ChatMessageEvent;
import net.shoreline.client.impl.event.gui.hud.RenderOverlayEvent;
import net.shoreline.client.impl.event.keyboard.KeyboardInputEvent;
import net.shoreline.client.impl.event.network.GameJoinEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.render.animation.Easing;
import net.shoreline.eventbus.annotation.EventListener;

public class ChatModule extends ToggleModule {
   private static ChatModule INSTANCE;
   private boolean ircChat;
   private boolean notified;
   private final Timer timer = new CacheTimer();
   public Config<Boolean> dmsOnly = this.register(new BooleanConfig("DMOnly", "Only receive private messages from IRC", false));
   private final Animation ircAnimation = new Animation(false, 200.0F, Easing.LINEAR);

   public ChatModule() {
      super("Chat", "Manages the client chat", ModuleCategory.CLIENT);
      INSTANCE = this;
   }

   public static ChatModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      if (mc.player != null && !this.notified && !this.dmsOnly.getValue()) {
         ChatUtil.clientSendMessageRaw("§s[Chat]§7 Press ALT in chat to enter IRC!", 107);
         this.notified = true;
      }
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      if (!this.dmsOnly.getValue()) {
         if (!this.notified) {
            ChatUtil.clientSendMessageRaw("§s[Chat]§7 Press ALT in chat to enter IRC!", 107);
            this.notified = true;
         }
      }
   }

   @EventListener
   public void onKey(KeyboardInputEvent event) {
      if (!this.dmsOnly.getValue()) {
         if (this.timer.passed(250)) {
            if (event.getAction() != 2 && (event.getKeycode() == 342 || event.getKeycode() == 346) && mc.screen instanceof ChatScreen) {
               this.ircChat = !this.ircChat;
               this.ircAnimation.setState(this.ircChat);
               this.timer.reset();
            }
         }
      }
   }

   @EventListener(
      priority = Integer.MIN_VALUE
   )
   public void onChatMessage(ChatMessageEvent.Client event) {
      if (!this.dmsOnly.getValue()) {
         if (this.ircChat) {
            String text = event.getMessage().trim();
            if (text.isEmpty()
               || text.isBlank()
               || text.startsWith(Managers.COMMAND.getPrefix())
               || ShorelineMod.isBaritonePresent() && text.startsWith(BaritoneAPI.getSettings().prefix.value)
               || text.startsWith("/")) {
               return;
            }

            event.cancel();
            mc.gui.getChat().addRecentChat(text);
         }
      }
   }

   @EventListener
   public void onRenderOverlay(RenderOverlayEvent.Hotbar event) {
      if (!this.dmsOnly.getValue()) {
         if (mc.screen instanceof ChatScreen && this.ircAnimation.getFactor() > 0.01) {
            float height = mc.getWindow().getGuiScaledHeight();
            float width = mc.getWindow().getGuiScaledWidth();
            float anim = HUDModule.getInstance().isEnabled() ? HUDModule.getInstance().getChatAnimation() : 1.0F;
            RenderManager.borderedRect(
               event.getContext().pose(),
               2.0,
               (int)height,
               width - 4.0F,
               -16.0F * anim,
               ColorsModule.getInstance().getRGB((int)(255.0 * this.ircAnimation.getFactor())),
               1.0
            );
         }
      }
   }

   public boolean isDmsOnly() {
      return this.dmsOnly.getValue();
   }
}

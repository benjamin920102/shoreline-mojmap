package net.shoreline.client.impl.module.misc;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.GuiMessage.Line;
import net.minecraft.network.chat.Component;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.gui.chat.ChatHistoryEvent;
import net.shoreline.client.impl.event.gui.chat.ChatLengthEvent;
import net.shoreline.client.impl.event.gui.hud.ChatLineEvent;
import net.shoreline.client.impl.event.gui.hud.ChatMessageEvent;
import net.shoreline.client.impl.event.gui.hud.ChatTextRenderEvent;
import net.shoreline.client.impl.event.gui.hud.RenderChatHudEvent;
import net.shoreline.client.impl.event.gui.hud.SignatureIndicatorEvent;
import net.shoreline.client.util.FormattingUtil;
import net.shoreline.client.util.render.animation.Easing;
import net.shoreline.client.util.render.animation.TimeAnimation;
import net.shoreline.eventbus.annotation.EventListener;

public class BetterChatModule extends ToggleModule {
   Config<Boolean> chatFontConfig = this.register(new BooleanConfig("ChatFont", "Uses custom font for chat text", false));
   Config<BetterChatModule.Timestamp> timestampConfig = this.register(
      new EnumConfig<>("Timestamp", "Shows chat timestamps", BetterChatModule.Timestamp.OFF, BetterChatModule.Timestamp.values())
   );
   Config<BetterChatModule.AnimationMode> animationConfig = this.register(
      new EnumConfig<>("Animation", "Animates the chat", BetterChatModule.AnimationMode.OFF, BetterChatModule.AnimationMode.values())
   );
   Config<Integer> timeConfig = this.register(new NumberConfig<>("Anim-Time", "Time for the animation", 0, 200, 1000, () -> false));
   Config<Boolean> noSignatureConfig = this.register(new BooleanConfig("NoSignatureIndicator", "Removes the message signature indicator", false));
   Config<Boolean> infiniteConfig = this.register(new BooleanConfig("Infinite", "Makes chat length infinite", false));
   Config<Boolean> keepChatConfig = this.register(new BooleanConfig("KeepChat", "Maintains chat history", false));
   public final Map<Line, TimeAnimation> animationMap = new HashMap<>();

   public BetterChatModule() {
      super("BetterChat", "Modifications for the chat", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onChatText(ChatMessageEvent event) {
      if (this.timestampConfig.getValue() != BetterChatModule.Timestamp.OFF) {
         String string = FormattingUtil.toString(event.getText());
         if (string.contains("§s[Shoreline] §r")) {
            return;
         }

         String time = new SimpleDateFormat("k:mm").format(new Date());

         String text = switch ((BetterChatModule.Timestamp)this.timestampConfig.getValue()) {
            case NORMAL -> "<" + time + ">§r ";
            case GRAY -> "§8<§7" + time + "§8>§r ";
            case COLOR -> "§s<" + time + ">§r ";
            case OFF -> "";
         };
         event.cancel();
         event.setText(Component.nullToEmpty(text + string));
      }
   }

   @EventListener
   public void onChatLine(ChatLineEvent event) {
      this.animationMap.put(event.getChatHudLine(), new TimeAnimation(false, event.getWidth(), 0.0, this.timeConfig.getValue().intValue(), Easing.LINEAR));
   }

   @EventListener
   public void onChatLineRender(RenderChatHudEvent event) {
      if (this.animationConfig.getValue() != BetterChatModule.AnimationMode.OFF) {
         TimeAnimation animation = null;
         if (event.getChatHudLine() != null && this.animationMap.containsKey(event.getChatHudLine())) {
            animation = this.animationMap.get(event.getChatHudLine());
         }

         if (animation != null) {
            animation.setState(true);
            event.cancel();
            if (this.animationConfig.getValue() == BetterChatModule.AnimationMode.SLIDE) {
               event.setAnimation(animation.getCurrent());
            } else {
               event.setAnimation(animation.getFactor());
            }

            event.setSlide(this.animationConfig.getValue() == BetterChatModule.AnimationMode.SLIDE);
         }
      }
   }

   @EventListener
   public void onSignatureIndicator(SignatureIndicatorEvent event) {
      if (this.noSignatureConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onChatHistory(ChatHistoryEvent event) {
      if (this.keepChatConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onChatLength(ChatLengthEvent event) {
      if (this.infiniteConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onChatTextRender(ChatTextRenderEvent event) {
      if (this.chatFontConfig.getValue()) {
         event.cancel();
      }
   }

   public static enum AnimationMode {
      SLIDE,
      FADE,
      OFF;
   }

   public static enum Timestamp {
      NORMAL,
      GRAY,
      COLOR,
      OFF;
   }
}

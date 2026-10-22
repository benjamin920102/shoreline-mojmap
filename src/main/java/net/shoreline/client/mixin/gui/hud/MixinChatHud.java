package net.shoreline.client.mixin.gui.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage.Line;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.gui.chat.ChatHistoryEvent;
import net.shoreline.client.impl.event.gui.chat.ChatLengthEvent;
import net.shoreline.client.impl.event.gui.hud.ChatMessageEvent;
import net.shoreline.client.impl.event.gui.hud.ChatTextEvent;
import net.shoreline.client.impl.event.gui.hud.ChatTextRenderEvent;
import net.shoreline.client.impl.event.gui.hud.RenderChatHudEvent;
import net.shoreline.client.impl.event.gui.hud.SignatureIndicatorEvent;
import net.shoreline.client.impl.imixin.IChatHud;
import net.shoreline.client.impl.imixin.IChatHudLine;
import net.shoreline.client.impl.imixin.IChatHudLineVisible;
import net.shoreline.client.util.FormattingUtil;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatComponent.class})
public abstract class MixinChatHud implements IChatHud, Globals {
   @Shadow
   @Final
   private List<GuiMessage> allMessages;
   @Shadow
   @Final
   private List<Line> trimmedMessages;
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   private boolean newMessageSinceScroll;
   @Shadow
   private int chatScrollbarPos;
   @Unique
   private Line current = null;
   @Unique
   private int currentId;

   @Shadow
   public abstract double getScale();

   @Shadow
   public abstract int getWidth();

   @Shadow
   public abstract void scrollChat(int var1);

   @Shadow
   public abstract boolean isChatFocused();

   @Shadow
   public abstract void addMessage(Component var1, @Nullable MessageSignature var2, @Nullable GuiMessageTag var3);

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/GuiMessage$Line;addedTime()I"
      )}
   )
   private void hookTimeAdded(CallbackInfo ci, @Local(ordinal = 13) int chatLineIndex) {
      try {
         this.current = this.trimmedMessages.get(chatLineIndex);
      } catch (Exception var4) {
      }
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"
      )
   )
   private int drawTextWithShadowHook(GuiGraphics instance, Font textRenderer, FormattedCharSequence text, int x, int y, int color) {
      RenderChatHudEvent renderChatHudEvent = new RenderChatHudEvent(this.current);
      EventBus.INSTANCE.dispatch(renderChatHudEvent);
      ChatTextRenderEvent chatTextRenderEvent = new ChatTextRenderEvent();
      EventBus.INSTANCE.dispatch(chatTextRenderEvent);
      if (renderChatHudEvent.isCanceled()) {
         if (renderChatHudEvent.getAnimationMode()) {
            if (chatTextRenderEvent.isCanceled()) {
               RenderManager.renderText(instance, FormattingUtil.toString(text), (int)renderChatHudEvent.getAnimation(), y, color);
               return RenderManager.textWidth(FormattingUtil.toString(text));
            } else {
               return instance.drawString(textRenderer, text, (int)renderChatHudEvent.getAnimation(), y, color);
            }
         } else {
            float alpha = (float)Mth.clamp(renderChatHudEvent.getAnimation(), 0.0, 1.0);
            float colorAlpha = color >> 24 & 0xFF;
            alpha = Math.max(0.0F, Math.min(1.0F, alpha));
            int colorAlphaInt = Math.max(10, (int)(colorAlpha * alpha));
            int color1 = alpha == 1.0F ? color : colorAlphaInt << 24 | color & 16777215;
            if (chatTextRenderEvent.isCanceled()) {
               RenderManager.renderText(instance, FormattingUtil.toString(text), 0.0F, y, color1);
               return RenderManager.textWidth(FormattingUtil.toString(text));
            } else {
               return instance.drawString(textRenderer, text, 0, y, color1);
            }
         }
      } else if (chatTextRenderEvent.isCanceled()) {
         RenderManager.renderText(instance, FormattingUtil.toString(text), 0.0F, y, color);
         return RenderManager.textWidth(FormattingUtil.toString(text));
      } else {
         return instance.drawString(textRenderer, text, 0, y, color);
      }
   }

   @ModifyExpressionValue(
      method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/GuiMessage$Line;tag()Lnet/minecraft/client/GuiMessageTag;"
      )}
   )
   private GuiMessageTag hookRender(GuiMessageTag original) {
      SignatureIndicatorEvent signatureIndicatorEvent = new SignatureIndicatorEvent();
      EventBus.INSTANCE.dispatch(signatureIndicatorEvent);
      return signatureIndicatorEvent.isCanceled() ? null : original;
   }

   @Inject(
      method = {"addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookAddMessage(Component message, MessageSignature signatureData, GuiMessageTag indicator, CallbackInfo ci) {
      ci.cancel();

      try {
         this.trimmedMessages.removeIf(msg -> ((IChatHudLineVisible)(Object)msg).getId() == this.currentId && this.currentId != 0);

         for (int i = this.allMessages.size() - 1; i > -1; i--) {
            if (((IChatHudLine)(Object)this.allMessages.get(i)).getId() == this.currentId && this.currentId != 0) {
               this.allMessages.remove(i);
            }
         }

         ChatMessageEvent chatTextEvent = new ChatMessageEvent(message);
         EventBus.INSTANCE.dispatch(chatTextEvent);
         int ix = Mth.floor(this.getWidth() / this.getScale());
         if (indicator != null && indicator.icon() != null) {
            ix -= indicator.icon().width + 4 + 2;
         }

         List<FormattedCharSequence> list = ComponentRenderUtils.wrapComponents(
            chatTextEvent.isCanceled() ? chatTextEvent.getText() : message, ix, this.minecraft.font
         );
         boolean bl = this.isChatFocused();

         for (int j = 0; j < list.size(); j++) {
            FormattedCharSequence orderedText = list.get(j);
            if (bl && this.chatScrollbarPos > 0) {
               this.newMessageSinceScroll = true;
               this.scrollChat(1);
            }

            ChatTextEvent chatMessageEvent = new ChatTextEvent(orderedText);
            EventBus.INSTANCE.dispatch(chatMessageEvent);
            boolean bl2 = j == list.size() - 1;
            Line visibleLine = new Line(mc.gui.getGuiTicks(), chatMessageEvent.isCanceled() ? chatMessageEvent.getText() : orderedText, indicator, bl2);
            ((IChatHudLineVisible)(Object)visibleLine).setId(this.currentId);
            this.trimmedMessages.add(0, visibleLine);
         }

         ChatLengthEvent chatLengthEvent = new ChatLengthEvent();
         EventBus.INSTANCE.dispatch(chatLengthEvent);
         boolean bl1 = chatLengthEvent.isCanceled();
         if (!bl1) {
            while (this.trimmedMessages.size() > 100) {
               this.trimmedMessages.remove(this.trimmedMessages.size() - 1);
            }
         }
      } catch (Exception var14) {
      }
   }

   @Inject(
      method = {"clearMessages(Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookClear(boolean clearHistory, CallbackInfo ci) {
      ChatHistoryEvent chatHistoryEvent = new ChatHistoryEvent();
      EventBus.INSTANCE.dispatch(chatHistoryEvent);
      if (chatHistoryEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Override
   public void addMessage(Component message, GuiMessageTag messageIndicator, int id) {
      this.currentId = id;
      this.addMessage(message, null, messageIndicator);
      this.currentId = 0;
   }
}

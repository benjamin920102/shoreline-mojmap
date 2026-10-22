package net.shoreline.client.mixin.gui.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
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
import net.shoreline.client.impl.event.gui.hud.*;
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

import java.util.List;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(ChatComponent.class)
public abstract class MixinChatHud implements IChatHud, Globals
{
    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    public abstract double getScale();

    @Shadow
    public abstract int getWidth();

    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private boolean newMessageSinceScroll;

    @Shadow
    public abstract void scrollChat(int scrollChat);

    @Shadow
    private int chatScrollbarPos;

    @Shadow
    public abstract boolean isChatFocused();

    @Shadow
    public abstract void addMessage(Component message, @Nullable MessageSignature signatureData, @Nullable GuiMessageTag indicator);

    @Unique
    private GuiMessage.Line current = null;

    @Unique
    private int currentId;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/GuiMessage$Line;addedTime()I"))
    private void hookTimeAdded(CallbackInfo ci, @Local(ordinal = 13) int chatLineIndex)
    {
        try
        {
            current = trimmedMessages.get(chatLineIndex);
        }
        catch (Exception ignored)
        {

        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    private int drawTextWithShadowHook(GuiGraphics instance,
                                       Font textRenderer,
                                       FormattedCharSequence text,
                                       int x,
                                       int y,
                                       int color)
    {
        RenderChatHudEvent renderChatHudEvent = new RenderChatHudEvent(current);
        EventBus.INSTANCE.dispatch(renderChatHudEvent);

        ChatTextRenderEvent chatTextRenderEvent = new ChatTextRenderEvent();
        EventBus.INSTANCE.dispatch(chatTextRenderEvent);

        if (renderChatHudEvent.isCanceled())
        {
            if (renderChatHudEvent.getAnimationMode())
            {
                if (chatTextRenderEvent.isCanceled())
                {
                    RenderManager.renderText(instance, FormattingUtil.toString(text), (int) renderChatHudEvent.getAnimation(), y, color);
                    return RenderManager.textWidth(FormattingUtil.toString(text));
                }
                return instance.drawString(textRenderer, text, (int) renderChatHudEvent.getAnimation(), y, color);
            }
            else
            {
                float alpha = (float) Mth.clamp(renderChatHudEvent.getAnimation(), 0.0f, 1.0f);

                float colorAlpha = (color >> 24) & 0xFF;
                alpha = Math.max(0.0f, Math.min(1.0f, alpha));
                int colorAlphaInt = Math.max(10, (int) (colorAlpha * alpha));

                int color1 = alpha == 1.0f ? color : (colorAlphaInt << 24) | (color & 0xFFFFFF);
                if (chatTextRenderEvent.isCanceled())
                {
                    RenderManager.renderText(instance, FormattingUtil.toString(text), 0, y, color1);
                    return RenderManager.textWidth(FormattingUtil.toString(text));
                }
                return instance.drawString(textRenderer, text, 0, y, color1);
            }
        }
        if (chatTextRenderEvent.isCanceled())
        {
            RenderManager.renderText(instance, FormattingUtil.toString(text), 0, y, color);
            return RenderManager.textWidth(FormattingUtil.toString(text));
        }
        return instance.drawString(textRenderer, text, 0, y, color);
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/GuiMessage$Line;tag()Lnet/minecraft/client/GuiMessageTag;"))
    private GuiMessageTag hookRender(GuiMessageTag original)
    {
        SignatureIndicatorEvent signatureIndicatorEvent = new SignatureIndicatorEvent();
        EventBus.INSTANCE.dispatch(signatureIndicatorEvent);
        return signatureIndicatorEvent.isCanceled() ? null : original;
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At(value = "HEAD"), cancellable = true)
    private void hookAddMessage(Component message, MessageSignature signatureData, GuiMessageTag indicator, CallbackInfo ci)
    {
        ci.cancel();
        try
        {
            trimmedMessages.removeIf(msg -> ((IChatHudLineVisible) (Object) msg).getId() == currentId && currentId != 0);
            for (int i = allMessages.size() - 1; i > -1; i--)
            {
                if (((IChatHudLine) (Object) allMessages.get(i)).getId() == currentId && currentId != 0)
                {
                    allMessages.remove(i);
                }
            }
            ChatMessageEvent chatTextEvent = new ChatMessageEvent(message);
            EventBus.INSTANCE.dispatch(chatTextEvent);
            int i = Mth.floor((double) this.getWidth() / this.getScale());
            if (indicator != null && indicator.icon() != null)
            {
                i -= indicator.icon().width + 4 + 2;
            }
            List<FormattedCharSequence> list = ComponentRenderUtils.wrapComponents(chatTextEvent.isCanceled() ? chatTextEvent.getText() : message, i, this.minecraft.font);
            boolean bl = this.isChatFocused();
            for (int j = 0; j < list.size(); ++j)
            {
                FormattedCharSequence orderedText = list.get(j);
                if (bl && this.chatScrollbarPos > 0)
                {
                    this.newMessageSinceScroll = true;
                    this.scrollChat(1);
                }
                ChatTextEvent chatMessageEvent = new ChatTextEvent(orderedText);
                EventBus.INSTANCE.dispatch(chatMessageEvent);
                boolean bl2 = j == list.size() - 1;
                GuiMessage.Line visibleLine = new GuiMessage.Line(mc.gui.getGuiTicks(), chatMessageEvent.isCanceled() ? chatMessageEvent.getText() : orderedText, indicator, bl2);
                ((IChatHudLineVisible) (Object) visibleLine).setId(currentId);
                this.trimmedMessages.add(0, visibleLine);
            }
            ChatLengthEvent chatLengthEvent = new ChatLengthEvent();
            EventBus.INSTANCE.dispatch(chatLengthEvent);
            boolean bl1 = chatLengthEvent.isCanceled();
            if (!bl1)
            {
                while (this.trimmedMessages.size() > 100)
                {
                    this.trimmedMessages.remove(this.trimmedMessages.size() - 1);
                }
            }
        }
        catch (Exception ignored)
        {

        }
    }

    @Inject(method = "clearMessages", at = @At(value = "HEAD"), cancellable = true)
    private void hookClear(boolean clearHistory, CallbackInfo ci)
    {
        ChatHistoryEvent chatHistoryEvent = new ChatHistoryEvent();
        EventBus.INSTANCE.dispatch(chatHistoryEvent);
        if (chatHistoryEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    @Override
    public void addMessage(Component message, GuiMessageTag messageIndicator, int id)
    {
        currentId = id;
        addMessage(message, null, messageIndicator);
        currentId = 0;
    }
}

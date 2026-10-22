package net.shoreline.client.mixin.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ChatScreen;
import net.shoreline.client.impl.event.gui.chat.ChatInputEvent;
import net.shoreline.client.impl.event.gui.chat.ChatKeyInputEvent;
import net.shoreline.client.impl.event.gui.chat.ChatMessageEvent;
import net.shoreline.client.impl.event.gui.chat.ChatRenderEvent;
import net.shoreline.client.impl.event.gui.screen.RenderOpenChatEvent;
import net.shoreline.client.mixin.accessor.AccessorTextFieldWidget;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author linus
 * @see ChatScreen
 * @since 1.0
 */
@Mixin(ChatScreen.class)
public class MixinChatScreen extends MixinScreen
{
    @Shadow
    protected EditBox input;

    /**
     * @param chatText
     */
    @Inject(method = "onEdited", at = @At(value = "TAIL"))
    private void hookOnChatFieldUpdate(String chatText, CallbackInfo ci)
    {
        ChatInputEvent chatInputEvent = new ChatInputEvent(chatText);
        EventBus.INSTANCE.dispatch(chatInputEvent);
    }

    /**
     * @param keyCode
     * @param scanCode
     * @param modifiers
     * @param cir
     */
    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void hookKeyPressed(int keyCode, int scanCode, int modifiers,
                                CallbackInfoReturnable<Boolean> cir)
    {
        ChatKeyInputEvent keyInputEvent = new ChatKeyInputEvent(keyCode,
                input.getValue());
        EventBus.INSTANCE.dispatch(keyInputEvent);
        if (keyInputEvent.isCanceled())
        {
            cir.cancel();
            input.setValue(keyInputEvent.getChatText());
        }
    }

    /**
     * @param chatText
     * @param addToHistory
     */
    @Inject(method = "handleChatInput", at = @At(value = "HEAD"), cancellable = true)
    private void hookSendMessage(String chatText, boolean addToHistory, CallbackInfo ci)
    {
        ChatMessageEvent.Client chatMessageEvent =
                new ChatMessageEvent.Client(chatText);
        EventBus.INSTANCE.dispatch(chatMessageEvent);
        if (chatMessageEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param context
     * @param mouseX
     * @param mouseY
     * @param delta
     * @param ci
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.BEFORE))
    private void hookRender(GuiGraphics context, int mouseX, int mouseY,
                            float delta, CallbackInfo ci)
    {
        float x = ((AccessorTextFieldWidget) input).isDrawsBackground() ?
                input.getX() + 6 : input.getX() + 2;
        float y = ((AccessorTextFieldWidget) input).isDrawsBackground() ?
                input.getY() + (input.getHeight() - 8) / 2.0f :
                input.getY();
        ChatRenderEvent chatTextRenderEvent = new ChatRenderEvent(context, x, y);
        EventBus.INSTANCE.dispatch(chatTextRenderEvent);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void hookFill(GuiGraphics instance, int x1, int y1, int x2, int y2, int color)
    {
        RenderOpenChatEvent renderOpenChatEvent = new RenderOpenChatEvent();
        EventBus.INSTANCE.dispatch(renderOpenChatEvent);
        float openAnimation = renderOpenChatEvent.isCanceled() ? 12.0f * renderOpenChatEvent.getAnimation() : 12.0f;
        instance.fill(2, (int) (this.height - 2.0f), this.width - 2, (int) ((this.height - 2.0f) - openAnimation), client.options.getBackgroundColor(Integer.MIN_VALUE));
    }

    @Override
    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addDrawableChild(T drawableElement)
    {
        return null;
    }
}

package net.shoreline.client.mixin.toast;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.shoreline.client.impl.event.toast.RenderToastEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastComponent.class)
public class MixinToastManager
{

    /**
     * @param context
     * @param ci
     */
    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void hookDraw(GuiGraphics context, CallbackInfo ci)
    {
        RenderToastEvent renderToastEvent = new RenderToastEvent();
        EventBus.INSTANCE.dispatch(renderToastEvent);
        if (renderToastEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}

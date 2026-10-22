package net.shoreline.client.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.shoreline.client.impl.event.keyboard.KeyboardInputEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @see KeyboardHandler
 * @since 1.0
 */
@Mixin(KeyboardHandler.class)
public class MixinKeyboard
{
    // @see Keyboard#client
    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * @param window
     * @param key
     * @param scancode
     * @param action
     * @param modifiers
     * @param ci
     */
    @Inject(method = "keyPress", at = @At(value = "HEAD"), cancellable = true)
    private void hookOnKey(long window, int key, int scancode, int action,
                           int modifiers, CallbackInfo ci)
    {
        if (minecraft.getWindow().getWindow() == window)
        {
            KeyboardInputEvent keyboardInputEvent = new KeyboardInputEvent(key, action);
            EventBus.INSTANCE.dispatch(keyboardInputEvent);
            // prevent keyboard input
            if (keyboardInputEvent.isCanceled())
            {
                ci.cancel();
            }
        }
    }
}
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

@Mixin({KeyboardHandler.class})
public class MixinKeyboard {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"keyPress(JIIII)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookOnKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
      if (this.minecraft.getWindow().getWindow() == window) {
         KeyboardInputEvent keyboardInputEvent = new KeyboardInputEvent(key, action);
         EventBus.INSTANCE.dispatch(keyboardInputEvent);
         if (keyboardInputEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }
}

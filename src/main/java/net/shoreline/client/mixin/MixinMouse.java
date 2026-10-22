package net.shoreline.client.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.shoreline.client.impl.event.MouseClickEvent;
import net.shoreline.client.impl.event.MouseUpdateEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public class MixinMouse {
   @Inject(
      method = {"onPress(JIII)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
      MouseClickEvent mouseClickEvent = new MouseClickEvent(button, action);
      EventBus.INSTANCE.dispatch(mouseClickEvent);
      if (mouseClickEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Redirect(
      method = {"turnPlayer(D)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
      )
   )
   public void onUpdate(LocalPlayer instance, double cursorDeltaX, double cursorDeltaY) {
      MouseUpdateEvent mouseUpdateEvent = new MouseUpdateEvent(cursorDeltaX, cursorDeltaY);
      EventBus.INSTANCE.dispatch(mouseUpdateEvent);
      if (!mouseUpdateEvent.isCanceled()) {
         instance.turn(cursorDeltaX, cursorDeltaY);
      }
   }
}

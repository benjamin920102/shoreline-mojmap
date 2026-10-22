package net.shoreline.client.mixin.gui.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.shoreline.client.impl.event.gui.hud.RenderOverlayEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BossHealthOverlay.class})
public class MixinBossBarHud {
   @Inject(
      method = {"render(Lnet/minecraft/client/gui/GuiGraphics;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRender(GuiGraphics context, CallbackInfo ci) {
      RenderOverlayEvent.BossBar renderOverlayEvent = new RenderOverlayEvent.BossBar(context);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }
   }
}

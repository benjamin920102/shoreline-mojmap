package net.shoreline.client.mixin.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.impl.event.gui.hud.RenderCrosshairEvent;
import net.shoreline.client.impl.event.gui.hud.RenderOverlayEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Gui.class})
public class MixinInGameHud implements Globals {
   @Shadow
   @Final
   private static ResourceLocation PUMPKIN_BLUR_LOCATION;
   @Shadow
   @Final
   private static ResourceLocation POWDER_SNOW_OUTLINE_LOCATION;

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("TAIL")}
   )
   private void hookRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      RenderOverlayEvent.Post renderOverlayEvent = new RenderOverlayEvent.Post(context, tickCounter.getGameTimeDeltaPartialTick(true));
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
   }

   @Inject(
      method = {"renderEffects(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderStatusEffectOverlay(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      RenderOverlayEvent.StatusEffect renderOverlayEvent = new RenderOverlayEvent.StatusEffect(context);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderPortalOverlay(Lnet/minecraft/client/gui/GuiGraphics;F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderPortalOverlay(GuiGraphics context, float nauseaStrength, CallbackInfo ci) {
      RenderOverlayEvent.Portal renderOverlayEvent = new RenderOverlayEvent.Portal(context);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderSpyglassOverlay(Lnet/minecraft/client/gui/GuiGraphics;F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderSpyglassOverlay(GuiGraphics context, float scale, CallbackInfo ci) {
      RenderOverlayEvent.Spyglass renderOverlayEvent = new RenderOverlayEvent.Spyglass(context);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderOverlay(GuiGraphics context, ResourceLocation texture, float opacity, CallbackInfo ci) {
      if (texture.getPath().equals(PUMPKIN_BLUR_LOCATION.getPath())) {
         RenderOverlayEvent.Pumpkin renderOverlayEvent = new RenderOverlayEvent.Pumpkin(context);
         EventBus.INSTANCE.dispatch(renderOverlayEvent);
         if (renderOverlayEvent.isCanceled()) {
            ci.cancel();
         }
      } else if (texture.getPath().equals(POWDER_SNOW_OUTLINE_LOCATION.getPath())) {
         RenderOverlayEvent.Frostbite renderOverlayEvent = new RenderOverlayEvent.Frostbite(context);
         EventBus.INSTANCE.dispatch(renderOverlayEvent);
         if (renderOverlayEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Redirect(
      method = {"renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)I"
      )
   )
   private int hookRenderHeldItemTooltip(GuiGraphics instance, Font textRenderer, Component text, int x, int y, int width, int color) {
      RenderOverlayEvent.ItemName renderOverlayEvent = new RenderOverlayEvent.ItemName(instance);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         return renderOverlayEvent.isUpdateXY() ? instance.drawString(mc.font, text, renderOverlayEvent.getX(), renderOverlayEvent.getY(), color, true) : 0;
      } else {
         return instance.drawString(mc.font, text, x, y, color, true);
      }
   }

   @Inject(
      method = {"renderHotbarAndDecorations(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("TAIL")}
   )
   private void hookRenderHotbar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      RenderOverlayEvent.Hotbar hotbar = new RenderOverlayEvent.Hotbar(context);
      EventBus.INSTANCE.dispatch(hotbar);
   }

   @Inject(
      method = {"renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      RenderCrosshairEvent renderCrosshairEvent = new RenderCrosshairEvent(context);
      EventBus.INSTANCE.dispatch(renderCrosshairEvent);
      if (renderCrosshairEvent.isCanceled()) {
         ci.cancel();
      }
   }
}

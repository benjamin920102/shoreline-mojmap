package net.shoreline.client.mixin.gui.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.shoreline.client.impl.event.gui.hud.RenderOverlayEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ScreenEffectRenderer.class})
public class MixinInGameOverlayRenderer {
   @Inject(
      method = {"renderFire(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookRenderFireOverlay(Minecraft client, PoseStack matrices, CallbackInfo ci) {
      RenderOverlayEvent.Fire renderOverlayEvent = new RenderOverlayEvent.Fire(null);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookRenderUnderwaterOverlay(Minecraft client, PoseStack matrices, CallbackInfo ci) {
      RenderOverlayEvent.Water renderOverlayEvent = new RenderOverlayEvent.Water(null);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderTex(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/vertex/PoseStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookRenderFireOverlay(TextureAtlasSprite sprite, PoseStack matrices, CallbackInfo ci) {
      RenderOverlayEvent.Block renderOverlayEvent = new RenderOverlayEvent.Block(null);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }
   }
}

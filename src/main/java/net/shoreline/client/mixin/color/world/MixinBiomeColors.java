package net.shoreline.client.mixin.color.world;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.shoreline.client.impl.event.color.world.BiomeColorEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BiomeColors.class})
public class MixinBiomeColors implements Globals {
   @Inject(
      method = {"getAverageColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/ColorResolver;)I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookGetColor(BlockAndTintGetter world, BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> cir) {
      if (mc.level != null) {
         BiomeColorEvent biomeColorEvent = new BiomeColorEvent(resolver);
         EventBus.INSTANCE.dispatch(biomeColorEvent);
         if (biomeColorEvent.isCanceled()) {
            cir.cancel();
            cir.setReturnValue(biomeColorEvent.getRGB());
         }
      }
   }
}

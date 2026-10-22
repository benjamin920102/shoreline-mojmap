package net.shoreline.client.mixin.biome;

import java.util.Optional;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.shoreline.client.impl.event.biome.BiomeEffectsEvent;
import net.shoreline.client.impl.event.world.SkyboxEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BiomeSpecialEffects.class})
public class MixinBiomeEffects {
   @Inject(
      method = {"getSkyColor()I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetSkyColor(CallbackInfoReturnable<Integer> cir) {
      SkyboxEvent.Sky skyboxEvent = new SkyboxEvent.Sky();
      EventBus.INSTANCE.dispatch(skyboxEvent);
      if (skyboxEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(skyboxEvent.getRGB());
      }
   }

   @Inject(
      method = {"getAmbientParticleSettings()Ljava/util/Optional;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetParticleConfig(CallbackInfoReturnable<Optional<AmbientParticleSettings>> cir) {
      BiomeEffectsEvent biomeEffectsEvent = new BiomeEffectsEvent();
      EventBus.INSTANCE.dispatch(biomeEffectsEvent);
      if (biomeEffectsEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(Optional.ofNullable(biomeEffectsEvent.getParticleConfig()));
      }
   }
}

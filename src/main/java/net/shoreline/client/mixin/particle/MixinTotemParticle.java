package net.shoreline.client.mixin.particle;

import java.awt.Color;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TotemParticle;
import net.shoreline.client.impl.event.particle.TotemParticleEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TotemParticle.class})
public abstract class MixinTotemParticle extends MixinParticle {
   @Inject(
      method = {"<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/client/particle/SpriteSet;)V"},
      at = {@At("TAIL")}
   )
   private void hookInit(
      ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet spriteProvider, CallbackInfo ci
   ) {
      TotemParticleEvent totemParticleEvent = new TotemParticleEvent();
      EventBus.INSTANCE.dispatch(totemParticleEvent);
      if (totemParticleEvent.isCanceled()) {
         Color color = totemParticleEvent.getColor();
         this.setColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
      }
   }
}

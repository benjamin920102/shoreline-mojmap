package net.shoreline.client.impl.event.particle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class ParticleEvent extends Event {
   private final ParticleOptions particle;

   public ParticleEvent(ParticleOptions particle) {
      this.particle = particle;
   }

   public ParticleOptions getParticle() {
      return this.particle;
   }

   public ParticleType<?> getParticleType() {
      return this.particle.getType();
   }

   @Cancelable
   public static class Emitter extends ParticleEvent {
      public Emitter(ParticleOptions particle) {
         super(particle);
      }
   }
}

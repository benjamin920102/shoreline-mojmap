package net.shoreline.client.impl.event.particle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

/**
 * @author linus
 * @since 1.0
 */
@Cancelable
public class ParticleEvent extends Event
{
    private final ParticleOptions particle;

    /**
     * @param particle
     */
    public ParticleEvent(ParticleOptions particle)
    {
        this.particle = particle;
    }

    /**
     * @return
     */
    public ParticleOptions getParticle()
    {
        return particle;
    }

    /**
     * @return
     */
    public ParticleType<?> getParticleType()
    {
        return particle.getType();
    }

    @Cancelable
    public static class Emitter extends ParticleEvent
    {
        /**
         * @param particle
         */
        public Emitter(ParticleOptions particle)
        {
            super(particle);
        }
    }
}

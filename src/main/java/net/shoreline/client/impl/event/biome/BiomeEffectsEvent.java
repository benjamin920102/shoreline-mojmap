package net.shoreline.client.impl.event.biome;

import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class BiomeEffectsEvent extends Event
{

    private AmbientParticleSettings particleConfig;

    public AmbientParticleSettings getParticleConfig()
    {
        return particleConfig;
    }

    public void setParticleConfig(AmbientParticleSettings particleConfig)
    {
        this.particleConfig = particleConfig;
    }
}

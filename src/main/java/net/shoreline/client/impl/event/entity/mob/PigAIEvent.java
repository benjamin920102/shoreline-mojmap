package net.shoreline.client.impl.event.entity.mob;

import net.minecraft.world.entity.animal.Pig;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class PigAIEvent extends Event
{
    private final Pig pigEntity;

    public PigAIEvent(Pig pigEntity)
    {
        this.pigEntity = pigEntity;
    }

    public Pig getPigEntity()
    {
        return pigEntity;
    }
}

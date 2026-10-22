package net.shoreline.client.impl.event.world;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class PlaySoundEvent extends Event
{

    private final Vec3 pos;
    private final SoundEvent event;
    private final SoundSource category;

    public PlaySoundEvent(Vec3 pos, SoundEvent event, SoundSource category)
    {
        this.pos = pos;
        this.event = event;
        this.category = category;
    }

    public Vec3 getPos()
    {
        return pos;
    }

    public SoundEvent getSoundEvent()
    {
        return event;
    }

    public SoundSource getCategory()
    {
        return category;
    }
}

package net.shoreline.client.impl.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.shoreline.eventbus.event.Event;

public final class EntityRotationVectorEvent extends Event
{
    private final Entity entity;
    private final float tickDelta;
    private Vec3 position;

    public EntityRotationVectorEvent(float tickDelta, Entity entity, Vec3 position)
    {
        this.entity = entity;
        this.tickDelta = tickDelta;
        this.position = position;
    }

    public Entity getEntity()
    {
        return entity;
    }

    public float getTickDelta()
    {
        return tickDelta;
    }

    public Vec3 getPosition()
    {
        return position;
    }

    public void setPosition(Vec3 position)
    {
        this.position = position;
    }
}

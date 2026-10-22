package net.shoreline.client.impl.event.camera;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.shoreline.eventbus.event.Event;

public class EntityCameraPositionEvent extends Event
{

    private Vec3 position;
    private final float tickDelta;
    private final Entity entity;

    public EntityCameraPositionEvent(Vec3 position, Entity entity, float tickDelta)
    {
        this.position = position;
        this.tickDelta = tickDelta;
        this.entity = entity;
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

    public Entity getEntity()
    {
        return entity;
    }
}
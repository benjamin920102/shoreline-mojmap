package net.shoreline.client.impl.event.entity;

import net.minecraft.world.phys.Vec3;
import net.shoreline.eventbus.event.Event;

public class EntityPositionEvent extends Event
{
    private final Vec3 updatePos;
    private final Vec3 prevPos;

    public EntityPositionEvent(Vec3 updatePos, Vec3 prevPos)
    {
        this.updatePos = updatePos;
        this.prevPos = prevPos;
    }

    public Vec3 getUpdatePos()
    {
        return updatePos;
    }

    public Vec3 getPrevPos()
    {
        return prevPos;
    }
}

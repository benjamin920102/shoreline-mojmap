package net.shoreline.client.impl.event.entity;

import net.minecraft.world.phys.Vec3;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.StageEvent;

@Cancelable
public class UpdateVelocityEvent extends StageEvent
{
    //
    private final Vec3 movementInput;
    private final float speed;
    private final float yaw;
    private Vec3 velocity;

    public UpdateVelocityEvent(Vec3 movementInput, float speed, float yaw, Vec3 velocity)
    {
        this.movementInput = movementInput;
        this.speed = speed;
        this.yaw = yaw;
        this.velocity = velocity;
    }

    public Vec3 getMovementInput()
    {
        return this.movementInput;
    }

    public float getSpeed()
    {
        return this.speed;
    }

    public Vec3 getVelocity()
    {
        return this.velocity;
    }

    public void setVelocity(Vec3 velocity)
    {
        this.velocity = velocity;
    }
}

package net.shoreline.client.impl.event.render.entity;

import net.minecraft.client.player.AbstractClientPlayer;
import net.shoreline.client.mixin.render.entity.MixinPlayerEntityRenderer;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

/**
 * @author linus
 * @see MixinPlayerEntityRenderer
 * @since 1.0
 */
@Cancelable
public class RenderPlayerEvent extends Event
{
    //
    private final AbstractClientPlayer entity;
    //
    private float yaw;
    private float pitch;

    /**
     * @param entity
     */
    public RenderPlayerEvent(AbstractClientPlayer entity)
    {
        this.entity = entity;
    }

    /**
     * @return
     */
    public AbstractClientPlayer getEntity()
    {
        return entity;
    }


    public float getYaw()
    {
        return yaw;
    }

    public void setYaw(float yaw)
    {
        this.yaw = yaw;
    }

    public float getPitch()
    {
        return pitch;
    }

    public void setPitch(float pitch)
    {
        this.pitch = pitch;
    }
}

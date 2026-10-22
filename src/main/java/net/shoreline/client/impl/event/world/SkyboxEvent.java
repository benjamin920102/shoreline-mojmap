package net.shoreline.client.impl.event.world;

import net.minecraft.world.phys.Vec3;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

import java.awt.*;

/**
 * @author linus
 * @since 1.0
 */
public class SkyboxEvent extends Event
{
    private Color color;

    public Color getColor()
    {
        return color;
    }

    public Vec3 getColorVec()
    {
        return new Vec3(color.getRed() / 255.0, color.getGreen() / 255.0, color.getBlue() / 255.0);
    }

    public int getRGB()
    {
        return color.getRGB();
    }

    public void setColor(Color color)
    {
        this.color = color;
    }

    @Cancelable
    public static class Sky extends SkyboxEvent
    {

    }

    @Cancelable
    public static class Cloud extends SkyboxEvent
    {

    }

    @Cancelable
    public static class Fog extends SkyboxEvent
    {
        private final float tickDelta;

        public Fog(float tickDelta)
        {
            this.tickDelta = tickDelta;
        }

        public float getTickDelta()
        {
            return tickDelta;
        }
    }
}

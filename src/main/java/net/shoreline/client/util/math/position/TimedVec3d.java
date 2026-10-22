package net.shoreline.client.util.math.position;

import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;

/**
 * @author linus
 * @since 1.0
 */
public record TimedVec3d(Vec3 pos, long time) implements Position
{
    /**
     * @param pos
     * @param time
     */
    public TimedVec3d
    {

    }

    /**
     * Returns the X coordinate.
     */
    @Override
    public double x()
    {
        return pos.x();
    }

    /**
     * Returns the Y coordinate.
     */
    @Override
    public double y()
    {
        return pos.y();
    }

    /**
     * Returns the Z coordinate.
     */
    @Override
    public double z()
    {
        return pos.z();
    }
}

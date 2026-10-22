package net.shoreline.client.util.math.position;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class DirectionUtil
{
    public static Vec3 getDirectionOffsetPos(BlockPos pos, Direction direction)
    {
        Vec3 pos1 = pos.getCenter();
        return switch (direction)
        {
            case UP -> pos1.add(0.0, 0.5, 0.0);
            case DOWN -> pos1.add(0.0, -0.5, 0.0);
            case NORTH -> pos1.add(0.0, 0.0, -0.5);
            case SOUTH -> pos1.add(0.0, 0.0, 0.5);
            case WEST -> pos1.add(-0.5, 0.0, 0.0);
            case EAST -> pos1.add(0.5, 0.0, 0.0);
        };
    }
}

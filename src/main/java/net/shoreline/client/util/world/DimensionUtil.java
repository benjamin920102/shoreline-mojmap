package net.shoreline.client.util.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.shoreline.client.util.Globals;

public class DimensionUtil implements Globals
{
    public static int getDimension()
    {
        ResourceKey<Level> world = mc.level.dimension();
        if (world == Level.OVERWORLD)
        {
            return 1;
        }
        if (world == Level.NETHER)
        {
            return 0;
        }
        if (world == Level.END)
        {
            return -1;
        }
        return 1;
    }
}

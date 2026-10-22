package net.shoreline.client.impl.event.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.shoreline.eventbus.event.Event;

public class UnloadChunkBlocksEvent extends Event
{
    private final LevelChunk chunk;

    public UnloadChunkBlocksEvent(LevelChunk chunk)
    {
        this.chunk = chunk;
    }

    public boolean contains(BlockPos blockPos)
    {
        int chunkX = blockPos.getX() >> 4;
        int chunkZ = blockPos.getZ() >> 4;
        return chunkX == chunk.getPos().x && chunkZ == chunk.getPos().z;
    }
}

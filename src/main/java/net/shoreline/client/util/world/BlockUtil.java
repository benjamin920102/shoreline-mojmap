package net.shoreline.client.util.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.shoreline.client.util.Globals;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linus
 * @since 1.0
 */
public class BlockUtil implements Globals
{
    /**
     * @return
     */
    public static List<BlockEntity> blockEntities()
    {
        List<BlockEntity> list = new ArrayList<>();
        for (LevelChunk chunk : loadedChunks())
        {
            list.addAll(chunk.getBlockEntities().values());
        }
        return list;
    }

    /**
     * Credit https://github.com/BleachDev/BleachHack/blob/1.19.4/src/main/java/org/bleachhack/util/world/WorldUtils.java#L83
     *
     * @return
     */
    public static List<LevelChunk> loadedChunks()
    {
        List<LevelChunk> chunks = new ArrayList<>();
        int viewDist = mc.options.renderDistance().get();
        for (int x = -viewDist; x <= viewDist; x++)
        {
            for (int z = -viewDist; z <= viewDist; z++)
            {
                LevelChunk chunk = mc.level.getChunkSource().getChunkNow(
                        (int) mc.player.getX() / 16 + x, (int) mc.player.getZ() / 16 + z);
                if (chunk != null)
                {
                    chunks.add(chunk);
                }
            }
        }
        return chunks;
    }

    /**
     * @param pos
     * @return
     */
    public static boolean isBlockAccessible(BlockPos pos)
    {
        return mc.level.isEmptyBlock(pos) && !mc.level.isEmptyBlock(pos.offset(0, -1, 0))
                && mc.level.isEmptyBlock(pos.offset(0, 1, 0)) && mc.level.isEmptyBlock(pos.offset(0, 2, 0));
    }


    /**
     * @param x
     * @param z
     * @return
     */
    public static boolean isBlockLoaded(double x, double z)
    {
        ChunkSource chunkManager = mc.level.getChunkSource();
        if (chunkManager != null)
        {
            return chunkManager.hasChunk(SectionPos.posToSectionCoord(x),
                    SectionPos.posToSectionCoord(z));
        }
        return false;
    }
}

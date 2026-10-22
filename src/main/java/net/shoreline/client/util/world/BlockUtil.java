package net.shoreline.client.util.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.shoreline.client.util.Globals;

public class BlockUtil implements Globals {
   public static List<BlockEntity> blockEntities() {
      List<BlockEntity> list = new ArrayList<>();

      for (LevelChunk chunk : loadedChunks()) {
         list.addAll(chunk.getBlockEntities().values());
      }

      return list;
   }

   public static List<LevelChunk> loadedChunks() {
      List<LevelChunk> chunks = new ArrayList<>();
      int viewDist = mc.options.renderDistance().get();

      for (int x = -viewDist; x <= viewDist; x++) {
         for (int z = -viewDist; z <= viewDist; z++) {
            LevelChunk chunk = mc.level.getChunkSource().getChunkNow((int)mc.player.getX() / 16 + x, (int)mc.player.getZ() / 16 + z);
            if (chunk != null) {
               chunks.add(chunk);
            }
         }
      }

      return chunks;
   }

   public static boolean isBlockAccessible(BlockPos pos) {
      return mc.level.isEmptyBlock(pos)
         && !mc.level.isEmptyBlock(pos.offset(0, -1, 0))
         && mc.level.isEmptyBlock(pos.offset(0, 1, 0))
         && mc.level.isEmptyBlock(pos.offset(0, 2, 0));
   }

   public static boolean isBlockLoaded(double x, double z) {
      ChunkSource chunkManager = mc.level.getChunkSource();
      return chunkManager != null ? chunkManager.hasChunk(SectionPos.posToSectionCoord(x), SectionPos.posToSectionCoord(z)) : false;
   }
}

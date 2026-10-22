package net.shoreline.client.util.math.position;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.shoreline.client.util.world.BlastResistantBlocks;

public class PositionUtil {
   public static AABB enclosingBox(List<BlockPos> posList) {
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;
      int maxZ = Integer.MIN_VALUE;

      for (BlockPos blockPos : posList) {
         if (blockPos.getX() < minX) {
            minX = blockPos.getX();
         }

         if (blockPos.getY() < minY) {
            minY = blockPos.getY();
         }

         if (blockPos.getZ() < minZ) {
            minZ = blockPos.getZ();
         }

         if (blockPos.getX() > maxX) {
            maxX = blockPos.getX();
         }

         if (blockPos.getY() > maxY) {
            maxY = blockPos.getY();
         }

         if (blockPos.getZ() > maxZ) {
            maxZ = blockPos.getZ();
         }
      }

      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public static BlockPos getRoundedBlockPos(double x, double y, double z) {
      int flooredX = Mth.floor(x);
      int flooredY = (int)Math.round(y);
      int flooredZ = Mth.floor(z);
      return new BlockPos(flooredX, flooredY, flooredZ);
   }

   public static boolean isBedrock(AABB box, BlockPos pos) {
      return getAllInBox(box, pos).stream().anyMatch(BlastResistantBlocks::isUnbreakable);
   }

   public static List<BlockPos> getAllInBox(AABB box, BlockPos pos) {
      List<BlockPos> intersections = new ArrayList<>();

      for (int x = (int)Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
         for (int z = (int)Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
            intersections.add(new BlockPos(x, pos.getY(), z));
         }
      }

      return intersections;
   }

   public static List<BlockPos> getAllInBox(AABB box) {
      List<BlockPos> intersections = new ArrayList<>();

      for (int x = (int)Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
         for (int y = (int)Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
            for (int z = (int)Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
               intersections.add(new BlockPos(x, y, z));
            }
         }
      }

      return intersections;
   }
}

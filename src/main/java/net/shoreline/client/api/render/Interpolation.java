package net.shoreline.client.api.render;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.util.Globals;

public class Interpolation implements Globals {
   public static Vec3 getRenderPosition(Vec3 pos, Vec3 lastPos, float tickDelta) {
      return new Vec3(
         pos.x - Mth.lerp((double)tickDelta, lastPos.x, pos.x),
         pos.y - Mth.lerp((double)tickDelta, lastPos.y, pos.y),
         pos.z - Mth.lerp((double)tickDelta, lastPos.z, pos.z)
      );
   }

   public static Vec3 getRenderPosition(Entity entity, float tickDelta) {
      return new Vec3(
         entity.getX() - Mth.lerp((double)tickDelta, entity.xOld, entity.getX()),
         entity.getY() - Mth.lerp((double)tickDelta, entity.yOld, entity.getY()),
         entity.getZ() - Mth.lerp((double)tickDelta, entity.zOld, entity.getZ())
      );
   }

   public static Vec3 getInterpolatedPosition(Entity entity, float tickDelta) {
      return new Vec3(
         entity.xo + (entity.getX() - entity.xo) * tickDelta,
         entity.yo + (entity.getY() - entity.yo) * tickDelta,
         entity.zo + (entity.getZ() - entity.zo) * tickDelta
      );
   }

   public static float interpolateFloat(float prev, float value, float factor) {
      return prev + (value - prev) * factor;
   }

   public static double interpolateDouble(double prev, double value, double factor) {
      return prev + (value - prev) * factor;
   }

   public static AABB getInterpolatedBox(AABB prevBox, AABB box) {
      double delta = mc.isPaused() ? 1.0 : mc.getTimer().getGameTimeDeltaPartialTick(true);
      return new AABB(
         interpolateDouble(prevBox.minX, box.minX, delta),
         interpolateDouble(prevBox.minY, box.minY, delta),
         interpolateDouble(prevBox.minZ, box.minZ, delta),
         interpolateDouble(prevBox.maxX, box.maxX, delta),
         interpolateDouble(prevBox.maxY, box.maxY, delta),
         interpolateDouble(prevBox.maxZ, box.maxZ, delta)
      );
   }

   public static AABB getInterpolatedEntityBox(Entity entity) {
      AABB box = entity.getBoundingBox();
      AABB prevBox = entity.getBoundingBox().move(entity.xo - entity.getX(), entity.yo - entity.getY(), entity.zo - entity.getZ());
      return getInterpolatedBox(prevBox, box);
   }
}

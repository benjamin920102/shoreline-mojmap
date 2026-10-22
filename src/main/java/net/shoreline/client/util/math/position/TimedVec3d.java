package net.shoreline.client.util.math.position;

import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;

public record TimedVec3d(Vec3 pos, long time) implements Position {
   @Override
   public double x() {
      return this.pos.x();
   }

   @Override
   public double y() {
      return this.pos.y();
   }

   @Override
   public double z() {
      return this.pos.z();
   }
}

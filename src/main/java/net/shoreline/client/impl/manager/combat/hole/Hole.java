package net.shoreline.client.impl.manager.combat.hole;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Hole implements Position {
   private final List<BlockPos> holeOffsets;
   private final BlockPos origin;
   private final HoleType safety;

   public Hole(BlockPos origin, HoleType safety, BlockPos... holeOffsets) {
      this.origin = origin;
      this.safety = safety;
      this.holeOffsets = Lists.newArrayList(holeOffsets);
      this.holeOffsets.add(origin);
   }

   public double squaredDistanceTo(Entity entity) {
      return entity.getEyePosition().distanceToSqr(this.getCenter());
   }

   public boolean isStandard() {
      return this.holeOffsets.size() == 5;
   }

   public boolean isDouble() {
      return this.holeOffsets.size() == 8;
   }

   public boolean isDoubleX() {
      return this.isDouble() && this.holeOffsets.contains(this.origin.offset(2, 0, 0));
   }

   public boolean isDoubleZ() {
      return this.isDouble() && this.holeOffsets.contains(this.origin.offset(0, 0, 2));
   }

   public boolean isQuad() {
      return this.holeOffsets.size() == 12;
   }

   public HoleType getSafety() {
      return this.safety;
   }

   public BlockPos getPos() {
      return this.origin;
   }

   public List<BlockPos> getHoleOffsets() {
      return this.holeOffsets;
   }

   public boolean addHoleOffsets(BlockPos... off) {
      return this.holeOffsets.addAll(Arrays.asList(off));
   }

   public Vec3 getCenter() {
      BlockPos center;
      if (this.isDoubleX()) {
         center = this.origin.offset(1, 0, 0);
      } else if (this.isDoubleZ()) {
         center = this.origin.offset(0, 0, -1);
      } else {
         if (!this.isQuad()) {
            return this.origin.getCenter();
         }

         center = this.origin.offset(1, 0, -1);
      }

      return Vec3.atLowerCornerOf(center);
   }

   public AABB getBoundingBox(double height) {
      AABB render = null;
      if (this.getSafety() == HoleType.VOID) {
         render = new AABB(this.x(), this.y(), this.z(), this.x() + 1.0, this.y() + 1.0, this.z() + 1.0);
      } else if (this.isDoubleX()) {
         render = new AABB(this.x(), this.y(), this.z(), this.x() + 2.0, this.y() + height, this.z() + 1.0);
      } else if (this.isDoubleZ()) {
         render = new AABB(this.x(), this.y(), this.z(), this.x() + 1.0, this.y() + height, this.z() + 2.0);
      } else if (this.isQuad()) {
         render = new AABB(this.x(), this.y(), this.z(), this.x() + 2.0, this.y() + height, this.z() + 2.0);
      } else if (this.isStandard()) {
         render = new AABB(this.x(), this.y(), this.z(), this.x() + 1.0, this.y() + height, this.z() + 1.0);
      }

      return render;
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof Hole hole ? new HashSet<>(hole.getHoleOffsets()).containsAll(this.holeOffsets) : false;
   }

   @Override
   public double x() {
      return this.origin.getX();
   }

   @Override
   public double y() {
      return this.origin.getY();
   }

   @Override
   public double z() {
      return this.origin.getZ();
   }
}

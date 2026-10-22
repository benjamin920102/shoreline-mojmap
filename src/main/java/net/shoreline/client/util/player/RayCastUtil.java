package net.shoreline.client.util.player;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.util.Globals;

public final class RayCastUtil implements Globals {
   public static HitResult raycastEntity(double reach, Vec3 position, float[] angles) {
      Camera view = mc.gameRenderer.getMainCamera();
      Vec3 vec3d2 = RotationUtil.getRotationVector(angles[1], angles[0]);
      Vec3 vec3d3 = position.add(vec3d2.x * reach, vec3d2.y * reach, vec3d2.z * reach);
      AABB box = new AABB(position, position).expandTowards(vec3d2.scale(reach)).inflate(1.0, 1.0, 1.0);
      return ProjectileUtil.getEntityHitResult(view.getEntity(), position, vec3d3, box, entity -> !entity.isSpectator() && entity.isPickable(), reach * reach);
   }

   public static HitResult raycastEntity(double reach) {
      Camera view = mc.gameRenderer.getMainCamera();
      Vec3 vec3d = view.getPosition();
      Vec3 vec3d2 = RotationUtil.getRotationVector(view.getXRot(), view.getYRot());
      Vec3 vec3d3 = vec3d.add(vec3d2.x * reach, vec3d2.y * reach, vec3d2.z * reach);
      AABB box = view.getEntity().getBoundingBox().expandTowards(vec3d2.scale(reach)).inflate(1.0, 1.0, 1.0);
      return ProjectileUtil.getEntityHitResult(view.getEntity(), vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.isPickable(), reach * reach);
   }

   public static HitResult rayCast(double reach, float[] angles) {
      double eyeHeight = mc.player.getEyeHeight();
      Vec3 eyes = new Vec3(mc.player.getX(), mc.player.getY() + eyeHeight, mc.player.getZ());
      return rayCast(reach, eyes, angles);
   }

   public static HitResult rayCast(double reach, Vec3 position, float[] angles) {
      if (!Float.isNaN(angles[0]) && !Float.isNaN(angles[1])) {
         Vec3 rotationVector = RotationUtil.getRotationVector(angles[1], angles[0]);
         return mc.level
            .clip(
               new ClipContext(
                  position, position.add(rotationVector.x * reach, rotationVector.y * reach, rotationVector.z * reach), Block.COLLIDER, Fluid.NONE, mc.player
               )
            );
      } else {
         return null;
      }
   }
}

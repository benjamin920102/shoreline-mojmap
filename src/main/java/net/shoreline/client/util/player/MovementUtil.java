package net.shoreline.client.util.player;

import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec2;
import net.shoreline.client.util.Globals;

public class MovementUtil implements Globals {
   public static boolean isInputtingMovement() {
      return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }

   public static boolean isMovingInput() {
      return mc.player.input.forwardImpulse != 0.0F || mc.player.input.leftImpulse != 0.0F;
   }

   public static boolean isMoving() {
      double d = mc.player.getX() - mc.player.xLast;
      double e = mc.player.getY() - mc.player.yLast1;
      double f = mc.player.getZ() - mc.player.zLast;
      return Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4);
   }

   public static void applySneak() {
      float modifier = Mth.clamp(0.3F + EnchantmentUtil.getLevel(mc.player.getItemBySlot(EquipmentSlot.FEET), Enchantments.SWIFT_SNEAK) * 0.15F, 0.0F, 1.0F);
      mc.player.input.forwardImpulse *= modifier;
      mc.player.input.leftImpulse *= modifier;
   }

   public static Vec2 applySafewalk(double motionX, double motionZ) {
      double offset = 0.05;
      double moveX = motionX;
      double moveZ = motionZ;
      float fallDist = -mc.player.maxUpStep();
      if (!mc.player.onGround()) {
         fallDist = -1.5F;
      }

      while (moveX != 0.0 && mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(moveX, fallDist, 0.0))) {
         if (moveX < 0.05 && moveX >= -0.05) {
            moveX = 0.0;
         } else if (moveX > 0.0) {
            moveX -= 0.05;
         } else {
            moveX += 0.05;
         }
      }

      while (moveZ != 0.0 && mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0.0, fallDist, moveZ))) {
         if (moveZ < 0.05 && moveZ >= -0.05) {
            moveZ = 0.0;
         } else if (moveZ > 0.0) {
            moveZ -= 0.05;
         } else {
            moveZ += 0.05;
         }
      }

      while (moveX != 0.0 && moveZ != 0.0 && mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(moveX, fallDist, moveZ))) {
         if (moveX < 0.05 && moveX >= -0.05) {
            moveX = 0.0;
         } else if (moveX > 0.0) {
            moveX -= 0.05;
         } else {
            moveX += 0.05;
         }

         if (moveZ < 0.05 && moveZ >= -0.05) {
            moveZ = 0.0;
         } else if (moveZ > 0.0) {
            moveZ -= 0.05;
         } else {
            moveZ += 0.05;
         }
      }

      return new Vec2((float)moveX, (float)moveZ);
   }

   public static float getYawOffset(Input input, float rotationYaw) {
      if (input.forwardImpulse < 0.0F) {
         rotationYaw += 180.0F;
      }

      float forward = 1.0F;
      if (input.forwardImpulse < 0.0F) {
         forward = -0.5F;
      } else if (input.forwardImpulse > 0.0F) {
         forward = 0.5F;
      }

      float strafe = input.leftImpulse;
      if (strafe > 0.0F) {
         rotationYaw -= 90.0F * forward;
      }

      if (strafe < 0.0F) {
         rotationYaw += 90.0F * forward;
      }

      return rotationYaw;
   }
}

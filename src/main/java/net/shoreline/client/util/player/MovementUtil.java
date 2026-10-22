package net.shoreline.client.util.player;

import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec2;
import net.shoreline.client.util.Globals;

/**
 * @author linus
 * @since 1.0
 */
public class MovementUtil implements Globals
{
    /**
     * @return
     */
    public static boolean isInputtingMovement()
    {
        return mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown();
    }

    /**
     * @return
     */
    public static boolean isMovingInput()
    {
        return mc.player.input.forwardImpulse != 0.0f
                || mc.player.input.leftImpulse != 0.0f;
    }

    /**
     * @return
     */
    public static boolean isMoving()
    {
        double d = mc.player.getX() - mc.player.xLast;
        double e = mc.player.getY() - mc.player.yLast1;
        double f = mc.player.getZ() - mc.player.zLast;
        return Mth.lengthSquared(d, e, f) > Mth.square(2.0e-4);
    }

    public static void applySneak()
    {
        final float modifier = Mth.clamp(0.3f + (EnchantmentUtil.getLevel(mc.player.getItemBySlot(EquipmentSlot.FEET), Enchantments.SWIFT_SNEAK) * 0.15F), 0.0f, 1.0f);
        mc.player.input.forwardImpulse *= modifier;
        mc.player.input.leftImpulse *= modifier;
    }

    public static Vec2 applySafewalk(final double motionX, final double motionZ)
    {
        final double offset = 0.05;

        double moveX = motionX;
        double moveZ = motionZ;

        float fallDist = -mc.player.maxUpStep();
        if (!mc.player.onGround())
        {
            fallDist = -1.5f;
        }

        while (moveX != 0.0 && mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(moveX, fallDist, 0.0)))
        {
            if (moveX < offset && moveX >= -offset)
            {
                moveX = 0.0;
            }
            else if (moveX > 0.0)
            {
                moveX -= offset;
            }
            else
            {
                moveX += offset;
            }
        }

        while (moveZ != 0.0 && mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0.0, fallDist, moveZ)))
        {
            if (moveZ < offset && moveZ >= -offset)
            {
                moveZ = 0.0;
            }
            else if (moveZ > 0.0)
            {
                moveZ -= offset;
            }
            else
            {
                moveZ += offset;
            }
        }

        while (moveX != 0.0 && moveZ != 0.0 && mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(moveX, fallDist, moveZ)))
        {
            if (moveX < offset && moveX >= -offset)
            {
                moveX = 0.0;
            }
            else if (moveX > 0.0)
            {
                moveX -= offset;
            }
            else
            {
                moveX += offset;
            }

            if (moveZ < offset && moveZ >= -offset)
            {
                moveZ = 0.0;
            }
            else if (moveZ > 0.0)
            {
                moveZ -= offset;
            }
            else
            {
                moveZ += offset;
            }
        }

        return new Vec2((float) moveX, (float) moveZ);

    }

    public static float getYawOffset(Input input, float rotationYaw)
    {
        if (input.forwardImpulse < 0.0f) rotationYaw += 180.0f;

        float forward = 1.0f;
        if (input.forwardImpulse < 0.0f)
        {
            forward = -0.5f;
        }
        else if (input.forwardImpulse > 0.0f)
        {
            forward = 0.5f;
        }

        float strafe = input.leftImpulse;
        if (strafe > 0.0f) rotationYaw -= 90.0f * forward;
        if (strafe < 0.0f) rotationYaw += 90.0f * forward;
        return rotationYaw;
    }
}

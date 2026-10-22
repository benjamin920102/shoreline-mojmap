package net.shoreline.client.util.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.chat.ChatUtil;

/**
 * @author linus
 * @since 1.0
 */
public class EntityUtil implements Globals
{
    /**
     *
     * @param entity
     * @return
     */
    public static BlockPos getRoundedBlockPos(Entity entity)
    {
        return new BlockPos(entity.getBlockX(), (int) Math.round(entity.getY()), entity.getBlockZ());
    }

    /**
     * @param entity
     * @return
     */
    public static float getHealth(Entity entity)
    {
        if (entity instanceof LivingEntity e)
        {
            return e.getHealth() + e.getAbsorptionAmount();
        }
        return 0.0f;
    }

    /**
     * @param e
     * @return
     */
    public static boolean isMonster(Entity e)
    {
        return e instanceof Enemy && !isNeutralInternal(e);
    }

    private static boolean isNeutralInternal(Entity e)
    {
        return e instanceof EnderMan enderman && !enderman.isAggressive()
                || e instanceof ZombifiedPiglin piglin && !piglin.isAggressive()
                || e instanceof Wolf wolf && !wolf.isAggressive()
                || e instanceof IronGolem ironGolem && !ironGolem.isAggressive()
                || e instanceof Bee bee && !bee.isAggressive();
    }

    /**
     * @param e
     * @return
     */
    public static boolean isNeutral(Entity e)
    {
        return e instanceof EnderMan || e instanceof ZombifiedPiglin || e instanceof Wolf || e instanceof IronGolem;
    }

    /**
     * @param e
     * @return
     */
    public static boolean isPassive(Entity e)
    {
        return e instanceof AgeableMob || e instanceof AmbientCreature || e instanceof Squid;
    }

    public static boolean isVehicle(Entity e)
    {
        return e instanceof Boat || e instanceof Minecart
                || e instanceof MinecartFurnace
                || e instanceof MinecartChest;
    }
}

package net.shoreline.client.util.player;

import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.math.position.PositionUtil;

/**
 * @author linus & xgraza
 * @since 1.0
 */
public final class PlayerUtil implements Globals
{
    public static float getLocalPlayerHealth()
    {
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    // from MC source
    public static int computeFallDamage(float fallDistance, float damageMultiplier)
    {
        if (mc.player.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE))
        {
            return 0;
        }
        else
        {
            final MobEffectInstance statusEffectInstance = mc.player.getEffect(MobEffects.JUMP);
            final float f = statusEffectInstance == null ? 0.0F : (float) (statusEffectInstance.getAmplifier() + 1);
            return Mth.ceil((fallDistance - 3.0F - f) * damageMultiplier);
        }
    }

    public static boolean isHolding(final Item item)
    {
        ItemStack itemStack = mc.player.getMainHandItem();
        if (!itemStack.isEmpty() && itemStack.getItem() == item)
        {
            return true;
        }
        itemStack = mc.player.getOffhandItem();
        return !itemStack.isEmpty() && itemStack.getItem() == item;
    }

    public static boolean isHotbarKeysPressed()
    {
        for (KeyMapping binding : mc.options.keyHotbarSlots)
        {
            if (binding.isDown())
            {
                return true;
            }
        }
        return false;
    }

    public static boolean inWeb(double expandBb)
    {
        for (BlockPos blockPos : PositionUtil.getAllInBox(mc.player.getBoundingBox().inflate(expandBb)))
        {
            BlockState state = mc.level.getBlockState(blockPos);
            if (state.getBlock() instanceof WebBlock)
            {
                return true;
            }
        }
        return false;
    }
}

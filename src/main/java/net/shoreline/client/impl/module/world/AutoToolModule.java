package net.shoreline.client.impl.module.world;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.AttackBlockEvent;
import net.shoreline.client.util.player.EnchantmentUtil;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * @author xgraza
 * @since 1.0
 */
public final class AutoToolModule extends ToggleModule
{
    private static AutoToolModule INSTANCE;

    public AutoToolModule()
    {
        super("AutoTool", "Automatically switches to a tool before mining", ModuleCategory.WORLD);
        INSTANCE = this;
    }

    public static AutoToolModule getInstance()
    {
        return INSTANCE;
    }

    @EventListener
    public void onBreakBlock(final AttackBlockEvent event)
    {
        final BlockState state = mc.level.getBlockState(event.getPos());
        final int blockSlot = getBestToolNoFallback(state);
        if (blockSlot != -1)
        {
            mc.player.getInventory().selected = blockSlot;
        }
    }

    public int getBestTool(final BlockState state)
    {
        int slot = getBestToolNoFallback(state);
        if (slot != -1)
        {
            return slot;
        }
        return mc.player.getInventory().selected;
    }

    public int getBestToolNoFallback(final BlockState state)
    {
        if (state.getBlock() == Blocks.COBWEB)
        {
            for (int i = 0; i < 9; i++)
            {
                final ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem))
                {
                    continue;
                }
                return i;
            }
        }
        int slot = -1;
        float bestTool = 0.0f;
        for (int i = 0; i < 9; i++)
        {
            final ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof TieredItem))
            {
                continue;
            }
            float speed = stack.getDestroySpeed(state);
            final int efficiency = EnchantmentUtil.getLevel(stack, Enchantments.EFFICIENCY);
            if (efficiency > 0)
            {
                speed += efficiency * efficiency + 1.0f;
            }
            if (speed > bestTool)
            {
                bestTool = speed;
                slot = i;
            }
        }
        return slot;
    }
}

package net.shoreline.client.impl.module.misc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * @author linus
 * @since 1.0
 */
public class AutoEatModule extends ToggleModule
{
    //
    Config<Float> hungerConfig = register(new NumberConfig<>("Hunger", "The minimum hunger level before eating", 1.0f, 19.0f, 20.0f));

    /**
     *
     */
    public AutoEatModule()
    {
        super("AutoEat", "Automatically eats when losing hunger",
                ModuleCategory.MISCELLANEOUS);
    }

    @Override
    public void onDisable()
    {
        mc.options.keyUse.setDown(false);
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        //
        FoodData hungerManager = mc.player.getFoodData();
        if (hungerManager.getFoodLevel() <= hungerConfig.getValue())
        {
            int slot = getFoodSlot();
            if (slot == -1)
            {
                return;
            }
            if (slot == 45)
            {
                mc.player.startUsingItem(InteractionHand.OFF_HAND);
            }
            else
            {
                Managers.INVENTORY.setClientSlot(slot);
            }
            mc.options.keyUse.setDown(true);
        }
        else
        {
            mc.options.keyUse.setDown(false);
        }
    }

    public int getFoodSlot()
    {
        int foodLevel = -1;
        int slot = -1;
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem().components().has(DataComponents.FOOD))
            {
                if (stack.getItem() == Items.PUFFERFISH
                        || stack.getItem() == Items.CHORUS_FRUIT)
                {
                    continue;
                }
                int hunger = stack.getItem().components().get(DataComponents.FOOD).nutrition();
                if (hunger > foodLevel)
                {
                    slot = i;
                    foodLevel = hunger;
                }
            }
        }
        ItemStack offhand = mc.player.getOffhandItem();
        if (offhand.getItem().components().has(DataComponents.FOOD))
        {
            if (offhand.getItem() == Items.PUFFERFISH
                    || offhand.getItem() == Items.CHORUS_FRUIT)
            {
                return slot;
            }
            int hunger = offhand.getItem().components().get(DataComponents.FOOD).nutrition();
            if (hunger > foodLevel)
            {
                slot = 45;
            }
        }
        return slot;
    }
}

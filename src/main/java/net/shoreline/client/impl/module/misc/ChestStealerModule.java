package net.shoreline.client.impl.module.misc;

import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.ItemListConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

import java.util.List;

public class ChestStealerModule extends ToggleModule
{
    Config<List<Item>> itemsConfig = register(new ItemListConfig<>("Items", "Items to steal", Items.SHULKER_BOX));
    Config<Float> delayConfig = register(new NumberConfig<>("Delay", "The item steal delay", 0.0f, 0.15f, 2.0f));
    private final Timer stealTimer = new CacheTimer();

    public ChestStealerModule()
    {
        super("ChestStealer", "Steals items from chests", ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() != StageEvent.EventStage.PRE)
        {
            return;
        }
        if (mc.player.containerMenu instanceof ChestMenu containerScreen)
        {
            for (int i = 0; i < containerScreen.getContainer().getContainerSize(); i++)
            {
                Slot slot = containerScreen.getSlot(i);
                if (slot.hasItem() && isValidItem(slot.getItem().getItem()) &&
                        stealTimer.passed(delayConfig.getValue() * 1000))
                {
                    stealItem(containerScreen.containerId, slot);
                    stealTimer.reset();
                }
            }
        }
    }

    private void stealItem(int syncId, Slot slot)
    {
        mc.gameMode.handleInventoryMouseClick(syncId, slot.index, 0, ClickType.QUICK_MOVE, mc.player);
    }

    private boolean isValidItem(Item item)
    {
        return ((ItemListConfig<?>) itemsConfig).contains(item);
    }
}

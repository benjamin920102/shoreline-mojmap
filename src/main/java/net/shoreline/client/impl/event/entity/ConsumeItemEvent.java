package net.shoreline.client.impl.event.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shoreline.eventbus.event.Event;

/**
 * @author linus
 * @since 1.0
 */
public class ConsumeItemEvent extends Event
{
    //
    private final ItemStack activeItemStack;

    public ConsumeItemEvent(ItemStack activeItemStack)
    {
        this.activeItemStack = activeItemStack;
    }

    public ItemStack getStack()
    {
        return activeItemStack;
    }

    public Item getItem()
    {
        return activeItemStack.getItem();
    }
}

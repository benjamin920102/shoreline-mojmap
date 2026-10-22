package net.shoreline.client.impl.event.render;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

/**
 * @author linus
 * @since 1.0
 */
@Cancelable
public class RenderFloatingItemEvent extends Event
{
    private final ItemStack floatingItem;

    public RenderFloatingItemEvent(ItemStack floatingItem)
    {
        this.floatingItem = floatingItem;
    }

    public Item getFloatingItem()
    {
        return floatingItem.getItem();
    }

    public ItemStack getFloatingItemStack()
    {
        return floatingItem;
    }
}

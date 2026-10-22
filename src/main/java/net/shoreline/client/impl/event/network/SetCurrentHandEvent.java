package net.shoreline.client.impl.event.network;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.shoreline.client.mixin.network.MixinClientPlayerEntity;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.event.Event;

/**
 * @author linus
 * @see MixinClientPlayerEntity
 * @since 1.0
 */
public class SetCurrentHandEvent extends Event implements Globals
{
    //
    private final InteractionHand hand;

    public SetCurrentHandEvent(InteractionHand hand)
    {
        this.hand = hand;
    }

    public InteractionHand getHand()
    {
        return hand;
    }

    public ItemStack getStackInHand()
    {
        return mc.player.getItemInHand(hand);
    }
}

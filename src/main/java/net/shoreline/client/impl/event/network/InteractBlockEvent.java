package net.shoreline.client.impl.event.network;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class InteractBlockEvent extends Event
{
    //
    private final LocalPlayer player;
    private final InteractionHand hand;
    private final BlockHitResult hitResult;

    /**
     * @param player
     * @param hand
     * @param hitResult
     */
    public InteractBlockEvent(LocalPlayer player, InteractionHand hand,
                              BlockHitResult hitResult)
    {
        this.player = player;
        this.hand = hand;
        this.hitResult = hitResult;
    }

    public LocalPlayer getPlayer()
    {
        return player;
    }

    public InteractionHand getHand()
    {
        return hand;
    }

    public BlockHitResult getHitResult()
    {
        return hitResult;
    }
}

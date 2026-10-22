package net.shoreline.client.impl.manager.player;

import net.shoreline.client.impl.event.network.PacketSneakingEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

import static net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY;
import static net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public class MovementManager implements Globals
{

    private boolean packetSneaking;

    public MovementManager()
    {
        EventBus.INSTANCE.subscribe(this);
    }

    /**
     * @param y
     */
    public void setMotionY(double y)
    {
        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), y, mc.player.getDeltaMovement().z());
    }

    /**
     * @param x
     * @param z
     */
    public void setMotionXZ(double x, double z)
    {
        mc.player.setDeltaMovement(x, mc.player.getDeltaMovement().y, z);
    }

    public void setMotionX(double x)
    {
        mc.player.setDeltaMovement(x, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z);
    }

    public void setMotionZ(double z)
    {
        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().y, z);
    }


    public void setPacketSneaking(final boolean packetSneaking)
    {
        this.packetSneaking = packetSneaking;
        if (packetSneaking)
        {
            Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, PRESS_SHIFT_KEY));
        }
        else
        {
            Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, RELEASE_SHIFT_KEY));
        }
    }

    @EventListener
    public void onPacketSneak(PacketSneakingEvent event)
    {
        event.setCanceled(packetSneaking);
    }
}

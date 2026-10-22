package net.shoreline.client.impl.event.network;

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.shoreline.eventbus.event.Event;

public class InventoryEvent extends Event
{
    private final ClientboundContainerSetContentPacket packet;

    public InventoryEvent(ClientboundContainerSetContentPacket packet)
    {
        this.packet = packet;
    }

    public ClientboundContainerSetContentPacket getPacket()
    {
        return packet;
    }
}

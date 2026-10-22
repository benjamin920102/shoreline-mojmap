package net.shoreline.client.security;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class AntiPacketLogger
{
    public void convertMinecraftProtocol(Packet<?> packet)
    {
        FriendlyByteBuf packetByteBuf = new FriendlyByteBuf(Unpooled.buffer());

    }
}

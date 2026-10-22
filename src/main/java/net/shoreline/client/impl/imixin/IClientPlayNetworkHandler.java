package net.shoreline.client.impl.imixin;

import net.minecraft.network.protocol.Packet;

@IMixin
public interface IClientPlayNetworkHandler {
   void sendQuietPacket(Packet<?> var1);
}

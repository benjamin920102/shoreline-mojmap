package net.shoreline.client.mixin.accessor;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Connection.class})
public interface AccessorClientConnection {
   @Invoker("doSendPacket")
   void hookSendInternal(Packet<?> var1, @Nullable PacketSendListener var2, boolean var3);
}

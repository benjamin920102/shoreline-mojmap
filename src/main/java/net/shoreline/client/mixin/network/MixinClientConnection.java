package net.shoreline.client.mixin.network;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.shoreline.client.impl.event.network.DecodePacketEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Connection.class})
public class MixinClientConnection {
   @Shadow
   @Nullable
   private volatile PacketListener packetListener;
   @Shadow
   @Final
   private static Logger LOGGER;

   @Inject(
      method = {"exceptionCaught(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Throwable;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookExceptionCaught(ChannelHandlerContext context, Throwable ex, CallbackInfo ci) {
      DecodePacketEvent decodePacketEvent = new DecodePacketEvent();
      EventBus.INSTANCE.dispatch(decodePacketEvent);
      if (decodePacketEvent.isCanceled()) {
         LOGGER.error("Exception caught on network thread:", ex);
         ci.cancel();
      }
   }

   @Inject(
      method = {"sendPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSendImmediately(Packet<?> packet, @Nullable PacketSendListener callbacks, boolean flush, CallbackInfo ci) {
      PacketEvent.Outbound packetOutboundEvent = new PacketEvent.Outbound(packet);
      EventBus.INSTANCE.dispatch(packetOutboundEvent);
      if (packetOutboundEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"sendPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V"},
      at = {@At("TAIL")},
      cancellable = true
   )
   private void hookSendImmediately$2(Packet<?> packet, @Nullable PacketSendListener callbacks, boolean flush, CallbackInfo ci) {
      PacketEvent.OutboundPost packetOutboundEvent = new PacketEvent.OutboundPost(packet);
      EventBus.INSTANCE.dispatch(packetOutboundEvent);
      if (packetOutboundEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookChannelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
      PacketListener ownedPacketListener = this.packetListener;
      if (packet != null && ownedPacketListener != null && ownedPacketListener.shouldHandleMessage(packet)) {
         PacketEvent.Inbound packetInboundEvent = new PacketEvent.Inbound(this.packetListener, packet);
         EventBus.INSTANCE.dispatch(packetInboundEvent);
         if (packetInboundEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"disconnect(Lnet/minecraft/network/chat/Component;)V"},
      at = {@At("HEAD")}
   )
   private void hookDisconnect(Component disconnectReason, CallbackInfo ci) {
      DisconnectEvent disconnectEvent = new DisconnectEvent();
      EventBus.INSTANCE.dispatch(disconnectEvent);
   }
}

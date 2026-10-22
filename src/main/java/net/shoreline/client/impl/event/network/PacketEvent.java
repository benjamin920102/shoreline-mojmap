package net.shoreline.client.impl.event.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

public class PacketEvent extends Event {
   private final Packet<?> packet;

   public PacketEvent(Packet<?> packet) {
      this.packet = packet;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   @Cancelable
   public static class Inbound extends PacketEvent {
      private final PacketListener packetListener;

      public Inbound(PacketListener packetListener, Packet<?> packet) {
         super(packet);
         this.packetListener = packetListener;
      }

      public PacketListener getPacketListener() {
         return this.packetListener;
      }
   }

   @Cancelable
   public static class Outbound extends PacketEvent {
      private final boolean cached;

      public Outbound(Packet<?> packet) {
         super(packet);
         this.cached = Managers.NETWORK.isCached(packet);
      }

      public boolean isClientPacket() {
         return this.cached;
      }
   }

   @Cancelable
   public static class OutboundPost extends PacketEvent.Outbound {
      public OutboundPost(Packet<?> packet) {
         super(packet);
      }
   }
}

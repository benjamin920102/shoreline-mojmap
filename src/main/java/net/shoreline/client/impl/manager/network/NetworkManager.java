package net.shoreline.client.impl.manager.network;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.shoreline.client.impl.event.gui.screen.ConnectScreenEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.imixin.IClientPlayNetworkHandler;
import net.shoreline.client.mixin.accessor.AccessorClientWorld;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.math.PerSecondCounter;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class NetworkManager implements Globals {
   private static final Set<Packet<?>> PACKET_CACHE = new HashSet<>();
   private ServerAddress address;
   private ServerData info;
   private final PerSecondCounter outgoingCounter = new PerSecondCounter();
   private final PerSecondCounter incomingCounter = new PerSecondCounter();

   public NetworkManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onConnect(ConnectScreenEvent event) {
      this.address = event.getAddress();
      this.info = event.getInfo();
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      PACKET_CACHE.clear();
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      this.outgoingCounter.updateCounter();
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Inbound event) {
      this.incomingCounter.updateCounter();
   }

   public void connect(ServerAddress address, ServerData info) {
      if (mc.getConnection() != null) {
         mc.getConnection()
            .getConnection()
            .initiateServerboundPlayConnection(
               address.getHost(),
               address.getPort(),
               new ClientHandshakePacketListenerImpl(mc.getConnection().getConnection(), mc, info, null, false, null, null, null)
            );
      }
   }

   public void sendPacket(Packet<?> p) {
      if (mc.getConnection() != null) {
         PACKET_CACHE.add(p);
         mc.getConnection().send(p);
      }
   }

   public void sendQuietPacket(Packet<?> p) {
      if (mc.getConnection() != null) {
         PACKET_CACHE.add(p);
         ((IClientPlayNetworkHandler)mc.getConnection()).sendQuietPacket(p);
      }
   }

   public void sendSequencedPacket(PredictiveAction p) {
      if (mc.level != null) {
         try (BlockStatePredictionHandler updater = ((AccessorClientWorld)mc.level).hookGetPendingUpdateManager().startPredicting()) {
            int i = updater.currentSequence();
            Packet<ServerGamePacketListener> packet = p.predict(i);
            this.sendPacket(packet);
         }
      }
   }

   public int getClientLatency() {
      if (mc.getConnection() != null) {
         PlayerInfo playerEntry = mc.getConnection().getPlayerInfo(mc.player.getGameProfile().getId());
         if (playerEntry != null) {
            return playerEntry.getLatency();
         }
      }

      return 0;
   }

   public ServerAddress getAddress() {
      return this.address;
   }

   public void setAddress(ServerAddress address) {
      this.address = address;
   }

   public ServerData getInfo() {
      return this.info;
   }

   public void setInfo(ServerData info) {
      this.info = info;
   }

   public boolean isCrystalPvpCC() {
      return this.getServerIp().contains("crystalpvp.cc");
   }

   public boolean is2b2t() {
      return this.getServerIp().contains("2b2t.org");
   }

   public int getOutgoingPPS() {
      return this.outgoingCounter.getPerSecond();
   }

   public int getIncomingPPS() {
      return this.incomingCounter.getPerSecond();
   }

   public String getServerIp() {
      return this.info != null ? this.info.ip : "Singleplayer";
   }

   public boolean isCached(Packet<?> p) {
      return PACKET_CACHE.contains(p);
   }
}

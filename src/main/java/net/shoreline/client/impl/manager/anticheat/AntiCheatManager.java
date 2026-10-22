package net.shoreline.client.impl.manager.anticheat;

import java.util.Arrays;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.Shoreline;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public final class AntiCheatManager implements Globals {
   private SetbackData lastSetback;
   private final int[] transactions = new int[4];
   private int index;
   private boolean isGrim;

   public AntiCheatManager() {
      EventBus.INSTANCE.subscribe(this);
      Arrays.fill(this.transactions, -1);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof ClientboundPingPacket packet) {
         if (this.index > 3) {
            return;
         }

         int uid = packet.getId();
         this.transactions[this.index] = uid;
         this.index++;
         if (this.index == 4) {
            this.grimCheck();
         }
      } else if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
         this.lastSetback = new SetbackData(new Vec3(packet.getX(), packet.getY(), packet.getZ()), System.currentTimeMillis(), packet.getId());
      }
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      Arrays.fill(this.transactions, -1);
      this.index = 0;
      this.isGrim = false;
   }

   private void grimCheck() {
      int i = 0;

      while (i < 4 && this.transactions[i] == -i) {
         i++;
      }

      this.isGrim = true;
      Shoreline.info("Server is running GrimAC.");
   }

   public boolean isGrim() {
      return this.isGrim;
   }

   public boolean hasPassed(long timeMS) {
      return this.lastSetback != null && this.lastSetback.timeSince() >= timeMS;
   }
}

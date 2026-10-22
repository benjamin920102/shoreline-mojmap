package net.shoreline.client.impl.manager.player;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.shoreline.client.impl.event.network.PacketSneakingEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class MovementManager implements Globals {
   private boolean packetSneaking;

   public MovementManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   public void setMotionY(double y) {
      mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), y, mc.player.getDeltaMovement().z());
   }

   public void setMotionXZ(double x, double z) {
      mc.player.setDeltaMovement(x, mc.player.getDeltaMovement().y, z);
   }

   public void setMotionX(double x) {
      mc.player.setDeltaMovement(x, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z);
   }

   public void setMotionZ(double z) {
      mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().y, z);
   }

   public void setPacketSneaking(boolean packetSneaking) {
      this.packetSneaking = packetSneaking;
      if (packetSneaking) {
         Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Action.PRESS_SHIFT_KEY));
      } else {
         Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Action.RELEASE_SHIFT_KEY));
      }
   }

   @EventListener
   public void onPacketSneak(PacketSneakingEvent event) {
      event.setCanceled(this.packetSneaking);
   }
}

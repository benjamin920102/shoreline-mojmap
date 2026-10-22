package net.shoreline.client.impl.manager.combat;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.module.exploit.PhaseModule;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.player.RayCastUtil;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class PearlManager implements Globals {
   private float[] lastThrownAngles;
   private AABB pearlBB;

   public PearlManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && PhaseModule.getInstance().shouldRaytrace()) {
         if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet && this.lastThrownAngles != null) {
            BlockHitResult hitResult = (BlockHitResult)RayCastUtil.rayCast(3.0, this.lastThrownAngles);
            this.pearlBB = new AABB(hitResult.getLocation().subtract(0.4, 0.4, 0.4), hitResult.getLocation().add(0.4, 0.4, 0.4));
            if (mc.level.getBlockState(hitResult.getBlockPos()).isAir()) {
               return;
            }

            if (!this.pearlBB.contains(packet.getX(), packet.getY(), packet.getZ())) {
               event.cancel();
               mc.getConnection().getConnection().send(new ServerboundAcceptTeleportationPacket(packet.getId()));
               mc.getConnection()
                  .getConnection()
                  .send(new PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), false));
            }

            this.lastThrownAngles = null;
         }
      }
   }

   public void setLastThrownAngles(float[] lastThrownAngles) {
      this.lastThrownAngles = lastThrownAngles;
   }
}

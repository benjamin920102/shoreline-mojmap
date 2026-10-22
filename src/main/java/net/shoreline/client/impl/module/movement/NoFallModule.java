package net.shoreline.client.impl.module.movement;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.impl.module.exploit.PacketFlyModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class NoFallModule extends ToggleModule {
   Config<NoFallModule.NoFallMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode to prevent fall damage", NoFallModule.NoFallMode.ANTI, NoFallModule.NoFallMode.values())
   );
   private double groundedY;

   public NoFallModule() {
      super("NoFall", "Prevents all fall damage", ModuleCategory.MOVEMENT);
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && this.checkFalling()) {
         if (this.modeConfig.getValue() == NoFallModule.NoFallMode.LIMIT) {
            if (mc.level.dimension() == Level.NETHER) {
               Managers.NETWORK.sendPacket(new Pos(mc.player.getX(), 0.0, mc.player.getZ(), true));
            } else {
               Managers.NETWORK.sendPacket(new Pos(0.0, 64.0, 0.0, true));
            }

            mc.player.fallDistance = 0.0F;
         } else if (this.modeConfig.getValue() == NoFallModule.NoFallMode.GRIM) {
            Managers.NETWORK
               .sendPacket(new PosRot(mc.player.getX(), mc.player.getY() + 1.0E-9, mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), true));
            mc.player.resetFallDistance();
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && this.checkFalling()) {
         if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
            if (this.modeConfig.getValue() == NoFallModule.NoFallMode.PACKET) {
               ((AccessorPlayerMoveC2SPacket)packet).hookSetOnGround(true);
            } else if (this.modeConfig.getValue() == NoFallModule.NoFallMode.ANTI) {
               double y = packet.getY(mc.player.getY());
               ((AccessorPlayerMoveC2SPacket)packet).hookSetY(y + 0.1F);
            } else if (this.modeConfig.getValue() == NoFallModule.NoFallMode.STRICT && this.isFallingLagback() && mc.player.fallDistance >= 3.0F) {
               Managers.MOVEMENT.setMotionY(0.0);
               ((AccessorPlayerMoveC2SPacket)packet).hookSetY(this.groundedY);
               mc.player.fallDistance = 0.0F;
            }
         }
      }
   }

   public double getGroundY() {
      for (int i = (int)Math.round(mc.player.getY()); i > 0; i--) {
         AABB bb = mc.player.getBoundingBox();
         AABB box = new AABB(bb.minX, i - 1.0, bb.minZ, bb.maxX, i, bb.maxZ);
         if (mc.level.noCollision(box) && !(box.minY > mc.player.getY())) {
            return i;
         }
      }

      return 0.0;
   }

   public boolean isFallingLagback() {
      this.groundedY = this.getGroundY() - 0.1;
      return mc.player.getY() - this.groundedY < 3.0;
   }

   private boolean checkFalling() {
      return mc.player.fallDistance > mc.player.getMaxFallDistance()
         && !mc.player.onGround()
         && !mc.player.isFallFlying()
         && !FlightModule.getInstance().isEnabled()
         && !PacketFlyModule.getInstance().isEnabled();
   }

   public static enum NoFallMode {
      ANTI,
      LIMIT,
      PACKET,
      STRICT,
      GRIM;
   }
}

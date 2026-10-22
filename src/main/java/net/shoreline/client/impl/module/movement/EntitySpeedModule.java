package net.shoreline.client.impl.module.movement;

import java.text.DecimalFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class EntitySpeedModule extends ToggleModule {
   Config<Float> speedConfig = this.register(new NumberConfig<>("Speed", "The speed of the entity while moving", 0.1F, 0.5F, 4.0F));
   Config<Boolean> antiStuckConfig = this.register(new BooleanConfig("AntiStuck", "Prevents entities from getting stuck when moving up", false));
   Config<Boolean> strictConfig = this.register(new BooleanConfig("Strict", "The NCP-Updated bypass for speeding up entity movement", false));
   private final Timer entityJumpTimer = new CacheTimer();

   public EntitySpeedModule() {
      super("EntitySpeed", "Increases riding entity speeds", ModuleCategory.MOVEMENT);
   }

   @Override
   public String getModuleData() {
      DecimalFormat decimal = new DecimalFormat("0.0");
      return decimal.format(this.speedConfig.getValue());
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (mc.player.isHandsBusy() && mc.player.getControlledVehicle() != null) {
            double d = Math.cos(Math.toRadians(mc.player.getYRot() + 90.0F));
            double d2 = Math.sin(Math.toRadians(mc.player.getYRot() + 90.0F));
            BlockPos pos1 = BlockPos.containing(mc.player.getX() + 2.0 * d, mc.player.getY() - 1.0, mc.player.getZ() + 2.0 * d2);
            BlockPos pos2 = BlockPos.containing(mc.player.getX() + 2.0 * d, mc.player.getY() - 2.0, mc.player.getZ() + 2.0 * d2);
            if (this.antiStuckConfig.getValue()
               && !mc.player.getControlledVehicle().onGround()
               && !mc.level.getBlockState(pos1).blocksMotion()
               && !mc.level.getBlockState(pos2).blocksMotion()) {
               this.entityJumpTimer.reset();
               return;
            }

            BlockPos pos3 = BlockPos.containing(mc.player.getX() + 2.0 * d, mc.player.getY(), mc.player.getZ() + 2.0 * d2);
            if (this.antiStuckConfig.getValue() && mc.level.getBlockState(pos3).blocksMotion()) {
               this.entityJumpTimer.reset();
               return;
            }

            BlockPos pos4 = BlockPos.containing(mc.player.getX() + d, mc.player.getY() + 1.0, mc.player.getZ() + d2);
            if (this.antiStuckConfig.getValue() && mc.level.getBlockState(pos4).blocksMotion()) {
               this.entityJumpTimer.reset();
               return;
            }

            if (mc.player.input.jumping) {
               this.entityJumpTimer.reset();
            }

            if (this.entityJumpTimer.passed(10000) || !this.antiStuckConfig.getValue()) {
               if (!mc.player.getControlledVehicle().isInWater() || mc.player.input.jumping || !this.entityJumpTimer.passed(1000)) {
                  if (mc.player.getControlledVehicle().onGround()) {
                     mc.player.getControlledVehicle().setDeltaMovement(mc.player.getDeltaMovement().x, 0.4, mc.player.getDeltaMovement().z);
                  }

                  mc.player.getControlledVehicle().setDeltaMovement(mc.player.getDeltaMovement().x, -0.4, mc.player.getDeltaMovement().z);
               }

               if (this.strictConfig.getValue()) {
                  Managers.NETWORK
                     .sendPacket(ServerboundInteractPacket.createInteractionPacket(mc.player.getControlledVehicle(), false, InteractionHand.MAIN_HAND));
               }

               this.handleEntityMotion(this.speedConfig.getValue(), d, d2);
               this.entityJumpTimer.reset();
            }
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.player.isHandsBusy() && !mc.options.keyShift.isDown() && mc.player.getControlledVehicle() != null) {
         if (this.strictConfig.getValue()) {
            if (event.getPacket() instanceof ClientboundSetPassengersPacket) {
               event.cancel();
            } else if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
               event.cancel();
            }
         }
      }
   }

   private void handleEntityMotion(float entitySpeed, double d, double d2) {
      Vec3 motion = mc.player.getControlledVehicle().getDeltaMovement();
      float forward = mc.player.input.forwardImpulse;
      float strafe = mc.player.input.leftImpulse;
      if (forward == 0.0F && strafe == 0.0F) {
         mc.player.getControlledVehicle().setDeltaMovement(0.0, motion.y, 0.0);
      } else {
         mc.player
            .getControlledVehicle()
            .setDeltaMovement(forward * entitySpeed * d + strafe * entitySpeed * d2, motion.y, forward * entitySpeed * d2 - strafe * entitySpeed * d);
      }
   }
}

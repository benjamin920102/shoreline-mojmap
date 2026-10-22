package net.shoreline.client.impl.module.movement;

import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.EntityTravelEvent;
import net.shoreline.client.impl.event.entity.player.TravelEvent;
import net.shoreline.client.impl.event.network.DismountVehicleEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.module.exploit.DisablerModule;
import net.shoreline.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class BoatFlyModule extends ToggleModule {
   Config<Float> speedConfig = this.register(new NumberConfig<>("Speed", "The horizontal flight speed", 0.1F, 2.5F, 10.0F));
   Config<Float> vspeedConfig = this.register(new NumberConfig<>("VerticalSpeed", "The vertical flight speed", 0.1F, 1.0F, 5.0F));
   Config<Boolean> remountConfig = this.register(new BooleanConfig("Remount", "Automatically remounts", false));
   Config<BoatFlyModule.AntiKick> antiKickConfig = this.register(
      new EnumConfig<>("AntiKick", "Prevents vanilla flight detection", BoatFlyModule.AntiKick.NORMAL, BoatFlyModule.AntiKick.values())
   );
   Config<Boolean> accelerateConfig = this.register(new BooleanConfig("Accelerate", "Accelerate as you fly", false));
   Config<Float> accelerateSpeedConfig = this.register(
      new NumberConfig<>("AccelerateSpeed", "Speed to accelerate as", 0.01F, 0.2F, 1.0F, () -> this.accelerateConfig.getValue())
   );
   Config<Float> maxSpeedConfig = this.register(
      new NumberConfig<>("MaxSpeed", "Max speed to accelerate to", 1.0F, 5.0F, 10.0F, () -> this.accelerateConfig.getValue())
   );
   private double speed;
   private double lastY;
   private boolean floating;
   private int floatingTicks;
   private boolean modifyY;

   public BoatFlyModule() {
      super("BoatFly", "Allows you fly with entities", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onEnable() {
      this.speed = 0.0;
   }

   @Override
   public void onDisable() {
      this.modifyY = false;
   }

   @EventListener
   public void onTravel(EntityTravelEvent event) {
      if (mc.player.getVehicle() != null && event.getEntity().getId() == mc.player.getVehicle().getId()) {
         event.cancel();
      }
   }

   @EventListener
   public void onDismountVehicle(DismountVehicleEvent event) {
      if (mc.options.keyShift.isDown()) {
         event.cancel();
      }
   }

   @EventListener
   public void onPlayerTick(TravelEvent event) {
      if (mc.player.getVehicle() != null && !DisablerModule.getInstance().grimFireworkCheck()) {
         Entity ridingEntity = mc.player.getVehicle();
         if (this.accelerateConfig.getValue()) {
            if (!MovementUtil.isInputtingMovement() || ridingEntity.horizontalCollision) {
               this.speed = 0.0;
            }

            this.speed = this.speed + this.accelerateSpeedConfig.getValue().floatValue();
            if (this.speed > this.maxSpeedConfig.getValue().floatValue()) {
               this.speed = this.maxSpeedConfig.getValue().floatValue();
            }
         } else {
            this.speed = this.speedConfig.getValue().floatValue();
         }

         ridingEntity.setYRot(mc.player.getYRot());
         if (ridingEntity instanceof Llama llama) {
            llama.yHeadRot = mc.player.getYRot();
         }

         boolean stopVerticalMovement = false;
         if (this.floating) {
            this.floatingTicks++;
            if (this.floatingTicks >= 20) {
               if (this.antiKickConfig.getValue() == BoatFlyModule.AntiKick.PACKET) {
                  this.modifyY = true;
               } else if (this.antiKickConfig.getValue() == BoatFlyModule.AntiKick.NORMAL) {
                  ridingEntity.setPos(ridingEntity.getX(), ridingEntity.getY() - 0.0313, ridingEntity.getZ());
               }

               this.floatingTicks = 0;
               this.floating = false;
               stopVerticalMovement = true;
            }
         }

         ridingEntity.setDeltaMovement(ridingEntity.getDeltaMovement().x, 0.0, ridingEntity.getDeltaMovement().z);
         if (mc.options.keyJump.isDown() && !stopVerticalMovement) {
            ridingEntity.setDeltaMovement(ridingEntity.getDeltaMovement().x, this.vspeedConfig.getValue().floatValue(), ridingEntity.getDeltaMovement().z);
         } else if (mc.options.keyShift.isDown()) {
            ridingEntity.setDeltaMovement(ridingEntity.getDeltaMovement().x, -this.vspeedConfig.getValue(), ridingEntity.getDeltaMovement().z);
         }

         this.speed = Math.max(this.speed, 0.2873F);
         float forward = mc.player.input.forwardImpulse;
         float strafe = mc.player.input.leftImpulse;
         float yaw = mc.player.getYRot();
         if (forward != 0.0F || strafe != 0.0F) {
            double rx = Math.cos(Math.toRadians(yaw + 90.0F));
            double rz = Math.sin(Math.toRadians(yaw + 90.0F));
            double var10001 = forward * this.speed * rx + strafe * this.speed * rz;
            double var10003 = forward * this.speed * rz;
            double var10004 = strafe * this.speed;
            ridingEntity.setDeltaMovement(var10001, ridingEntity.getDeltaMovement().y, var10003 - var10004 * rx);
            event.cancel();
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.player.getVehicle() != null) {
         Entity ridingEntity = mc.player.getVehicle();
         if (event.getPacket() instanceof ServerboundMoveVehiclePacket packet && this.antiKickConfig.getValue() != BoatFlyModule.AntiKick.OFF) {
            double packetY = packet.getY();
            if (this.modifyY) {
               ((AccessorPlayerMoveC2SPacket)packet).hookSetY(this.lastY - 0.04);
               this.modifyY = false;
            } else {
               this.floating = this.floatingCheck(packet, ridingEntity);
               this.lastY = packetY;
            }
         }

         if (event.getPacket() instanceof ServerboundPlayerCommandPacket packetx
            && packetx.getAction() == Action.PRESS_SHIFT_KEY
            && mc.player.getVehicle() != null
            && !mc.player.getVehicle().onGround()) {
            event.cancel();
         }
      }
   }

   private boolean floatingCheck(ServerboundMoveVehiclePacket packet, Entity ridingEntity) {
      double e = Mth.clamp(packet.getY(), -2.0E7, 2.0E7);
      double s = e - this.lastY;
      return s >= -0.03125 && !ridingEntity.verticalCollisionBelow && this.isEntityOnAir(ridingEntity);
   }

   private boolean isEntityOnAir(Entity entity) {
      return entity.level().getBlockStates(entity.getBoundingBox().inflate(0.0625).expandTowards(0.0, -0.55, 0.0)).allMatch(BlockStateBase::isAir);
   }

   public static enum AntiKick {
      NORMAL,
      PACKET,
      OFF;
   }
}

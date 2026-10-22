package net.shoreline.client.impl.module.movement;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.ShorelineMod;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.MouseUpdateEvent;
import net.shoreline.client.impl.event.camera.CameraRotationEvent;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.entity.player.PlayerMoveEvent;
import net.shoreline.client.impl.event.entity.player.TravelEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorFireworkRocketEntity;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class ElytraFlyModule extends RotationModule {
   private static ElytraFlyModule INSTANCE;
   Config<ElytraFlyModule.FlyMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for elytra flight", ElytraFlyModule.FlyMode.CONTROL, ElytraFlyModule.FlyMode.values())
   );
   Config<Float> speedConfig = this.register(
      new NumberConfig<>(
         "Speed",
         "The horizontal flight speed",
         0.1F,
         2.5F,
         10.0F,
         () -> this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOOST && this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOUNCE
      )
   );
   Config<Float> vspeedConfig = this.register(
      new NumberConfig<>(
         "VerticalSpeed",
         "The vertical flight speed",
         0.1F,
         1.0F,
         5.0F,
         () -> this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOOST && this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOUNCE
      )
   );
   Config<Boolean> accelerateConfig = this.register(
      new BooleanConfig(
         "Accelerate",
         "Accelerates fly speed",
         false,
         () -> this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOOST && this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOUNCE
      )
   );
   Config<Float> accelSpeedConfig = this.register(
      new NumberConfig<>(
         "AccelSpeed",
         "Acceleration speed",
         0.01F,
         0.1F,
         1.0F,
         () -> this.accelerateConfig.getValue()
            && this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOOST
            && this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOUNCE
      )
   );
   Config<Float> maxSpeedConfig = this.register(
      new NumberConfig<>(
         "MaxSpeed",
         "The maximum flight speed",
         0.1F,
         3.5F,
         10.0F,
         () -> this.accelerateConfig.getValue()
            && this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOOST
            && this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOUNCE
      )
   );
   Config<Boolean> boostConfig = this.register(
      new BooleanConfig("VanillaBoost", "Uses vanilla boost speed", false, () -> this.modeConfig.getValue() != ElytraFlyModule.FlyMode.BOOST)
   );
   Config<Boolean> rotateConfig = this.register(
      new BooleanConfig("Rotate", "Rotates the player when moving", false, () -> this.modeConfig.getValue() == ElytraFlyModule.FlyMode.CONTROL)
   );
   Config<Boolean> fireworkConfig = this.register(
      new BooleanConfig("Fireworks", "Uses fireworks when flying", false, () -> this.modeConfig.getValue() == ElytraFlyModule.FlyMode.CONTROL)
   );
   Config<Float> pitchConfig = this.register(
      new NumberConfig<>("Pitch", "The pitch angle of bounce", 70.0F, 80.0F, 90.0F, () -> this.modeConfig.getValue() == ElytraFlyModule.FlyMode.BOUNCE)
   );
   Config<Boolean> obstaclePasserConfig = new BooleanConfig(
      "ObstaclePasser", "Passes obstacles and resets fly using Baritone", this.modeConfig.getValue() == ElytraFlyModule.FlyMode.BOUNCE
   );
   private static final double GRIM_AIR_FRICTION = 0.0264444413;
   private boolean resetSpeed;
   private float speed;
   private float cameraPitch;

   public ElytraFlyModule() {
      super("ElytraFly", "Allows you to fly freely using an elytra", ModuleCategory.MOVEMENT);
      INSTANCE = this;
      if (ShorelineMod.isBaritonePresent()) {
         this.register(this.obstaclePasserConfig);
      }
   }

   public static ElytraFlyModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @Override
   public void onEnable() {
      this.resetSpeed = true;
   }

   @Override
   public void onDisable() {
      if (ShorelineMod.isBaritonePresent()) {
         BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().forceCancel();
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST && event.getConfig() == this.accelerateConfig && this.accelerateConfig.getValue()) {
         this.resetSpeed = true;
      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!ShorelineMod.isBaritonePresent() || !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
         if (mc.player.isFallFlying()) {
            if (ShorelineMod.isBaritonePresent() && this.obstaclePasserConfig.getValue()) {
               if (!BaritoneAPI.getSettings().freeLook.value) {
                  ChatUtil.clientSendMessage(ChatFormatting.RED + "Please enable FreeLook in Baritone to use ObstaclePasser!", 5005);
                  return;
               }

               if (mc.player.horizontalCollision) {
                  BlockPos goalBlockPos = null;

                  for (int i = 3; i < 64; i++) {
                     Vec3 position = mc.player.position();
                     Vec3 vec3d2 = RotationUtil.getRotationVector(0.0F, mc.player.getYRot());
                     Vec3 vec3d3 = position.add(vec3d2.x * i, 0.0, vec3d2.z * i);
                     BlockPos blockPos = BlockPos.containing(vec3d3);
                     if (mc.level.getBlockState(blockPos).isAir() && mc.level.getBlockState(blockPos.above()).isAir()) {
                        goalBlockPos = blockPos;
                        break;
                     }
                  }

                  if (goalBlockPos != null) {
                     BaritoneAPI.getProvider()
                        .getPrimaryBaritone()
                        .getCustomGoalProcess()
                        .setGoalAndPath(new GoalBlock(goalBlockPos.getX(), goalBlockPos.getY(), goalBlockPos.getZ()));
                  }
               }
            }

            if (this.modeConfig.getValue() == ElytraFlyModule.FlyMode.CONTROL && this.rotateConfig.getValue()) {
               float yaw = SprintModule.getInstance().getSprintYaw(mc.player.getYRot());
               this.setRotation(yaw, this.getControlPitch());
               int fireworkSlot = -1;

               for (int ix = 0; ix < 36; ix++) {
                  ItemStack stack = mc.player.getInventory().getItem(ix);
                  if (stack.getItem() instanceof FireworkRocketItem) {
                     fireworkSlot = ix;
                     break;
                  }
               }

               if (this.fireworkConfig.getValue() && fireworkSlot != -1) {
                  if (fireworkSlot < 9) {
                     Managers.INVENTORY.setSlot(fireworkSlot);
                     mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                     Managers.INVENTORY.syncToClient();
                  } else {
                     mc.gameMode.handleInventoryMouseClick(0, fireworkSlot, 0, ClickType.PICKUP, mc.player);
                     mc.gameMode.handleInventoryMouseClick(0, mc.player.getInventory().selected + 36, 0, ClickType.PICKUP, mc.player);
                     mc.gameMode.handleInventoryMouseClick(0, fireworkSlot, 0, ClickType.PICKUP, mc.player);
                     mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                     mc.gameMode.handleInventoryMouseClick(0, fireworkSlot, 0, ClickType.PICKUP, mc.player);
                     mc.gameMode.handleInventoryMouseClick(0, mc.player.getInventory().selected + 36, 0, ClickType.PICKUP, mc.player);
                     mc.gameMode.handleInventoryMouseClick(0, fireworkSlot, 0, ClickType.PICKUP, mc.player);
                  }
               }
            } else if (this.modeConfig.getValue() == ElytraFlyModule.FlyMode.BOUNCE) {
               if (this.canSprint()) {
                  mc.player.setSprinting(true);
               }

               mc.player.setXRot(this.pitchConfig.getValue());
               this.setRotation(mc.player.getYRot(), this.pitchConfig.getValue());
            }
         }
      }
   }

   @EventListener
   public void onPlayerMove(PlayerMoveEvent event) {
      if (!ShorelineMod.isBaritonePresent() || !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
         if (mc.player.isFallFlying() && !mc.player.isInWater() && !mc.player.isInLava() && !(mc.player.getFoodData().getFoodLevel() <= 6.0F)) {
            if (this.modeConfig.getValue() == ElytraFlyModule.FlyMode.BOOST) {
               boolean boost = mc.options.keyJump.isDown() || AutoWalkModule.getInstance().isEnabled();
               if (boost) {
                  this.applyVanillaBoost();
               }
            } else if (this.boostConfig.getValue()) {
               this.applyVanillaBoost();
            }
         }
      }
   }

   @EventListener
   public void onTravel(TravelEvent event) {
      if (mc.player != null && mc.level != null) {
         if (!ShorelineMod.isBaritonePresent() || !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            if (!MovementUtil.isInputtingMovement() || mc.player.horizontalCollision) {
               this.resetSpeed = true;
            }

            if (this.accelerateConfig.getValue()) {
               if (this.resetSpeed) {
                  this.speed = 0.1F;
                  this.resetSpeed = false;
               }

               if (this.speed < this.maxSpeedConfig.getValue()) {
                  this.speed = this.speed + this.accelSpeedConfig.getValue();
               }

               if (this.speed - this.accelSpeedConfig.getValue() > this.maxSpeedConfig.getValue()) {
                  this.speed = this.speed - this.accelSpeedConfig.getValue();
               }
            } else {
               this.speed = this.speedConfig.getValue();
            }

            switch ((ElytraFlyModule.FlyMode)this.modeConfig.getValue()) {
               case CONTROL:
                  if (!mc.player.isFallFlying()) {
                     return;
                  }

                  event.cancel();
                  float forward = mc.player.input.forwardImpulse;
                  float side = mc.player.input.leftImpulse;
                  float yaw = Managers.ROTATION.isRotating() ? Managers.ROTATION.getRotationYaw() : mc.player.getYRot();
                  if (forward != 0.0F) {
                     if (side > 0.0F) {
                        yaw += forward > 0.0F ? -45.0F : 45.0F;
                     } else if (side < 0.0F) {
                        yaw += forward > 0.0F ? 45.0F : -45.0F;
                     }

                     side = 0.0F;
                     if (forward > 0.0F) {
                        forward = 1.0F;
                     } else if (forward < 0.0F) {
                        forward = -1.0F;
                     }
                  }

                  double sin = Math.sin(Math.toRadians(yaw + 90.0F));
                  double cos = Math.cos(Math.toRadians(yaw + 90.0F));
                  double rx = forward * this.speed * cos + side * this.speed * sin;
                  double rz = forward * this.speed * sin - side * this.speed * cos;
                  if (forward == 0.0F && side == 0.0F) {
                     Managers.MOVEMENT.setMotionXZ(0.0, 0.0);
                  } else {
                     Managers.MOVEMENT.setMotionXZ(rx, rz);
                  }

                  Managers.MOVEMENT.setMotionY(0.0);
                  if (mc.options.keyJump.isDown()) {
                     Managers.MOVEMENT.setMotionY(this.vspeedConfig.getValue().floatValue());
                  } else if (mc.options.keyShift.isDown()) {
                     Managers.MOVEMENT.setMotionY(-this.vspeedConfig.getValue());
                  }
                  break;
               case BOUNCE:
                  if (!mc.player.getInventory().getArmor(2).getItem().equals(Items.ELYTRA)) {
                     return;
                  }

                  if (!mc.player.isFallFlying()) {
                     Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
                     mc.player.startFallFlying();
                  }

                  if (mc.player.onGround()) {
                     mc.player.jumpFromGround();
                  }
            }
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && (!ShorelineMod.isBaritonePresent() || !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())) {
         if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.resetSpeed = true;
         }
      }
   }

   @EventListener
   public void onCameraRotation(CameraRotationEvent event) {
      if (!ShorelineMod.isBaritonePresent() || !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
         if (this.modeConfig.getValue() == ElytraFlyModule.FlyMode.BOUNCE) {
            event.setYaw(event.getYaw());
            event.setPitch(this.cameraPitch);
         }
      }
   }

   @EventListener
   public void onMouseUpdate(MouseUpdateEvent event) {
      if (!ShorelineMod.isBaritonePresent() || !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
         if (this.modeConfig.getValue() == ElytraFlyModule.FlyMode.BOUNCE) {
            event.cancel();
            float f = (float)event.getCursorDeltaY() * 0.15F;
            float g = (float)event.getCursorDeltaX() * 0.15F;
            this.cameraPitch += f;
            mc.player.setYRot(mc.player.getYRot() + g);
            this.cameraPitch = Mth.clamp(this.cameraPitch, -90.0F, 90.0F);
         }
      }
   }

   public float getControlPitch() {
      if (this.isBoostedByRocket()) {
         if (mc.options.keyJump.isDown() && !mc.options.keyShift.isDown()) {
            return MovementUtil.isMoving() ? -50.0F : -90.0F;
         } else if (mc.options.keyShift.isDown() && !mc.options.keyJump.isDown()) {
            return MovementUtil.isMoving() ? 50.0F : 90.0F;
         } else {
            return 0.0F;
         }
      } else if (mc.options.keyJump.isDown() && !mc.options.keyShift.isDown()) {
         return -50.0F;
      } else {
         return mc.options.keyShift.isDown() && !mc.options.keyJump.isDown() ? 50.0F : 0.1F;
      }
   }

   public void applyVanillaBoost() {
      float yaw = Managers.ROTATION.isRotating() ? Managers.ROTATION.getRotationYaw() : mc.player.getYRot();
      double x = 0.0264444413 * Math.cos(Math.toRadians(yaw + 90.0F));
      double z = 0.0264444413 * Math.sin(Math.toRadians(yaw + 90.0F));
      double motionX = mc.player.getDeltaMovement().x + x;
      double motionZ = mc.player.getDeltaMovement().z + z;
      mc.player.setDeltaMovement(motionX, mc.player.getDeltaMovement().y, motionZ);
   }

   private boolean isBoostedByRocket() {
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof FireworkRocketEntity rocket
            && ((AccessorFireworkRocketEntity)rocket).hookWasShotByEntity()
            && ((AccessorFireworkRocketEntity)rocket).hookGetShooter() == mc.player) {
            return true;
         }
      }

      return false;
   }

   private boolean canSprint() {
      return !mc.player.isShiftKeyDown()
         && !mc.player.isHandsBusy()
         && !mc.player.isFallFlying()
         && !mc.player.isInWater()
         && !mc.player.isInLava()
         && !mc.player.isSuppressingSlidingDownLadder()
         && !mc.player.hasEffect(MobEffects.BLINDNESS)
         && mc.player.getFoodData().getFoodLevel() > 6.0F;
   }

   public boolean isBounce() {
      return this.modeConfig.getValue() == ElytraFlyModule.FlyMode.BOUNCE;
   }

   public static enum FlyMode {
      CONTROL,
      BOOST,
      BOUNCE;
   }
}

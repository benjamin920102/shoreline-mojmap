package net.shoreline.client.impl.module.movement;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.NumberDisplay;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.player.PushEntityEvent;
import net.shoreline.client.impl.event.entity.player.PushFluidsEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.network.PushOutOfBlocksEvent;
import net.shoreline.client.impl.module.combat.SurroundModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorBundlePacket;
import net.shoreline.client.mixin.accessor.AccessorClientWorld;
import net.shoreline.client.mixin.accessor.AccessorEntityVelocityUpdateS2CPacket;
import net.shoreline.client.mixin.accessor.AccessorExplosionS2CPacket;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;

public class VelocityModule extends ToggleModule {
   private static VelocityModule INSTANCE;
   Config<Boolean> knockbackConfig = this.register(new BooleanConfig("Knockback", "Removes player knockback velocity", true));
   Config<Boolean> explosionConfig = this.register(new BooleanConfig("Explosion", "Removes player explosion velocity", true));
   Config<VelocityModule.VelocityMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for velocity", VelocityModule.VelocityMode.NORMAL, VelocityModule.VelocityMode.values())
   );
   Config<Float> horizontalConfig = this.register(
      new NumberConfig<>(
         "Horizontal",
         "How much horizontal knock-back to take",
         0.0F,
         0.0F,
         100.0F,
         NumberDisplay.PERCENT,
         () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.NORMAL || this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS
      )
   );
   Config<Float> verticalConfig = this.register(
      new NumberConfig<>(
         "Vertical",
         "How much vertical knock-back to take",
         0.0F,
         0.0F,
         100.0F,
         NumberDisplay.PERCENT,
         () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.NORMAL || this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS
      )
   );
   Config<Boolean> concealConfig = this.register(new BooleanConfig("Conceal", "Fixes velocity on servers with excessive setbacks", false));
   Config<Boolean> wallsAirConfig = this.register(
      new BooleanConfig(
         "GroundOnly", "Only applies velocity in walls while on ground", false, () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS
      )
   );
   Config<Boolean> wallsTrappedConfig = this.register(
      new BooleanConfig(
         "Trapped", "Applies velocity while player head is trapped", false, () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS
      )
   );
   Config<Boolean> pushEntitiesConfig = this.register(new BooleanConfig("NoPush-Entities", "Prevents being pushed away from entities", true));
   Config<Boolean> pushBlocksConfig = this.register(new BooleanConfig("NoPush-Blocks", "Prevents being pushed out of blocks", true));
   Config<Boolean> pushLiquidsConfig = this.register(new BooleanConfig("NoPush-Liquids", "Prevents being pushed by flowing liquids", true));
   Config<Boolean> pushFishhookConfig = this.register(new BooleanConfig("NoPush-Fishhook", "Prevents being pulled by fishing rod hooks", true));
   private boolean cancelVelocity;
   private boolean concealVelocity;

   public VelocityModule() {
      super("Velocity", "Reduces the amount of player knockback velocity", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static VelocityModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      if (this.modeConfig.getValue() == VelocityModule.VelocityMode.NORMAL) {
         DecimalFormat decimal = new DecimalFormat("0.0");
         return String.format("H:%s%%, V:%s%%", decimal.format(this.horizontalConfig.getValue()), decimal.format(this.verticalConfig.getValue()));
      } else {
         return EnumFormatter.formatEnum(this.modeConfig.getValue());
      }
   }

   @Override
   public void onEnable() {
      this.cancelVelocity = false;
   }

   @Override
   public void onDisable() {
      if (this.cancelVelocity) {
         if (this.modeConfig.getValue() == VelocityModule.VelocityMode.GRIM) {
            float yaw = mc.player.getYRot();
            float pitch = mc.player.getXRot();
            if (Managers.ROTATION.isRotating()) {
               yaw = Managers.ROTATION.getRotationYaw();
               pitch = Managers.ROTATION.getRotationPitch();
            }

            Managers.NETWORK.sendPacket(new PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround()));
            Managers.NETWORK
               .sendPacket(
                  new ServerboundPlayerActionPacket(
                     Action.STOP_DESTROY_BLOCK, mc.player.isVisuallyCrawling() ? mc.player.blockPosition() : mc.player.blockPosition().above(), Direction.DOWN
                  )
               );
         }

         this.cancelVelocity = false;
      }

      this.concealVelocity = false;
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ClientboundPlayerPositionPacket && this.concealConfig.getValue()) {
            this.concealVelocity = true;
         }

         if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && this.knockbackConfig.getValue()) {
            if (packet.getId() != mc.player.getId()) {
               return;
            }

            if (this.concealVelocity && packet.getXa() == 0.0 && packet.getZa() == 0.0 && packet.getZa() == 0.0) {
               this.concealVelocity = false;
               return;
            }

            if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS) {
               if (!this.isPhased() && (!this.wallsTrappedConfig.getValue() || !this.isWallsTrapped())) {
                  return;
               }

               if (this.wallsAirConfig.getValue() && !Managers.POSITION.isOnGround()) {
                  return;
               }
            }

            switch ((VelocityModule.VelocityMode)this.modeConfig.getValue()) {
               case NORMAL:
               case WALLS:
                  if (this.horizontalConfig.getValue() == 0.0F && this.verticalConfig.getValue() == 0.0F) {
                     event.cancel();
                     return;
                  }

                  ((AccessorEntityVelocityUpdateS2CPacket)packet).setVelocityX((int)(packet.getXa() * (this.horizontalConfig.getValue() / 100.0F)));
                  ((AccessorEntityVelocityUpdateS2CPacket)packet).setVelocityY((int)(packet.getYa() * (this.verticalConfig.getValue() / 100.0F)));
                  ((AccessorEntityVelocityUpdateS2CPacket)packet).setVelocityZ((int)(packet.getZa() * (this.horizontalConfig.getValue() / 100.0F)));
                  break;
               case GRIM:
                  if (!Managers.ANTICHEAT.hasPassed(100L)) {
                     return;
                  }

                  event.cancel();
                  this.cancelVelocity = true;
                  break;
               case GRIM_V3:
                  event.setCanceled(this.isPhased());
            }
         } else if (event.getPacket() instanceof ClientboundExplodePacket packet && this.explosionConfig.getValue()) {
            if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS && !this.isPhased()) {
               return;
            }

            switch ((VelocityModule.VelocityMode)this.modeConfig.getValue()) {
               case NORMAL:
               case WALLS:
                  if (this.horizontalConfig.getValue() == 0.0F && this.verticalConfig.getValue() == 0.0F) {
                     event.cancel();
                     break;
                  }

                  ((AccessorExplosionS2CPacket)packet).setPlayerVelocityX(packet.getKnockbackX() * (this.horizontalConfig.getValue() / 100.0F));
                  ((AccessorExplosionS2CPacket)packet).setPlayerVelocityY(packet.getKnockbackY() * (this.verticalConfig.getValue() / 100.0F));
                  ((AccessorExplosionS2CPacket)packet).setPlayerVelocityZ(packet.getKnockbackZ() * (this.horizontalConfig.getValue() / 100.0F));
                  break;
               case GRIM:
                  if (!Managers.ANTICHEAT.hasPassed(100L)) {
                     return;
                  }

                  event.cancel();
                  this.cancelVelocity = true;
                  break;
               case GRIM_V3:
                  event.setCanceled(this.isPhased());
            }

            if (event.isCanceled()) {
               mc.executeIfPossible(
                  () -> ((AccessorClientWorld)mc.level)
                     .hookPlaySound(
                        packet.getX(),
                        packet.getY(),
                        packet.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.BLOCKS,
                        4.0F,
                        (1.0F + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2F) * 0.7F,
                        false,
                        RANDOM.nextLong()
                     )
               );
            }
         } else if (event.getPacket() instanceof ClientboundBundlePacket packet) {
            List<Packet<?>> allowedBundle = new ArrayList<>();

            for (Packet<?> packet1 : packet.subPackets()) {
               if (packet1 instanceof ClientboundExplodePacket packet2 && this.explosionConfig.getValue()) {
                  mc.executeIfPossible(
                     () -> ((AccessorClientWorld)mc.level)
                        .hookPlaySound(
                           packet2.getX(),
                           packet2.getY(),
                           packet2.getZ(),
                           SoundEvents.GENERIC_EXPLODE.value(),
                           SoundSource.BLOCKS,
                           4.0F,
                           (1.0F + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2F) * 0.7F,
                           false,
                           RANDOM.nextLong()
                        )
                  );
                  if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS && !this.isPhased()) {
                     allowedBundle.add(packet1);
                     continue;
                  }

                  switch ((VelocityModule.VelocityMode)this.modeConfig.getValue()) {
                     case NORMAL:
                     case WALLS:
                        if (this.horizontalConfig.getValue() == 0.0F && this.verticalConfig.getValue() == 0.0F) {
                           continue;
                        }

                        ((AccessorExplosionS2CPacket)packet2).setPlayerVelocityX(packet2.getKnockbackX() * (this.horizontalConfig.getValue() / 100.0F));
                        ((AccessorExplosionS2CPacket)packet2).setPlayerVelocityY(packet2.getKnockbackY() * (this.verticalConfig.getValue() / 100.0F));
                        ((AccessorExplosionS2CPacket)packet2).setPlayerVelocityZ(packet2.getKnockbackZ() * (this.horizontalConfig.getValue() / 100.0F));
                        break;
                     case GRIM:
                        if (Managers.ANTICHEAT.hasPassed(100L)) {
                           allowedBundle.add(packet1);
                        } else {
                           this.cancelVelocity = true;
                        }
                        continue;
                     case GRIM_V3:
                        if (this.isPhased()) {
                           continue;
                        }
                  }
               } else if (packet1 instanceof ClientboundSetEntityMotionPacket packet2 && this.knockbackConfig.getValue()) {
                  if (packet2.getId() != mc.player.getId()) {
                     allowedBundle.add(packet1);
                     continue;
                  }

                  if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS) {
                     if (!this.isPhased() && (!this.wallsTrappedConfig.getValue() || !this.isWallsTrapped())) {
                        allowedBundle.add(packet1);
                        return;
                     }

                     if (this.wallsAirConfig.getValue() && !Managers.POSITION.isOnGround()) {
                        allowedBundle.add(packet1);
                        continue;
                     }
                  }

                  switch ((VelocityModule.VelocityMode)this.modeConfig.getValue()) {
                     case NORMAL:
                     case WALLS:
                        if (this.horizontalConfig.getValue() == 0.0F && this.verticalConfig.getValue() == 0.0F) {
                           continue;
                        }

                        ((AccessorEntityVelocityUpdateS2CPacket)packet2).setVelocityX((int)(packet2.getXa() * (this.horizontalConfig.getValue() / 100.0F)));
                        ((AccessorEntityVelocityUpdateS2CPacket)packet2).setVelocityY((int)(packet2.getYa() * (this.verticalConfig.getValue() / 100.0F)));
                        ((AccessorEntityVelocityUpdateS2CPacket)packet2).setVelocityZ((int)(packet2.getZa() * (this.horizontalConfig.getValue() / 100.0F)));
                        break;
                     case GRIM:
                        if (!Managers.ANTICHEAT.hasPassed(100L)) {
                           allowedBundle.add(packet1);
                        } else {
                           this.cancelVelocity = true;
                        }
                        continue;
                     case GRIM_V3:
                        if (this.isPhased()) {
                           continue;
                        }
                  }
               }

               allowedBundle.add(packet1);
            }

            ((AccessorBundlePacket)packet).setIterable(allowedBundle);
         } else if (event.getPacket() instanceof ClientboundDamageEventPacket packet
            && packet.entityId() == mc.player.getId()
            && this.modeConfig.getValue() == VelocityModule.VelocityMode.GRIM_V3
            && this.isPhased()) {
            Managers.NETWORK.sendPacket(new StatusOnly(false));
            Managers.NETWORK.sendPacket(new StatusOnly(true));
         } else if (event.getPacket() instanceof ClientboundEntityEventPacket packet
            && packet.getEventId() == 31
            && this.pushFishhookConfig.getValue()
            && packet.getEntity(mc.level) instanceof FishingHook hook
            && hook.getHookedIn() == mc.player) {
            event.cancel();
         }
      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      this.concealVelocity = false;
      if (this.cancelVelocity) {
         if (this.modeConfig.getValue() == VelocityModule.VelocityMode.GRIM) {
            float yaw = Managers.ROTATION.getServerYaw();
            float pitch = Managers.ROTATION.getServerPitch();
            if (Managers.ROTATION.isRotating()) {
               yaw = Managers.ROTATION.getRotationYaw();
               pitch = Managers.ROTATION.getRotationPitch();
            }

            Managers.NETWORK.sendPacket(new PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround()));
            Managers.NETWORK
               .sendPacket(
                  new ServerboundPlayerActionPacket(
                     Action.STOP_DESTROY_BLOCK, mc.player.isVisuallyCrawling() ? mc.player.blockPosition() : mc.player.blockPosition().above(), Direction.DOWN
                  )
               );
         }

         this.cancelVelocity = false;
      }
   }

   @EventListener
   public void onPushEntity(PushEntityEvent event) {
      if (this.pushEntitiesConfig.getValue() && event.getPushed().equals(mc.player)) {
         event.cancel();
      }
   }

   @EventListener
   public void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
      if (this.pushBlocksConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onPushFluid(PushFluidsEvent event) {
      if (this.pushLiquidsConfig.getValue()) {
         event.cancel();
      }
   }

   private boolean isWallsTrapped() {
      BlockPos headPos = mc.player.blockPosition().above(mc.player.isVisuallyCrawling() ? 1 : 2);
      return mc.level.getBlockState(headPos).canBeReplaced()
         ? false
         : SurroundModule.getInstance()
            .getSurroundNoDown(mc.player)
            .stream()
            .noneMatch(blockPos -> mc.level.getBlockState(mc.player.isVisuallyCrawling() ? blockPos : blockPos.above()).canBeReplaced());
   }

   private boolean isPhased() {
      return PositionUtil.getAllInBox(mc.player.getBoundingBox()).stream().anyMatch(blockPos -> !mc.level.getBlockState(blockPos).canBeReplaced());
   }

   private static enum VelocityMode {
      NORMAL,
      WALLS,
      GRIM,
      GRIM_V3;
   }
}

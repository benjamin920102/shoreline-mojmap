package net.shoreline.client.impl.module.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.JumpRotationEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.network.SprintCancelEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.impl.module.client.AnticheatModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.client.util.player.PlayerUtil;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class SprintModule extends RotationModule {
   private static SprintModule INSTANCE;
   Config<SprintModule.SprintMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "Sprinting mode. Rage allows for multi-directional sprinting.", SprintModule.SprintMode.LEGIT, SprintModule.SprintMode.values())
   );
   Config<Boolean> jumpFixConfig = this.register(
      new BooleanConfig(
         "JumpFix",
         "Fixes jumping slowdown in Rage sprint",
         true,
         () -> this.modeConfig.getValue() == SprintModule.SprintMode.RAGE || this.modeConfig.getValue() == SprintModule.SprintMode.RAGE_STRICT
      )
   );

   public SprintModule() {
      super("Sprint", "Automatically sprints", ModuleCategory.MOVEMENT, 110);
      INSTANCE = this;
   }

   public static SprintModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (this.canSprint()) {
            float sprintYaw = this.getSprintYaw(mc.player.getYRot());
            if (this.checkSprintAngle(sprintYaw)) {
               return;
            }

            switch ((SprintModule.SprintMode)this.modeConfig.getValue()) {
               case LEGIT:
                  if (mc.player.input.hasForwardImpulse() && (!mc.player.horizontalCollision || mc.player.minorHorizontalCollision)) {
                     mc.player.setSprinting(true);
                  }
                  break;
               case RAGE:
               case RAGE_STRICT:
               case GRIM:
                  mc.player.setSprinting(true);
            }
         }
      }
   }

   @EventListener
   public void onSprintCancel(SprintCancelEvent event) {
      if (this.canSprint() && (this.modeConfig.getValue() == SprintModule.SprintMode.RAGE || this.modeConfig.getValue() == SprintModule.SprintMode.RAGE_STRICT)
         )
       {
         float sprintYaw = this.getSprintYaw(mc.player.getYRot());
         if (this.checkSprintAngle(sprintYaw)) {
            return;
         }

         event.cancel();
      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (this.canSprint() && this.modeConfig.getValue() == SprintModule.SprintMode.RAGE_STRICT) {
         this.setRotation(this.getSprintYaw(mc.player.getYRot()), mc.player.getXRot());
      }
   }

   @EventListener
   public void onJumpYaw(JumpRotationEvent event) {
      if (this.jumpFixConfig.getValue()
         && (this.modeConfig.getValue() == SprintModule.SprintMode.RAGE || this.modeConfig.getValue() == SprintModule.SprintMode.RAGE_STRICT)) {
         float yaw = event.getYaw();
         float forward = Math.signum(mc.player.input.forwardImpulse);
         float strafe = 90.0F * Math.signum(mc.player.input.leftImpulse);
         if (forward != 0.0F) {
            strafe *= forward * 0.5F;
         }

         yaw -= strafe;
         if (forward < 0.0F) {
            yaw -= 180.0F;
         }

         event.cancel();
         event.setYaw(yaw);
      }
   }

   private boolean canSprint() {
      return AnticheatModule.getInstance().getWebJumpFix() && PlayerUtil.inWeb(1.0)
         ? false
         : MovementUtil.isInputtingMovement()
            && !mc.player.isShiftKeyDown()
            && !mc.player.isHandsBusy()
            && !mc.player.isFallFlying()
            && !mc.player.isInWater()
            && !mc.player.isInLava()
            && !mc.player.isSuppressingSlidingDownLadder()
            && !mc.player.hasEffect(MobEffects.BLINDNESS)
            && mc.player.getFoodData().getFoodLevel() > 6.0F;
   }

   private boolean checkSprintAngle(float sprintYaw) {
      if (this.modeConfig.getValue() == SprintModule.SprintMode.RAGE_STRICT) {
         return Mth.degreesDifferenceAbs(sprintYaw, Managers.ROTATION.getServerYaw()) > 0.0F;
      } else {
         return this.modeConfig.getValue() == SprintModule.SprintMode.GRIM
            ? Mth.degreesDifferenceAbs(mc.player.getYRot(), Managers.ROTATION.getServerYaw()) > 0.0F
            : false;
      }
   }

   public float getSprintYaw(float yaw) {
      boolean forward = mc.options.keyUp.isDown();
      boolean backward = mc.options.keyDown.isDown();
      boolean left = mc.options.keyLeft.isDown();
      boolean right = mc.options.keyRight.isDown();
      if (forward && !backward) {
         if (left && !right) {
            yaw -= 45.0F;
         } else if (right && !left) {
            yaw += 45.0F;
         }
      } else if (backward && !forward) {
         yaw += 180.0F;
         if (left && !right) {
            yaw += 45.0F;
         } else if (right && !left) {
            yaw -= 45.0F;
         }
      } else if (left && !right) {
         yaw -= 90.0F;
      } else if (right && !left) {
         yaw += 90.0F;
      }

      return Mth.wrapDegrees(yaw);
   }

   public static enum SprintMode {
      LEGIT,
      RAGE,
      RAGE_STRICT,
      GRIM;
   }
}

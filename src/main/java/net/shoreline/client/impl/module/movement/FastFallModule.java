package net.shoreline.client.impl.module.movement;

import net.minecraft.world.phys.AABB;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.player.PlayerMoveEvent;
import net.shoreline.client.impl.event.network.TickMovementEvent;
import net.shoreline.client.impl.module.exploit.PacketFlyModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class FastFallModule extends ToggleModule {
   Config<Float> heightConfig = this.register(new NumberConfig<>("Height", "The maximum fall height", 1.0F, 3.0F, 10.0F));
   Config<FastFallModule.FallMode> fallModeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for falling down blocks", FastFallModule.FallMode.STEP, FastFallModule.FallMode.values())
   );
   Config<Boolean> accelerateConfig = this.register(
      new BooleanConfig("Accelerate", "Accels the fall speed", false, () -> this.fallModeConfig.getValue() == FastFallModule.FallMode.STEP)
   );
   Config<Integer> shiftTicksConfig = this.register(
      new NumberConfig<>("ShiftTicks", "Number of ticks to shift ahead", 1, 3, 5, () -> this.fallModeConfig.getValue() == FastFallModule.FallMode.SHIFT)
   );
   private boolean prevOnGround;
   private boolean cancelFallMovement;
   private int fallTicks;
   private final Timer fallTimer = new CacheTimer();

   public FastFallModule() {
      super("FastFall", "Falls down blocks faster", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onDisable() {
      this.cancelFallMovement = false;
      this.fallTicks = 0;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         this.prevOnGround = mc.player.onGround();
         if (this.fallModeConfig.getValue() == FastFallModule.FallMode.STEP) {
            if (mc.player.isHandsBusy()
               || mc.player.isFallFlying()
               || mc.player.isSuppressingSlidingDownLadder()
               || mc.player.isInLava()
               || mc.player.isInWater()
               || mc.player.input.jumping
               || mc.player.input.shiftKeyDown
               || !Managers.ANTICHEAT.hasPassed(1000L)) {
               return;
            }

            if (SpeedModule.getInstance().isEnabled()
               || LongJumpModule.getInstance().isEnabled()
               || FlightModule.getInstance().isEnabled()
               || PacketFlyModule.getInstance().isEnabled()) {
               return;
            }

            double fallHeight = this.traceDown();
            if (fallHeight > 0.01 && fallHeight <= this.heightConfig.getValue().floatValue() && mc.player.onGround()) {
               Managers.MOVEMENT.setMotionXZ(mc.player.getDeltaMovement().x * 0.05, mc.player.getDeltaMovement().z * 0.05);
               Managers.MOVEMENT.setMotionY(this.accelerateConfig.getValue() ? mc.player.getDeltaMovement().y - 0.62F : -3.0);
            }
         }
      }
   }

   @EventListener
   public void onTickMovement(TickMovementEvent event) {
      if (this.fallModeConfig.getValue() == FastFallModule.FallMode.SHIFT) {
         if (mc.player.isHandsBusy()
            || mc.player.isFallFlying()
            || mc.player.isSuppressingSlidingDownLadder()
            || mc.player.isInLava()
            || mc.player.isInWater()
            || mc.player.input.jumping
            || mc.player.input.shiftKeyDown
            || !Managers.ANTICHEAT.hasPassed(1000L)) {
            return;
         }

         if (!Managers.ANTICHEAT.hasPassed(1000L)
            || !this.fallTimer.passed(1000)
            || SpeedModule.getInstance().isEnabled()
            || LongJumpModule.getInstance().isEnabled()
            || FlightModule.getInstance().isEnabled()
            || PacketFlyModule.getInstance().isEnabled()) {
            return;
         }

         double fallHeight = this.traceDown();
         if (fallHeight > 0.01 && fallHeight <= this.heightConfig.getValue().floatValue() + 0.01) {
            if (mc.player.onGround()) {
               Managers.MOVEMENT.setMotionXZ(mc.player.getDeltaMovement().x * 0.05, mc.player.getDeltaMovement().z * 0.05);
            }

            if (mc.player.getDeltaMovement().y < 0.0 && this.prevOnGround && !mc.player.onGround()) {
               this.fallTimer.reset();
               event.cancel();
               event.setIterations(this.shiftTicksConfig.getValue());
               this.cancelFallMovement = true;
               this.fallTicks = 0;
            }
         }
      }
   }

   @EventListener
   public void onPlayerMove(PlayerMoveEvent event) {
      if (!FlightModule.getInstance().isEnabled() && !PacketFlyModule.getInstance().isEnabled()) {
         if (this.cancelFallMovement && this.fallModeConfig.getValue() == FastFallModule.FallMode.SHIFT) {
            event.setX(0.0);
            event.setZ(0.0);
            Managers.MOVEMENT.setMotionXZ(0.0, 0.0);
            this.fallTicks++;
            if (this.fallTicks > this.shiftTicksConfig.getValue()) {
               this.cancelFallMovement = false;
               this.fallTicks = 0;
            }
         }
      }
   }

   private double traceDown() {
      AABB bb = mc.player.getBoundingBox();

      for (double i = 0.0; i < this.heightConfig.getValue().floatValue() + 0.5; i += 0.01) {
         if (!mc.level.noCollision(mc.player, bb.move(0.0, -i, 0.0))) {
            return i;
         }
      }

      return -1.0;
   }

   public static enum FallMode {
      STEP,
      SHIFT;
   }
}

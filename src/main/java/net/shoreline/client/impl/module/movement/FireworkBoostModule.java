package net.shoreline.client.impl.module.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.mixin.accessor.AccessorFireworkRocketEntity;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public final class FireworkBoostModule extends ToggleModule {
   Config<Double> speedConfig = this.register(new NumberConfig<>("Speed", "The initial speed of the rocket.", 1.0, 1.5, 10.0));
   Config<Boolean> accelerateConfig = this.register(new BooleanConfig("Accelerate", "Whether or not to accelerate the rocket.", false));
   Config<Double> accelSpeedConfig = this.register(
      new NumberConfig<>("AccelSpeed", "The speed to accelerate the rocket at.", 0.1, 0.5, 5.0, () -> this.accelerateConfig.getValue())
   );
   Config<Double> maxSpeedConfig = this.register(
      new NumberConfig<>("MaxSpeed", "The initial speed of the rocket.", 1.0, 3.0, 10.0, () -> this.accelerateConfig.getValue())
   );
   private int tickCounter;
   private double speed;

   public FireworkBoostModule() {
      super("FireworkBoost", "Allows you to change the acceleration of your firework.", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onDisable() {
      this.speed = 0.0;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && !mc.player.isInLava()) {
         if (this.isBoostedByRocket() && mc.player.isFallFlying()) {
            Vec3 vec3d = mc.player.getLookAngle();
            Vec3 vec3d2 = mc.player.getDeltaMovement();
            if (this.accelerateConfig.getValue()) {
               this.speed = this.speed + this.accelSpeedConfig.getValue() * this.tickCounter;
               if (this.speed < this.maxSpeedConfig.getValue()) {
                  this.tickCounter++;
               } else {
                  this.speed = Mth.clamp(this.speed, 0.0, this.maxSpeedConfig.getValue());
               }
            } else {
               this.speed = this.speedConfig.getValue();
            }

            mc.player
               .setDeltaMovement(
                  vec3d2.add(
                     vec3d.x * 0.1 + (vec3d.x * this.speed - vec3d2.x) * 0.5,
                     vec3d.y * 0.1 + (vec3d.y * this.speed - vec3d2.y) * 0.5,
                     vec3d.z * 0.1 + (vec3d.z * this.speed - vec3d2.z) * 0.5
                  )
               );
         } else {
            this.speed = 0.0;
         }
      }
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
}

package net.shoreline.client.impl.module.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.imixin.IMinecraftClient;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorMinecraftClient;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class TriggerModule extends ToggleModule {
   Config<TriggerModule.TriggerMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for activating the trigger bot", TriggerModule.TriggerMode.MOUSE_BUTTON, TriggerModule.TriggerMode.values())
   );
   Config<Float> attackSpeedConfig = this.register(new NumberConfig<>("AttackSpeed", "The speed to attack entities", 0.1F, 8.0F, 20.0F));
   Config<Float> randomSpeedConfig = this.register(new NumberConfig<>("RandomSpeed", "The speed randomizer for attacks", 0.1F, 2.0F, 10.0F));
   private final Timer triggerTimer = new CacheTimer();

   public TriggerModule() {
      super("Trigger", "Automatically attacks entities in the crosshair", ModuleCategory.COMBAT);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         boolean buttonDown = switch ((TriggerModule.TriggerMode)this.modeConfig.getValue()) {
            case MOUSE_BUTTON -> mc.mouseHandler.isLeftPressed();
            case MOUSE_OVER -> {
               if (mc.hitResult != null && mc.hitResult.getType() == Type.ENTITY) {
                  EntityHitResult entityHit = (EntityHitResult)mc.hitResult;
                  Entity crosshairEntity = entityHit.getEntity();
                  yield !mc.player.isAlliedTo(crosshairEntity) && !Managers.SOCIAL.isFriend(crosshairEntity.getName());
               } else {
                  yield false;
               }
            }
            case MOUSE_CLICK -> true;
         };
         double d = Math.random() * this.randomSpeedConfig.getValue().floatValue() * 2.0 - this.randomSpeedConfig.getValue().floatValue();
         if (buttonDown && this.triggerTimer.passed(1000.0 - Math.max(this.attackSpeedConfig.getValue().floatValue() + d, 0.5) * 50.0)) {
            ((IMinecraftClient)mc).leftClick();
            ((AccessorMinecraftClient)mc).hookSetAttackCooldown(0);
            this.triggerTimer.reset();
         }
      }
   }

   public static enum TriggerMode {
      MOUSE_BUTTON,
      MOUSE_OVER,
      MOUSE_CLICK;
   }
}

package net.shoreline.client.impl.module.misc;

import java.text.DecimalFormat;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.render.TickCounterEvent;
import net.shoreline.client.impl.module.movement.SpeedModule;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class TimerModule extends ToggleModule {
   private static TimerModule INSTANCE;
   Config<Float> ticksConfig = this.register(new NumberConfig<>("Ticks", "The game tick speed", 0.1F, 2.0F, 50.0F));
   Config<Boolean> tpsSyncConfig = this.register(new BooleanConfig("TPSSync", "Syncs game tick speed to server tick speed", false));
   private float prevTimer = -1.0F;
   private float timer = 1.0F;

   public TimerModule() {
      super("Timer", "Changes the client tick speed", ModuleCategory.MISCELLANEOUS);
   }

   public static TimerModule getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new TimerModule();
      }

      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      DecimalFormat decimal = new DecimalFormat("0.0#");
      return decimal.format(this.timer);
   }

   @Override
   public void toggle() {
      SpeedModule.getInstance().setPrevTimer();
      if (!SpeedModule.getInstance().isUsingTimer()) {
         super.toggle();
      }
   }

   @Override
   public void onDisable() {
      Managers.TICK.setClientTick(1.0F);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (SpeedModule.getInstance().isUsingTimer()) {
            return;
         }

         if (this.tpsSyncConfig.getValue()) {
            this.timer = Math.max(Managers.TICK.getTpsCurrent() / 20.0F, 0.1F);
            return;
         }

         this.timer = this.ticksConfig.getValue();
      }
   }

   @EventListener
   public void onTickCounter(TickCounterEvent event) {
      if (this.timer != 1.0F) {
         event.cancel();
         event.setTicks(this.timer);
      }
   }

   public float getTimer() {
      return this.timer;
   }

   public void setTimer(float timer) {
      this.prevTimer = this.timer;
      this.timer = timer;
   }

   public void resetTimer() {
      if (this.prevTimer > 0.0F) {
         this.timer = this.prevTimer;
         this.prevTimer = -1.0F;
      }
   }
}

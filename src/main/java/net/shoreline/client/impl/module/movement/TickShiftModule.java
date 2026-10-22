package net.shoreline.client.impl.module.movement;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class TickShiftModule extends ToggleModule {
   Config<Integer> ticksConfig = this.register(new NumberConfig<>("MaxTicks", "Maximum charge ticks", 1, 20, 40));
   Config<Integer> packetsConfig = this.register(new NumberConfig<>("Packets", "Packets to release from storage every tick", 1, 1, 5));
   Config<Integer> chargeSpeedConfig = this.register(new NumberConfig<>("ChargeSpeed", "The speed to charge the stored packets", 1, 1, 5));
   private int packets;

   public TickShiftModule() {
      super("TickShift", "Exploits NCP to speed up ticks", ModuleCategory.MOVEMENT);
   }

   @Override
   public String getModuleData() {
      return String.valueOf(this.packets);
   }

   @Override
   public void onDisable() {
      Managers.TICK.setClientTick(1.0F);
   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (!MovementUtil.isMoving() && mc.player.onGround()) {
            this.packets = this.packets + this.chargeSpeedConfig.getValue();
            if (this.packets > this.ticksConfig.getValue()) {
               this.packets = this.ticksConfig.getValue();
            }
         } else {
            this.packets = this.packets - this.packetsConfig.getValue();
            if (this.packets <= 0) {
               this.packets = 0;
               Managers.TICK.setClientTick(1.0F);
               return;
            }

            Managers.TICK.setClientTick(this.packetsConfig.getValue().intValue() + 1.0F);
         }
      }
   }
}

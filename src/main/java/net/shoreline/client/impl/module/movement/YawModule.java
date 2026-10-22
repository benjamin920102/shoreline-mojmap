package net.shoreline.client.impl.module.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Llama;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.LookDirectionEvent;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class YawModule extends ToggleModule {
   Config<Boolean> lockConfig = this.register(new BooleanConfig("Lock", "Locks the yaw in cardinal direction", false));

   public YawModule() {
      super("Yaw", "Locks player yaw to a cardinal axis", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         float yaw = Math.round(mc.player.getYRot() / 45.0F) * 45.0F;
         Entity vehicle = mc.player.getVehicle();
         if (vehicle != null) {
            vehicle.setYRot(yaw);
            if (vehicle instanceof Llama llama) {
               llama.setYHeadRot(yaw);
            }

            return;
         }

         mc.player.setYRot(yaw);
         mc.player.setYHeadRot(yaw);
      }
   }

   @EventListener
   public void onLookDirection(LookDirectionEvent event) {
      if (this.lockConfig.getValue()) {
         event.cancel();
         float f = (float)event.getCursorDeltaY() * 0.15F;
         mc.player.setXRot(Mth.clamp(mc.player.getXRot() + f, -90.0F, 90.0F));
      }
   }
}

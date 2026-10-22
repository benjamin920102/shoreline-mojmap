package net.shoreline.client.impl.module.movement;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Llama;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.mob.PigAIEvent;
import net.shoreline.client.impl.event.entity.passive.EntitySteerEvent;
import net.shoreline.client.impl.event.network.MountJumpStrengthEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class EntityControlModule extends ToggleModule {
   Config<Float> jumpStrengthConfig = this.register(new NumberConfig<>("JumpStrength", "The fixed jump strength of the mounted entity", 0.1F, 0.7F, 2.0F));
   Config<Boolean> noPigMoveConfig = this.register(new BooleanConfig("NoPigAI", "Prevents the pig movement when controlling pigs", false));

   public EntityControlModule() {
      super("EntityControl", "Allows you to steer entities without a saddle", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onTick(TickEvent event) {
      Entity vehicle = mc.player.getVehicle();
      if (vehicle != null) {
         vehicle.setYRot(mc.player.getYRot());
         if (vehicle instanceof Llama llama) {
            llama.yHeadRot = mc.player.getYRot();
         }
      }
   }

   @EventListener
   public void onEntitySteer(EntitySteerEvent event) {
      event.cancel();
   }

   @EventListener
   public void onMountJumpStrength(MountJumpStrengthEvent event) {
      event.cancel();
      event.setJumpStrength(this.jumpStrengthConfig.getValue());
   }

   @EventListener
   public void onPigAI(PigAIEvent event) {
      if (mc.player != null) {
         if (this.noPigMoveConfig.getValue() && event.getPigEntity().getPassengers().contains(mc.player)) {
            event.cancel();
         }
      }
   }
}

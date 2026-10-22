package net.shoreline.client.impl.module.misc;

import java.util.Set;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.mixin.accessor.AccessorGameOptions;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class SkinBlinkModule extends ToggleModule {
   Config<Float> speedConfig = this.register(new NumberConfig<>("Speed", "The speed to toggle the player model parts", 0.0F, 0.1F, 20.0F));
   Config<Boolean> randomConfig = this.register(new BooleanConfig("Random", "Randomizes the toggling of each skin model part", false));
   private final Timer blinkTimer = new CacheTimer();
   private Set<PlayerModelPart> enabledPlayerModelParts;

   public SkinBlinkModule() {
      super("SkinBlink", "Toggles the skin model rendering", ModuleCategory.MISCELLANEOUS);
   }

   @Override
   public void onEnable() {
      if (mc.options != null) {
         this.enabledPlayerModelParts = ((AccessorGameOptions)mc.options).getPlayerModelParts();
      }
   }

   @Override
   public void onDisable() {
      if (this.enabledPlayerModelParts != null && mc.options != null) {
         for (PlayerModelPart modelPart : PlayerModelPart.values()) {
            mc.options.toggleModelPart(modelPart, this.enabledPlayerModelParts.contains(modelPart));
         }
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST && this.blinkTimer.passed(this.speedConfig.getValue() * 1000.0F)) {
         Set<PlayerModelPart> currentModelParts = ((AccessorGameOptions)mc.options).getPlayerModelParts();

         for (PlayerModelPart modelPart : PlayerModelPart.values()) {
            mc.options.toggleModelPart(modelPart, this.randomConfig.getValue() ? Math.random() < 0.5 : !currentModelParts.contains(modelPart));
         }

         this.blinkTimer.reset();
      }
   }
}

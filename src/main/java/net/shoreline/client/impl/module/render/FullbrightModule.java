package net.shoreline.client.impl.module.render;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.network.GameJoinEvent;
import net.shoreline.client.impl.event.render.LightmapGammaEvent;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class FullbrightModule extends ToggleModule {
   Config<FullbrightModule.Brightness> brightnessConfig = this.register(
      new EnumConfig<>("Mode", "Mode for world brightness", FullbrightModule.Brightness.GAMMA, FullbrightModule.Brightness.values())
   );

   public FullbrightModule() {
      super("Fullbright", "Brightens the world", ModuleCategory.RENDER);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.level != null && this.brightnessConfig.getValue() == FullbrightModule.Brightness.POTION) {
         mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0));
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null && mc.level != null && this.brightnessConfig.getValue() == FullbrightModule.Brightness.POTION) {
         mc.player.removeEffect(MobEffects.NIGHT_VISION);
      }
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      this.onDisable();
      this.onEnable();
   }

   @EventListener
   public void onLightmapGamma(LightmapGammaEvent event) {
      if (this.brightnessConfig.getValue() == FullbrightModule.Brightness.GAMMA) {
         event.cancel();
         event.setGamma(-1);
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (mc.player != null
         && this.brightnessConfig == event.getConfig()
         && event.getStage() == StageEvent.EventStage.POST
         && this.brightnessConfig.getValue() != FullbrightModule.Brightness.POTION) {
         mc.player.removeEffect(MobEffects.NIGHT_VISION);
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.brightnessConfig.getValue() == FullbrightModule.Brightness.POTION && !mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
         mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0));
      }
   }

   public static enum Brightness {
      GAMMA,
      POTION;
   }
}

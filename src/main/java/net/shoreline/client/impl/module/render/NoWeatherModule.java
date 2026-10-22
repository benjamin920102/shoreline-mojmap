package net.shoreline.client.impl.module.render;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.biome.BiomeEffectsEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class NoWeatherModule extends ToggleModule {
   Config<NoWeatherModule.Weather> weatherConfig = this.register(
      new EnumConfig<>("Weather", "The world weather", NoWeatherModule.Weather.CLEAR, NoWeatherModule.Weather.values())
   );
   private NoWeatherModule.Weather weather;

   public NoWeatherModule() {
      super("NoWeather", "Prevents weather rendering", ModuleCategory.RENDER);
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.weatherConfig.getValue());
   }

   @Override
   public void onEnable() {
      if (mc.level != null) {
         if (mc.level.isThundering()) {
            this.weather = NoWeatherModule.Weather.THUNDER;
         } else if (mc.level.isRaining()) {
            this.weather = NoWeatherModule.Weather.RAIN;
         } else {
            this.weather = NoWeatherModule.Weather.CLEAR;
         }

         this.setWeather(this.weatherConfig.getValue());
      }
   }

   @Override
   public void onDisable() {
      if (mc.level != null && this.weather != null) {
         this.setWeather(this.weather);
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST) {
         this.setWeather(this.weatherConfig.getValue());
      }
   }

   @EventListener
   public void onBiomeEffects(BiomeEffectsEvent event) {
      if (this.weatherConfig.getValue() == NoWeatherModule.Weather.ASH) {
         event.cancel();
         event.setParticleConfig(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.118093334F));
      }
   }

   private void setWeather(NoWeatherModule.Weather weather) {
      switch (weather) {
         case CLEAR:
         case ASH:
            mc.level.getLevelData().setRaining(false);
            mc.level.setRainLevel(0.0F);
            mc.level.setThunderLevel(0.0F);
            break;
         case RAIN:
            mc.level.getLevelData().setRaining(true);
            mc.level.setRainLevel(1.0F);
            mc.level.setThunderLevel(0.0F);
            break;
         case THUNDER:
            mc.level.getLevelData().setRaining(true);
            mc.level.setRainLevel(2.0F);
            mc.level.setThunderLevel(1.0F);
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof ClientboundGameEventPacket packet
         && (
            packet.getEvent() == ClientboundGameEventPacket.START_RAINING
               || packet.getEvent() == ClientboundGameEventPacket.STOP_RAINING
               || packet.getEvent() == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE
               || packet.getEvent() == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE
         )) {
         event.cancel();
      }
   }

   public static enum Weather {
      CLEAR,
      RAIN,
      THUNDER,
      ASH;
   }
}

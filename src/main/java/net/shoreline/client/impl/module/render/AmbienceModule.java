package net.shoreline.client.impl.module.render;

import java.awt.Color;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.biome.BiomeColorEvent;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.render.AmbientColorEvent;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class AmbienceModule extends ToggleModule {
   private static AmbienceModule INSTANCE;
   Config<Boolean> lightConfig = this.register(new BooleanConfig("Light", "Colors the light", false));
   Config<Color> lightColorConfig = this.register(
      new ColorConfig("LightColor", "The color of the light", Color.RED, false, true, () -> this.lightConfig.getValue())
   );
   Config<Boolean> grassConfig = this.register(new BooleanConfig("Grass", "Colors the grass", false));
   Config<Color> grassColorConfig = this.register(
      new ColorConfig("GrassColor", "The color of the grass", Color.RED, false, true, () -> this.grassConfig.getValue())
   );
   Config<Boolean> foliageConfig = this.register(new BooleanConfig("Foliage", "Colors the foliage", false));
   Config<Color> foliageColorConfig = this.register(
      new ColorConfig("FoliageColor", "The color of the foliage", Color.RED, false, true, () -> this.foliageConfig.getValue())
   );
   Config<Boolean> waterConfig = this.register(new BooleanConfig("Water", "Colors the water", false));
   Config<Color> waterColorConfig = this.register(
      new ColorConfig("WaterColor", "The color of the water", Color.RED, false, true, () -> this.waterConfig.getValue())
   );

   public AmbienceModule() {
      super("Ambience", "Changes rendering of world", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static AmbienceModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      if (mc.level != null) {
         mc.levelRenderer.allChanged();
      }
   }

   @Override
   public void onDisable() {
      if (mc.level != null) {
         mc.levelRenderer.allChanged();
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (mc.level != null
         && event.getConfig().getContainer() == this
         && (event.getConfig() != this.lightConfig || event.getConfig() != this.lightColorConfig)
         && event.getStage() == StageEvent.EventStage.POST) {
         mc.levelRenderer.allChanged();
      }
   }

   @EventListener
   public void onAmbientColor(AmbientColorEvent event) {
      if (this.lightConfig.getValue()) {
         event.cancel();
         event.setColor(this.lightColorConfig.getValue());
      }
   }

   @EventListener
   public void onGrassColor(BiomeColorEvent.Grass event) {
      if (this.grassConfig.getValue()) {
         event.cancel();
         event.setColor(this.grassColorConfig.getValue());
      }
   }

   @EventListener
   public void onFoliageColor(BiomeColorEvent.Foliage event) {
      if (this.foliageConfig.getValue()) {
         event.cancel();
         event.setColor(this.foliageColorConfig.getValue());
      }
   }

   @EventListener
   public void onWaterColor(BiomeColorEvent.Water event) {
      if (this.waterConfig.getValue()) {
         event.cancel();
         event.setColor(this.waterColorConfig.getValue());
      }
   }
}

package net.shoreline.client.impl.module.render;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.render.entity.RenderCrystalEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class CrystalModelModule extends ToggleModule {
   private static CrystalModelModule INSTANCE;
   Config<Float> spinConfig = this.register(new NumberConfig<>("Spin", "The spin speed of crystals", 0.0F, 1.0F, 10.0F));
   Config<Boolean> bounceCrystalConfig = this.register(new BooleanConfig("Bounce", "The crystal bounce", true));
   Config<Float> scaleConfig = this.register(new NumberConfig<>("Scale", "The scale of crystals", 0.1F, 1.0F, 1.5F));

   public CrystalModelModule() {
      super("CrystalModel", "Renders the crystal model", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static CrystalModelModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onRender(RenderCrystalEvent event) {
      event.setSpin(this.spinConfig.getValue());
      event.setBounce(this.bounceCrystalConfig.getValue());
      event.setScale(this.scaleConfig.getValue());
   }

   public boolean getBounce() {
      return this.bounceCrystalConfig.getValue();
   }

   public float getSpin() {
      return this.spinConfig.getValue();
   }

   public float getScale() {
      return this.scaleConfig.getValue();
   }
}

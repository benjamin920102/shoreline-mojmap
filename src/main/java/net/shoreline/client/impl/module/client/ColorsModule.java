package net.shoreline.client.impl.module.client;

import java.awt.Color;
import net.minecraft.util.Mth;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.module.ConcurrentModule;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.ClientColorEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class ColorsModule extends ConcurrentModule {
   private static ColorsModule INSTANCE;
   Config<Color> colorConfig = this.register(new ColorConfig("Global", "The primary client color", new Color(50, 100, 205), false, false));

   public ColorsModule() {
      super("Colors", "Client color scheme", ModuleCategory.CLIENT);
      INSTANCE = this;
   }

   public static ColorsModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onClientColor(ClientColorEvent event) {
      event.setRgb(this.getRGB());
   }

   public Color getColor() {
      return this.colorConfig.getValue();
   }

   public Color getColor(float alpha) {
      ColorConfig config = (ColorConfig)this.colorConfig;
      return new Color(config.getRed() / 255.0F, config.getGreen() / 255.0F, config.getBlue() / 255.0F, Mth.clamp(alpha, 0.0F, 1.0F));
   }

   public Color getColor(int alpha) {
      ColorConfig config = (ColorConfig)this.colorConfig;
      return new Color(config.getRed(), config.getGreen(), config.getBlue(), Mth.clamp(alpha, 0, 255));
   }

   public Integer getRGB() {
      return this.getColor().getRGB();
   }

   public int getRGB(int a) {
      return this.getColor(a).getRGB();
   }
}

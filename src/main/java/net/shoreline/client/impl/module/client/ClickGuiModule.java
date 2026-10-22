package net.shoreline.client.impl.module.client;

import net.shoreline.client.Shoreline;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.gui.click.ClickGuiScreen;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.render.animation.Easing;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class ClickGuiModule extends ToggleModule {
   private static ClickGuiModule INSTANCE;
   Config<Boolean> blurConfig = this.register(new BooleanConfig("Blur", "Adds a blur background to the panels", false));
   Config<Float> scaleConfig = this.register(new NumberConfig<>("Scale", "The gui scale", 0.5F, 1.0F, 3.0F));
   Config<Integer> scrollSpeedConfig = this.register(new NumberConfig<>("ScrollSpeed", "The speed of GUI scrolling", 5, 30, 100));
   Config<Boolean> soundsConfig = this.register(new BooleanConfig("Sounds", "Click sounds", true));
   Config<Boolean> descriptionsConfig = this.register(new BooleanConfig("Descriptions", "Shows feature descriptions", true));
   public static ClickGuiScreen CLICK_GUI_SCREEN;
   public static float CLICK_GUI_SCALE = 1.0F;
   private final Animation openCloseAnimation = new Animation(false, 400.0F, Easing.BACK_OUT);
   private final Animation transparencyAnimation = new Animation(false, 300.0F, Easing.CUBIC_IN_OUT);

   public ClickGuiModule() {
      super("ClickGui", "Opens the clickgui screen", ModuleCategory.CLIENT, 344);
      INSTANCE = this;
   }

   public static ClickGuiModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.level != null) {
         if (CLICK_GUI_SCREEN == null) {
            CLICK_GUI_SCALE = this.scaleConfig.getValue();
            CLICK_GUI_SCREEN = new ClickGuiScreen(this);
            Shoreline.CONFIG.loadClickGui();
         }

         if (CLICK_GUI_SCALE != this.scaleConfig.getValue()) {
            CLICK_GUI_SCALE = this.scaleConfig.getValue();
            CLICK_GUI_SCREEN = new ClickGuiScreen(this);
         }

         this.openCloseAnimation.setState(true);
         this.transparencyAnimation.setState(true);
         this.openCloseAnimation.reset();
         this.transparencyAnimation.reset();
         mc.setScreen(CLICK_GUI_SCREEN);
      } else {
         this.toggle();
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null && mc.level != null) {
         if (CLICK_GUI_SCREEN != null) {
            Shoreline.CONFIG.saveClickGui();
         }

         mc.player.clientSideCloseContainer();
         this.openCloseAnimation.setState(false);
         this.transparencyAnimation.setState(false);
      } else {
         this.toggle();
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST && event.getConfig() == this.scaleConfig && mc.level == null) {
         CLICK_GUI_SCALE = this.scaleConfig.getValue();
      }
   }

   public int getColor() {
      return ColorsModule.getInstance().getColor((int)(100.0 * this.openCloseAnimation.getFactor())).getRGB();
   }

   public int getColor(int a) {
      return ColorsModule.getInstance().getColor((int)(a * this.openCloseAnimation.getFactor())).getRGB();
   }

   public int getColor(float alpha) {
      return ColorsModule.getInstance().getColor((int)(100.0F * alpha * this.openCloseAnimation.getFactor())).getRGB();
   }

   public int getColor(int a, float alpha) {
      return ColorsModule.getInstance().getColor((int)(a * alpha * this.openCloseAnimation.getFactor())).getRGB();
   }

   public int fixTransparency(int color) {
      float alpha = this.getAlpha();
      if (alpha == 1.0F) {
         return color;
      } else {
         float colorAlpha = color >> 24 & 0xFF;
         alpha = Math.max(0.0F, Math.min(1.0F, alpha));
         int colorAlphaInt = Math.max(10, (int)(colorAlpha * alpha));
         return colorAlphaInt << 24 | color & 16777215;
      }
   }

   public boolean getBlur() {
      return this.blurConfig.getValue();
   }

   public boolean getSounds() {
      return this.soundsConfig.getValue();
   }

   public boolean getDescriptions() {
      return this.descriptionsConfig.getValue();
   }

   public float getAlpha() {
      return (float)this.transparencyAnimation.getFactor();
   }

   public float getScaleFactor() {
      return (float)this.openCloseAnimation.getFactor();
   }

   public int getScrollSpeed() {
      return this.scrollSpeedConfig.getValue();
   }
}

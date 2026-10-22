package net.shoreline.client.impl.module.misc;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.FramerateLimitEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class UnfocusedFPSModule extends ToggleModule {
   Config<Integer> limitConfig = this.register(new NumberConfig<>("Limit", "The FPS limit when game is in the background", 5, 30, 120));

   public UnfocusedFPSModule() {
      super("UnfocusedFPS", "Reduces FPS when game is in the background", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onFramerateLimit(FramerateLimitEvent event) {
      if (!mc.isWindowActive()) {
         event.cancel();
         event.setFramerateLimit(this.limitConfig.getValue());
      }
   }
}

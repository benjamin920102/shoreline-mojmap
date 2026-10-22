package net.shoreline.client.impl.module.movement;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.player.LedgeClipEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class SafeWalkModule extends ToggleModule {
   Config<Boolean> sneakConfig = this.register(new BooleanConfig("Sneak", "Sneaks at the edge of blocks", false));

   public SafeWalkModule() {
      super("SafeWalk", "Prevents you from walking off ledges", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onLedgeClip(LedgeClipEvent event) {
      if (!mc.player.isShiftKeyDown()) {
         if (this.sneakConfig.getValue()) {
            mc.player.setShiftKeyDown(true);
         }

         event.cancel();
         event.setClipped(true);
      }
   }
}

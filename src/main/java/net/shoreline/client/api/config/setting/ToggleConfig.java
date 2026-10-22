package net.shoreline.client.api.config.setting;

import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.eventbus.EventBus;

public class ToggleConfig extends BooleanConfig {
   public ToggleConfig(String name, String desc, Boolean val) {
      super(name, desc, val);
   }

   @Override
   public void setValue(Boolean val) {
      super.setValue(val);
      if (this.getContainer() instanceof ToggleModule toggle) {
         Animation anim = toggle.getAnimation();
         anim.setState(val);
         if (val) {
            EventBus.INSTANCE.subscribe(toggle);
         } else {
            EventBus.INSTANCE.unsubscribe(toggle);
         }
      }
   }

   public void enable() {
      if (this.getContainer() instanceof ToggleModule toggle) {
         toggle.enable();
      }
   }

   public void disable() {
      if (this.getContainer() instanceof ToggleModule toggle) {
         toggle.disable();
      }
   }
}

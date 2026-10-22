package net.shoreline.client.impl.module.render;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.StringConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.text.TextVisitEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class NameProtectModule extends ToggleModule {
   Config<String> placeholderConfig = this.register(new StringConfig("Placeholder", "The placeholder name for the player", "Player"));

   public NameProtectModule() {
      super("NameProtect", "Hides the player name in chat and tablist", ModuleCategory.RENDER);
   }

   @EventListener
   public void onTextVisit(TextVisitEvent event) {
      if (mc.player != null) {
         String username = mc.getUser().getName();
         String text = event.getText();
         if (text.contains(username)) {
            event.cancel();
            event.setText(text.replace(username, this.placeholderConfig.getValue()));
         }
      }
   }
}

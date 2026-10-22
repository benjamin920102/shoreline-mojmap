package net.shoreline.client.impl.event.config;

import net.shoreline.client.api.config.Config;
import net.shoreline.eventbus.event.StageEvent;

public class ConfigUpdateEvent extends StageEvent {
   private final Config<?> config;

   public ConfigUpdateEvent(Config<?> config) {
      this.config = config;
   }

   public Config<?> getConfig() {
      return this.config;
   }
}

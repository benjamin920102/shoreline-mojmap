package net.shoreline.client.api.module;

import net.shoreline.eventbus.EventBus;

public class ConcurrentModule extends Module {
   public ConcurrentModule(String name, String desc, ModuleCategory category) {
      super(name, desc, category);
      EventBus.INSTANCE.subscribe(this);
   }
}

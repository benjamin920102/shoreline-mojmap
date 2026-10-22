package net.shoreline.client.impl.module.render;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.MacroConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.macro.Macro;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.keyboard.KeyboardInputEvent;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class ZoomModule extends ToggleModule {
   Config<Integer> zoomConfig = this.register(new NumberConfig<>("Zoom", "The zoom value", 10, 30, 50));
   Config<Boolean> smoothCameraConfig = this.register(new BooleanConfig("SmoothCamera", "Adds motion reduction to the camera", true));
   Config<Macro> zoomKeyConfig = this.register(new MacroConfig("ZoomKey", "The zoom key bind", new Macro(this.getId() + "-zoomkey", 67, () -> {})));
   private boolean flag;
   private boolean flag1 = true;
   private boolean isPressed;
   private int defaultFov = 100;

   public ZoomModule() {
      super("Zoom", "Zooms in the camera perspective", ModuleCategory.RENDER);
   }

   @EventListener
   public void onKey(KeyboardInputEvent event) {
      if (event.getAction() != 2 && event.getKeycode() == this.zoomKeyConfig.getValue().getKeycode()) {
         this.isPressed = event.getAction() == 1;
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && mc.screen == null) {
         if (this.isPressed) {
            if (this.flag1) {
               this.defaultFov = mc.options.fov().get();
               this.flag1 = false;
            }

            mc.options.smoothCamera = this.smoothCameraConfig.getValue();
            mc.options.hideGui = true;
            mc.options.fov().set(this.zoomConfig.getValue());
            this.flag = true;
         } else if (this.flag) {
            mc.options.smoothCamera = false;
            mc.options.hideGui = false;
            mc.options.fov().set(this.defaultFov);
            this.flag = false;
            this.flag1 = true;
         }
      }
   }
}

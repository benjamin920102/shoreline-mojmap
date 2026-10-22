package net.shoreline.client.impl.manager.client;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.shoreline.client.api.macro.Macro;
import net.shoreline.client.impl.event.MouseClickEvent;
import net.shoreline.client.impl.event.keyboard.KeyboardInputEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class MacroManager implements Globals {
   private final Set<Macro> macros = new HashSet<>();

   public MacroManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (mc.player != null && mc.level != null && mc.screen == null) {
         if (!this.macros.isEmpty()) {
            for (Macro macro : this.macros) {
               if (macro.getKeycode() < 1000 && event.getAction() == 1 && event.getKeycode() != -1 && event.getKeycode() == macro.getKeycode()) {
                  macro.runMacro();
               }
            }
         }
      }
   }

   @EventListener
   public void onMouseInput(MouseClickEvent event) {
      if (mc.player != null && mc.level != null && mc.screen == null) {
         if (!this.macros.isEmpty()) {
            for (Macro macro : this.macros) {
               if (macro.getKeycode() >= 1000 && event.getAction() == 1 && event.getButton() != -1 && event.getButton() + 1000 == macro.getKeycode()) {
                  macro.runMacro();
               }
            }
         }
      }
   }

   public void postInit() {
   }

   public void setMacro(Macro macro, int keycode) {
      Macro m1 = this.getMacro(m -> m.getId().equals(macro.getId()));
      if (m1 != null) {
         m1.setKeycode(keycode);
      }
   }

   public void register(Macro... macros) {
      for (Macro macro : macros) {
         this.register(macro);
      }
   }

   public void register(Macro macro) {
      this.macros.add(macro);
   }

   public Macro getMacro(Predicate<? super Macro> predicate) {
      return this.macros.stream().filter(predicate).findFirst().orElse(null);
   }

   public Collection<Macro> getMacros() {
      return this.macros;
   }
}

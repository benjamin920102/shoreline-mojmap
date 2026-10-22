package net.shoreline.client.api.config.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.macro.Macro;
import net.shoreline.client.init.Managers;

public class MacroConfig extends Config<Macro> {
   public MacroConfig(String name, String desc, Macro val) {
      super(name, desc, val);
   }

   public void setValue(int keycode) {
      this.value.setKeycode(keycode);
      if (Managers.isInitialized()) {
         Managers.MACRO.setMacro(this.getValue(), keycode);
      }
   }

   public Macro fromJson(JsonObject jsonObj) {
      if (jsonObj.has("value")) {
         JsonElement element = jsonObj.get("value");
         return new Macro(this.getMacroId(), element.getAsInt(), this.getRunnable());
      } else {
         return null;
      }
   }

   @Override
   public JsonObject toJson() {
      JsonObject object = super.toJson();
      object.addProperty("value", this.getKeycode());
      return object;
   }

   public String getMacroId() {
      return this.value.getName();
   }

   public Runnable getRunnable() {
      return this.value.getRunnable();
   }

   public int getKeycode() {
      return this.value.getKeycode();
   }

   public String getKeyName() {
      return this.value.getKeyName();
   }
}

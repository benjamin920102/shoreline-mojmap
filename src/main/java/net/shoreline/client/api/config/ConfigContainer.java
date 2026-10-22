package net.shoreline.client.api.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.Identifiable;
import net.shoreline.client.api.config.setting.BlockListConfig;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.ItemListConfig;
import net.shoreline.client.api.config.setting.MacroConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.config.setting.StringConfig;
import net.shoreline.client.api.config.setting.ToggleConfig;
import net.shoreline.client.api.macro.Macro;
import net.shoreline.client.util.Globals;

public class ConfigContainer implements Identifiable, Serializable<Config<?>>, Globals {
   protected final String name;
   private final Map<String, Config<?>> configurations = Collections.synchronizedMap(new LinkedHashMap<>());

   public ConfigContainer(String name) {
      this.name = name;
   }

   protected <T> Config<T> register(Config<T> config) {
      config.setContainer(this);
      this.configurations.put(config.getId(), config);
      return config;
   }

   protected void register(Config<?>... configs) {
      for (Config<?> config : configs) {
         this.register(config);
      }
   }

   protected void unregister(Config<?> config) {
      this.configurations.remove(config.getId());
   }

   protected void unregister(Config<?>... configs) {
      for (Config<?> config : configs) {
         this.unregister(config);
      }
   }

   @Override
   public JsonObject toJson() {
      JsonObject out = new JsonObject();
      out.addProperty("name", this.getName());
      out.addProperty("id", this.getId());
      JsonArray array = new JsonArray();

      for (Config<?> config : this.getConfigs()) {
         if (!config.getName().equalsIgnoreCase("Keybind")) {
            array.add(config.toJson());
         }
      }

      out.add("configs", array);
      return out;
   }

   public Config<?> fromJson(JsonObject jsonObj) {
      if (jsonObj.has("configs")) {
         JsonElement element = jsonObj.get("configs");
         if (!element.isJsonArray()) {
            return null;
         }

         for (JsonElement je : element.getAsJsonArray()) {
            if (je.isJsonObject()) {
               JsonObject configObj = je.getAsJsonObject();
               JsonElement id = configObj.get("id");
               Config<?> config = this.getConfig(id.getAsString());
               if (config != null) {
                  try {
                     if (config instanceof ToggleConfig cfg) {
                        Boolean val = cfg.fromJson(configObj);
                        if (mc.level != null) {
                           if (val) {
                              cfg.disable();
                              cfg.enable();
                           } else {
                              cfg.disable();
                           }
                        } else {
                           cfg.setValue(val);
                        }
                     } else if (config instanceof BooleanConfig cfgx) {
                        Boolean val = cfgx.fromJson(configObj);
                        cfgx.setValue(val);
                     } else if (config instanceof ColorConfig cfgx) {
                        Color val = cfgx.fromJson(configObj);
                        cfgx.setValue(val);
                     } else if (config instanceof EnumConfig cfgx) {
                        Enum<?> val = cfgx.fromJson(configObj);
                        if (val != null) {
                           cfgx.setValue(val);
                        }
                     } else if (config instanceof ItemListConfig cfgxx) {
                        List<?> val = cfgxx.fromJson(configObj);
                        cfgxx.setValue(val);
                     } else if (config instanceof BlockListConfig cfgxx) {
                        List<?> val = cfgxx.fromJson(configObj);
                        cfgxx.setValue(val);
                     } else if (config instanceof NumberConfig cfgxx) {
                        Number val = cfgxx.fromJson(configObj);
                        cfgxx.setValue(val);
                     } else if (config instanceof StringConfig cfgxx) {
                        String val = cfgxx.fromJson(configObj);
                        cfgxx.setValue(val);
                     } else if (config instanceof MacroConfig cfgxx) {
                        Macro val = cfgxx.fromJson(configObj);
                        cfgxx.setValue(val);
                     }
                  } catch (Exception var18) {
                     Shoreline.error("Couldn't parse Json for {}!", config.getName());
                     var18.printStackTrace();
                  }
               }
            }
         }
      }

      return null;
   }

   public String getName() {
      return this.name;
   }

   @Override
   public String getId() {
      return String.format("%s-container", this.name.toLowerCase());
   }

   public Config<?> getConfig(String id) {
      return this.configurations.get(id);
   }

   public Collection<Config<?>> getConfigs() {
      return this.configurations.values();
   }
}

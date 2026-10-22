package net.shoreline.client.api.macro;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.file.ConfigFile;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.init.Managers;

public class MacroFile extends ConfigFile {
   public MacroFile(Path dir) {
      super(dir, "macros");
   }

   public MacroFile(Path dir, String path) {
      super(dir, path);
   }

   @Override
   public void save() {
      try {
         Path filepath = this.getFilepath();
         if (!Files.exists(filepath)) {
            Files.createFile(filepath);
         }

         JsonArray object = new JsonArray();

         for (Macro macro : Managers.MACRO.getMacros()) {
            object.add(macro.toJson());
         }

         this.write(filepath, this.serialize(object));
      } catch (IOException var5) {
         Shoreline.error("Could not save macro file!");
         var5.printStackTrace();
      }
   }

   @Override
   public void load() {
      try {
         Path filepath = this.getFilepath();
         if (Files.exists(filepath)) {
            String content = this.read(filepath);
            JsonArray object = this.parseArray(content);

            for (JsonElement element : object.getAsJsonArray()) {
               JsonObject jsonObject = element.getAsJsonObject();
               if (jsonObject.has("id")) {
                  String id = jsonObject.get("id").getAsString();
                  Macro macro = Managers.MACRO.getMacro(m -> m.getId().equals(id));
                  if (macro != null) {
                     macro.fromJson(jsonObject);
                     if (Managers.MODULE.getModuleById(id.substring(0, id.length() - 6)) instanceof ToggleModule t) {
                        t.keybind(jsonObject.get("value").getAsInt());
                     }
                  }
               }
            }
         }
      } catch (IOException var11) {
         Shoreline.error("Could not read macro file!");
         var11.printStackTrace();
      }
   }
}

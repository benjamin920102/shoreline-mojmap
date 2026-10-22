package net.shoreline.client.api.module.file;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.shoreline.client.api.file.ConfigFile;
import net.shoreline.client.api.module.Module;
import net.shoreline.client.init.Managers;

public class ModuleConfigFile extends ConfigFile {
   public ModuleConfigFile(Path dir, String path) {
      super(dir, path);
   }

   @Override
   public void save() {
      try {
         Path filepath = this.getFilepath();
         if (!Files.exists(filepath)) {
            Files.createFile(filepath);
         }

         JsonObject out = new JsonObject();
         out.addProperty(
            "version",
            String.format("%s %s (%s%s%s)", "Shoreline", "1.0", "beta", !"beta".equals("dev") ? "-25" : "", !"fa4cb0f".equals("null") ? "-fa4cb0f" : "")
         );
         JsonArray array = new JsonArray();

         for (Module module : Managers.MODULE.getModules()) {
            array.add(module.toJson());
         }

         out.add("configs", array);
         this.write(filepath, this.serialize(out));
      } catch (IOException var6) {
         var6.printStackTrace();
      }
   }

   @Override
   public void load() {
      try {
         Path filepath = this.getFilepath();
         if (Files.exists(filepath)) {
            String content = this.read(filepath);
            JsonObject in = this.parseObject(content);
            if (!in.has("configs")) {
               return;
            }

            JsonArray array = in.getAsJsonArray("configs");

            for (JsonElement element : array.asList()) {
               JsonObject object = element.getAsJsonObject();
               if (object.has("id")) {
                  Module module = Managers.MODULE.getModuleById(object.get("id").getAsString());
                  if (module == null) {
                     return;
                  }

                  module.fromJson(object);
               }
            }
         }
      } catch (IOException var9) {
         var9.printStackTrace();
      }
   }
}

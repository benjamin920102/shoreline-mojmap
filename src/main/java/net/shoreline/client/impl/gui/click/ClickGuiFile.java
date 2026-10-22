package net.shoreline.client.impl.gui.click;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.file.ConfigFile;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.gui.click.impl.config.CategoryFrame;
import net.shoreline.client.impl.module.client.ClickGuiModule;

public class ClickGuiFile extends ConfigFile {
   public ClickGuiFile(Path dir) {
      super(dir, "clickgui");
   }

   @Override
   public void save() {
      try {
         Path filepath = this.getFilepath();
         if (!Files.exists(filepath)) {
            Files.createFile(filepath);
         }

         JsonArray object = new JsonArray();

         for (CategoryFrame frame : ClickGuiModule.CLICK_GUI_SCREEN.getCategoryFrames()) {
            object.add(frame.toJson());
         }

         this.write(filepath, this.serialize(object));
      } catch (IOException var5) {
         Shoreline.error("Could not save clickgui file!");
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
               if (jsonObject.has("category")) {
                  JsonElement category = jsonObject.get("category");
                  CategoryFrame frame = this.getFrame(ModuleCategory.valueOf(category.getAsString()));
                  frame.fromJson(jsonObject);
               }
            }
         }
      } catch (IOException var9) {
         Shoreline.error("Could not read clickgui file!");
         var9.printStackTrace();
      }
   }

   public CategoryFrame getFrame(ModuleCategory category) {
      for (CategoryFrame frame : ClickGuiModule.CLICK_GUI_SCREEN.getCategoryFrames()) {
         if (frame.getCategory().equals(category)) {
            return frame;
         }
      }

      return null;
   }
}

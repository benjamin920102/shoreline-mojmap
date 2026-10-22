package net.shoreline.client.api.font;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.shoreline.client.api.file.ConfigFile;
import net.shoreline.client.init.Fonts;

public class FontFile extends ConfigFile {
   public FontFile(Path dir) {
      super(dir, "fonts");
   }

   @Override
   public void save() {
      try {
         Path filepath = this.getFilepath();
         if (!Files.exists(filepath)) {
            Files.createFile(filepath);
         }

         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("fontpath", Fonts.FONT_FILE_PATH);
         this.write(this.getFilepath(), this.serialize(jsonObject));
      } catch (IOException var3) {
         var3.printStackTrace();
      }
   }

   @Override
   public void load() {
      try {
         Path filepath = this.getFilepath();
         if (Files.exists(filepath)) {
            String content = this.read(filepath);
            JsonObject object = this.parseObject(content);
            Fonts.FONT_FILE_PATH = object.get("fontpath").getAsString();
         }
      } catch (IOException var4) {
         var4.printStackTrace();
      }
   }
}

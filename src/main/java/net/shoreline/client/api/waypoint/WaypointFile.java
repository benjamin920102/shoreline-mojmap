package net.shoreline.client.api.waypoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.file.ConfigFile;
import net.shoreline.client.init.Managers;

public class WaypointFile extends ConfigFile {
   public WaypointFile(Path dir) {
      super(dir, "waypoints");
   }

   @Override
   public void save() {
      try {
         Path filepath = this.getFilepath();
         if (!Files.exists(filepath)) {
            Files.createFile(filepath);
         }

         JsonArray array = new JsonArray();

         for (Waypoint point : Managers.WAYPOINT.getWaypoints()) {
            if (point instanceof UserWaypoint) {
               array.add(point.toJson());
            }
         }

         this.write(filepath, this.serialize(array));
      } catch (IOException var5) {
         Shoreline.error("Could not save file for waypoints.json!");
         var5.printStackTrace();
      }
   }

   @Override
   public void load() {
      try {
         Path filepath = this.getFilepath();
         if (Files.exists(filepath)) {
            String content = this.read(filepath);
            JsonArray array = this.parseArray(content);
            if (array == null) {
               return;
            }

            for (JsonElement e : array.asList()) {
               JsonObject obj = e.getAsJsonObject();
               if (obj.has("tag") && obj.has("dimension") && obj.has("ip")) {
                  JsonElement tag = obj.get("tag");
                  JsonElement ip = obj.get("ip");
                  JsonElement dimension = obj.get("dimension");
                  JsonElement x = obj.get("x");
                  JsonElement y = obj.get("y");
                  JsonElement z = obj.get("z");
                  Managers.WAYPOINT
                     .register(new UserWaypoint(tag.getAsString(), ip.getAsString(), dimension.getAsInt(), x.getAsDouble(), y.getAsDouble(), z.getAsDouble()));
               }
            }
         }
      } catch (IOException var13) {
         Shoreline.error("Could not read file for waypoints.json!");
         var13.printStackTrace();
      }
   }
}

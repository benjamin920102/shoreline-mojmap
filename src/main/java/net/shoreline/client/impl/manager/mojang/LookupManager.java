package net.shoreline.client.impl.manager.mojang;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.shoreline.client.util.Globals;
import org.apache.commons.io.IOUtils;

public class LookupManager implements Globals {
   private static final Map<String, UUID> LOOKUPS_UUID = new HashMap<>();
   private static final Map<UUID, String> LOOKUPS_NAME = new HashMap<>();
   private static final Set<UUID> FAILED_LOOKUPS = new HashSet<>();

   public UUID getUUIDFromName(String name) {
      UUID uuid = LOOKUPS_UUID.get(name);
      if (uuid != null) {
         return uuid;
      } else {
         if (mc.getConnection() != null) {
            List<PlayerInfo> playerListEntries = new ArrayList<>(mc.getConnection().getOnlinePlayers());
            PlayerInfo profile = playerListEntries.stream().filter(info -> info.getProfile().getName().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (profile != null) {
               UUID result = profile.getProfile().getId();
               LOOKUPS_UUID.put(name, result);
               return result;
            }
         }

         return null;
      }
   }

   public String getNameFromUUID(UUID uuid) {
      if (FAILED_LOOKUPS.contains(uuid)) {
         return null;
      } else if (LOOKUPS_NAME.containsKey(uuid)) {
         return LOOKUPS_NAME.get(uuid);
      } else {
         String url = String.format("https://laby.net/api/v2/user/%s/get-profile", uuid.toString());

         try {
            String name = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
            JsonObject jsonObject = JsonParser.parseString(name).getAsJsonObject();
            String result = jsonObject.get("username").toString();
            LOOKUPS_NAME.put(uuid, result.replace("\"", ""));
            return result;
         } catch (IOException var6) {
            var6.printStackTrace();
            FAILED_LOOKUPS.add(uuid);
            return null;
         }
      }
   }

   public Map<String, String> getNameHistoryFromUUID(UUID uuid) {
      Map<String, String> result = new TreeMap<>(Collections.reverseOrder());

      try {
         String url = String.format("https://laby.net/api/v2/user/%s/get-profile", uuid.toString());
         HttpsURLConnection connection = null;

         JsonArray array;
         try {
            connection = (HttpsURLConnection)new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder builder = new StringBuilder();

            while (scanner.hasNextLine()) {
               builder.append(scanner.nextLine());
               builder.append('\n');
            }

            scanner.close();
            String json = builder.toString();
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            array = jsonObject.getAsJsonArray("username_history");
         } finally {
            if (connection != null) {
               connection.disconnect();
            }
         }

         if (array == null) {
            return null;
         }

         for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            String name = object.get("username").getAsString();
            String changedAt = object.has("changed_at") ? object.get("changed_at").getAsString() : "";
            result.put(changedAt, name);
         }
      } catch (Exception var14) {
         var14.printStackTrace();
      }

      return result;
   }

   public Map<String, String> getPlayerStats2b2t(String playerName) {
      Map<String, String> result = new TreeMap<>(Collections.reverseOrder());

      try {
         String url = String.format("https://api.2b2t.vc/stats/player?playerName=%s", playerName);
         HttpsURLConnection connection = null;

         JsonObject jsonObject;
         try {
            connection = (HttpsURLConnection)new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder builder = new StringBuilder();

            while (scanner.hasNextLine()) {
               builder.append(scanner.nextLine());
               builder.append('\n');
            }

            scanner.close();
            String json = builder.toString();
            jsonObject = JsonParser.parseString(json).getAsJsonObject();
         } finally {
            if (connection != null) {
               connection.disconnect();
            }
         }

         if (jsonObject == null) {
            return null;
         }

         result.put("Join Count", String.valueOf(jsonObject.get("joinCount").getAsInt()));
         result.put("Leave Count", String.valueOf(jsonObject.get("leaveCount").getAsInt()));
         result.put("Death Count", String.valueOf(jsonObject.get("deathCount").getAsInt()));
         result.put("Kill Count", String.valueOf(jsonObject.get("killCount").getAsInt()));
         result.put("First Seen", jsonObject.get("firstSeen").getAsString());
         result.put("Last Seen", jsonObject.get("lastSeen").getAsString());
         result.put("Playtime", jsonObject.get("playtimeSeconds").getAsInt() / 3600 + "hrs");
         result.put("Playtime Month", jsonObject.get("playtimeSecondsMonth").getAsInt() / 3600 + "hrs");
         result.put("Chats Count", String.valueOf(jsonObject.get("chatsCount").getAsInt()));
         result.put("Prio", String.valueOf(jsonObject.get("prio").getAsBoolean()));
      } catch (Exception var13) {
         var13.printStackTrace();
      }

      return result;
   }

   public String get2b2tQueueSize() {
      try {
         String url = "https://api.2b2t.vc/queue";
         HttpsURLConnection connection = null;

         JsonObject jsonObject;
         try {
            connection = (HttpsURLConnection)new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder builder = new StringBuilder();

            while (scanner.hasNextLine()) {
               builder.append(scanner.nextLine());
               builder.append('\n');
            }

            scanner.close();
            String json = builder.toString();
            jsonObject = JsonParser.parseString(json).getAsJsonObject();
         } finally {
            if (connection != null) {
               connection.disconnect();
            }
         }

         return jsonObject == null
            ? null
            : String.format("§7Priority: §f%d, §7Regular: §f%d", jsonObject.get("prio").getAsInt(), jsonObject.get("regular").getAsInt());
      } catch (Exception var11) {
         var11.printStackTrace();
         return null;
      }
   }
}

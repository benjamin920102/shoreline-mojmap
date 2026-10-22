package net.shoreline.client.api.account.type;

import com.google.gson.JsonObject;
import net.minecraft.client.User;

public interface MinecraftAccount {
   User login();

   String username();

   default JsonObject toJSON() {
      JsonObject object = new JsonObject();
      object.addProperty("username", this.username());
      return object;
   }
}

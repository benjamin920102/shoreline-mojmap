package net.shoreline.client.api.config.setting;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.shoreline.client.api.config.Config;

public class ItemConfig extends Config<Item> {
   private final Item[] values;

   public ItemConfig(String name, String desc, Item value, Item[] values) {
      super(name, desc, value);
      this.values = values;
   }

   public ItemConfig(String name, String desc, Item value) {
      this(name, desc, value, (Item[])BuiltInRegistries.ITEM.stream().toArray());
   }

   @Override
   public JsonObject toJson() {
      return null;
   }

   public Item fromJson(JsonObject jsonObj) {
      return null;
   }
}

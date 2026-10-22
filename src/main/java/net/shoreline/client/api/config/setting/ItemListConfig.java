package net.shoreline.client.api.config.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.event.StageEvent;

public class ItemListConfig<T extends List<Item>> extends Config<T> {
   public ItemListConfig(String name, String desc, Item... values) {
      super(name, desc, (T)(new ArrayList<Item>(Arrays.asList(values))));
   }

   public ItemListConfig(String name, String desc, T values) {
      super(name, desc, values);
   }

   public void add(Item item) {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      this.getValue().add(item);
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public void remove(Item item) {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      this.getValue().remove(item);
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public void clear() {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      this.getValue().clear();
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public boolean contains(Object obj) {
      return obj instanceof Item ? this.value.contains(obj) : false;
   }

   @Override
   public JsonObject toJson() {
      JsonObject jsonObj = super.toJson();
      JsonArray array = new JsonArray();

      for (Item item : this.getValue()) {
         ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
         array.add(id.toString());
      }

      jsonObj.add("value", array);
      return jsonObj;
   }

   public T fromJson(JsonObject jsonObj) {
      if (!jsonObj.has("value")) {
         return null;
      } else {
         JsonElement element = jsonObj.get("value");
         List<Item> temp = new ArrayList<>();

         for (JsonElement je : element.getAsJsonArray()) {
            String val = je.getAsString();
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(val));
            temp.add(item);
         }

         return (T)temp;
      }
   }
}

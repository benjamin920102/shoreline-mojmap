package net.shoreline.client.api.config.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.event.StageEvent;

public class EntityListConfig<T extends List<EntityType>> extends Config<T> {
   public EntityListConfig(String name, String desc, EntityType... values) {
      super(name, desc, (T)(new ArrayList<EntityType>(Arrays.asList(values))));
   }

   public void add(EntityType entityType) {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      this.getValue().add(entityType);
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public void remove(EntityType<?> entityType) {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      this.getValue().remove(entityType);
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
      return obj instanceof EntityType ? this.value.contains(obj) : false;
   }

   @Override
   public JsonObject toJson() {
      JsonObject jsonObj = super.toJson();
      JsonArray array = new JsonArray();

      for (EntityType entityType : this.getValue()) {
         ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
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
         List<EntityType> temp = new ArrayList<>();

         for (JsonElement je : element.getAsJsonArray()) {
            String val = je.getAsString();
            EntityType entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(val));
            temp.add(entityType);
         }

         return (T)temp;
      }
   }
}

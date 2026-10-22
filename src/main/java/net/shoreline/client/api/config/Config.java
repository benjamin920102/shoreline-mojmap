package net.shoreline.client.api.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.Supplier;
import net.shoreline.client.api.Identifiable;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.render.animation.Easing;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.event.StageEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class Config<T> implements Identifiable, Serializable<T> {
   private final String name;
   private final String desc;
   protected T value;
   private final T defaultValue;
   private ConfigContainer container;
   private Supplier<Boolean> visible;
   protected final Animation configAnimation = new Animation(false, 200.0F, Easing.LINEAR);

   public Config(String name, String desc, T value) {
      if (value == null) {
         throw new NullPointerException("Null values not supported");
      } else {
         this.name = name;
         this.desc = desc;
         this.value = value;
         this.defaultValue = value;
      }
   }

   public Config(String name, String desc, T value, Supplier<Boolean> visible) {
      this(name, desc, value);
      this.visible = visible;
   }

   @Internal
   public Config(String name, String desc) {
      this.name = name;
      this.desc = desc;
      this.defaultValue = null;
   }

   @Override
   public JsonObject toJson() {
      JsonObject obj = new JsonObject();
      obj.addProperty("name", this.getName());
      obj.addProperty("id", this.getId());
      return obj;
   }

   @Override
   public T fromJson(JsonObject obj) {
      if (obj.has("value")) {
         JsonElement element = obj.get("value");
         return (T)Byte.valueOf(element.getAsByte());
      } else {
         return null;
      }
   }

   public String getName() {
      return this.name;
   }

   @Override
   public String getId() {
      return String.format("%s-%s-config", this.container.getName().toLowerCase(), this.name.toLowerCase());
   }

   public String getDescription() {
      return this.desc;
   }

   public T getValue() {
      return this.value;
   }

   public void setValue(T val) {
      if (val == null) {
         throw new NullPointerException("Null values not supported!");
      } else {
         ConfigUpdateEvent event = new ConfigUpdateEvent(this);
         event.setStage(StageEvent.EventStage.PRE);
         EventBus.INSTANCE.dispatch(event);
         this.value = val;
         event.setStage(StageEvent.EventStage.POST);
         EventBus.INSTANCE.dispatch(event);
      }
   }

   public ConfigContainer getContainer() {
      return this.container;
   }

   public void setContainer(ConfigContainer cont) {
      this.container = cont;
   }

   public Animation getAnimation() {
      return this.configAnimation;
   }

   public void setVisible(boolean visible) {
      this.visible = () -> visible;
   }

   public boolean isVisible() {
      return this.visible != null ? this.visible.get() : true;
   }

   public void resetValue() {
      this.setValue(this.defaultValue);
   }
}

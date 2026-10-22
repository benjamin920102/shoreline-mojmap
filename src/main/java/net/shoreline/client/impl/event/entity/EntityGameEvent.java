package net.shoreline.client.impl.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.shoreline.eventbus.event.Event;

public class EntityGameEvent extends Event {
   private final GameEvent gameEvent;
   private final Entity entity;

   public EntityGameEvent(GameEvent gameEvent, Entity entity) {
      this.gameEvent = gameEvent;
      this.entity = entity;
   }

   public GameEvent getGameEvent() {
      return this.gameEvent;
   }

   public Entity getEntity() {
      return this.entity;
   }
}

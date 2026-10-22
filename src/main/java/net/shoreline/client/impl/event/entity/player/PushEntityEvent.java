package net.shoreline.client.impl.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class PushEntityEvent extends Event {
   private final Entity pushed;
   private final Entity pusher;

   public PushEntityEvent(Entity pushed, Entity pusher) {
      this.pushed = pushed;
      this.pusher = pusher;
   }

   public Entity getPushed() {
      return this.pushed;
   }

   public Entity getPusher() {
      return this.pusher;
   }
}

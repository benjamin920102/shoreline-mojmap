package net.shoreline.client.impl.event.world;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.event.Event;

public class RemoveEntityEvent extends Event implements Globals {
   private final Entity entity;
   private final RemovalReason removalReason;

   public RemoveEntityEvent(Entity entity, RemovalReason removalReason) {
      this.entity = entity;
      this.removalReason = removalReason;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public RemovalReason getRemovalReason() {
      return this.removalReason;
   }
}

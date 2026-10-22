package net.shoreline.client.impl.event.entity;

import net.minecraft.world.entity.Entity;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class LookDirectionEvent extends Event {
   private final Entity entity;
   private final double cursorDeltaX;
   private final double cursorDeltaY;

   public LookDirectionEvent(Entity entity, double cursorDeltaX, double cursorDeltaY) {
      this.entity = entity;
      this.cursorDeltaX = cursorDeltaX;
      this.cursorDeltaY = cursorDeltaY;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public double getCursorDeltaX() {
      return this.cursorDeltaX;
   }

   public double getCursorDeltaY() {
      return this.cursorDeltaY;
   }
}

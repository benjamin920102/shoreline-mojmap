package net.shoreline.client.impl.event.entity;

import net.minecraft.world.phys.AABB;
import net.shoreline.eventbus.event.Event;

public class SetBBEvent extends Event {
   private final AABB boundingBox;

   public SetBBEvent(AABB boundingBox) {
      this.boundingBox = boundingBox;
   }

   public AABB getBoundingBox() {
      return this.boundingBox;
   }
}

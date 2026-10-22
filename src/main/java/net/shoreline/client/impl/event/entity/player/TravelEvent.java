package net.shoreline.client.impl.event.entity.player;

import net.minecraft.world.phys.Vec3;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.StageEvent;

@Cancelable
public class TravelEvent extends StageEvent {
   private final Vec3 movementInput;

   public TravelEvent(Vec3 movementInput) {
      this.movementInput = movementInput;
   }

   public Vec3 getMovementInput() {
      return this.movementInput;
   }
}

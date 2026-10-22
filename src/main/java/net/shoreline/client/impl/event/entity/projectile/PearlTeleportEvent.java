package net.shoreline.client.impl.event.entity.projectile;

import net.minecraft.world.level.portal.DimensionTransition;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class PearlTeleportEvent extends Event {
   private final DimensionTransition teleportTarget;

   public PearlTeleportEvent(DimensionTransition teleportTarget) {
      this.teleportTarget = teleportTarget;
   }

   public DimensionTransition getTeleportTarget() {
      return this.teleportTarget;
   }
}

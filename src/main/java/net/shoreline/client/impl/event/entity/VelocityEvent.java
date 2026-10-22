package net.shoreline.client.impl.event.entity;

import net.minecraft.world.phys.Vec3;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class VelocityEvent extends Event {
   private Vec3 velocity;

   public VelocityEvent(Vec3 velocity) {
      this.velocity = velocity;
   }

   public void setVelocity(Vec3 velocity) {
      this.velocity = velocity;
   }

   public void setXYZ(double x, double y, double z) {
      this.velocity = new Vec3(x, y, z);
   }

   public Vec3 getVelocity() {
      return this.velocity;
   }
}

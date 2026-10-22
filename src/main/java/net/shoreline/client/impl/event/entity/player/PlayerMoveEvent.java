package net.shoreline.client.impl.event.entity.player;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class PlayerMoveEvent extends Event {
   private final MoverType type;
   private double x;
   private double y;
   private double z;

   public PlayerMoveEvent(MoverType type, Vec3 movement) {
      this.type = type;
      this.x = movement.x();
      this.y = movement.y();
      this.z = movement.z();
   }

   public MoverType getType() {
      return this.type;
   }

   public Vec3 getMovement() {
      return new Vec3(this.x, this.y, this.z);
   }

   public double getX() {
      return this.x;
   }

   public void setX(double x) {
      this.x = x;
   }

   public double getY() {
      return this.y;
   }

   public void setY(double y) {
      this.y = y;
   }

   public double getZ() {
      return this.z;
   }

   public void setZ(double z) {
      this.z = z;
   }
}

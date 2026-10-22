package net.shoreline.client.api.waypoint;

import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.ConfigContainer;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;

public class Waypoint extends ConfigContainer implements Globals {
   private final String ip;
   private final int dimension;
   double x;
   double y;
   double z;
   private final Timer timer;

   public Waypoint(String name, String ip, int dimension, double x, double y, double z) {
      super(name);
      this.ip = ip;
      this.dimension = dimension;
      this.x = x;
      this.y = y;
      this.z = z;
      this.timer = new CacheTimer();
   }

   private boolean passedTime(long time) {
      return this.timer.passed(time);
   }

   public String getIp() {
      return this.ip;
   }

   @Override
   public JsonObject toJson() {
      JsonObject obj = new JsonObject();
      obj.addProperty("tag", this.getName());
      obj.addProperty("ip", this.getIp());
      obj.addProperty("dimension", this.getDimension());
      obj.addProperty("x", this.getX());
      obj.addProperty("y", this.getY());
      obj.addProperty("z", this.getZ());
      return obj;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public Vec3 getPos() {
      return new Vec3(this.getX(), this.getY(), this.getZ());
   }

   public int getDimension() {
      return this.dimension;
   }
}

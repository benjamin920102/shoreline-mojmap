package net.shoreline.client.impl.event.world;

import java.awt.Color;
import net.minecraft.world.phys.Vec3;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

public class SkyboxEvent extends Event {
   private Color color;

   public Color getColor() {
      return this.color;
   }

   public Vec3 getColorVec() {
      return new Vec3(this.color.getRed() / 255.0, this.color.getGreen() / 255.0, this.color.getBlue() / 255.0);
   }

   public int getRGB() {
      return this.color.getRGB();
   }

   public void setColor(Color color) {
      this.color = color;
   }

   @Cancelable
   public static class Cloud extends SkyboxEvent {
   }

   @Cancelable
   public static class Fog extends SkyboxEvent {
      private final float tickDelta;

      public Fog(float tickDelta) {
         this.tickDelta = tickDelta;
      }

      public float getTickDelta() {
         return this.tickDelta;
      }
   }

   @Cancelable
   public static class Sky extends SkyboxEvent {
   }
}

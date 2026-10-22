package net.shoreline.client.impl.event.render.entity;

import net.minecraft.client.player.AbstractClientPlayer;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderPlayerEvent extends Event {
   private final AbstractClientPlayer entity;
   private float yaw;
   private float pitch;

   public RenderPlayerEvent(AbstractClientPlayer entity) {
      this.entity = entity;
   }

   public AbstractClientPlayer getEntity() {
      return this.entity;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }
}

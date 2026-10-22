package net.shoreline.client.impl.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class BobViewEvent extends Event {
   private final PoseStack matrixStack;
   private final float tickDelta;
   private float y;

   public BobViewEvent(PoseStack matrixStack, float tickDelta) {
      this.matrixStack = matrixStack;
      this.tickDelta = tickDelta;
   }

   public float getY() {
      return this.y;
   }

   public void setY(float y) {
      this.y = y;
   }

   public PoseStack getMatrixStack() {
      return this.matrixStack;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }
}

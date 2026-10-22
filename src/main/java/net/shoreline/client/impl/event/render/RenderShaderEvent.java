package net.shoreline.client.impl.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderShaderEvent extends Event {
   private final PoseStack matrices;
   private final float tickDelta;

   public RenderShaderEvent(PoseStack matrices, float tickDelta) {
      this.matrices = matrices;
      this.tickDelta = tickDelta;
   }

   public PoseStack getMatrices() {
      return this.matrices;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public static class BlockEntities extends RenderShaderEvent {
      public BlockEntities(PoseStack matrices, float tickDelta) {
         super(matrices, tickDelta);
      }
   }
}

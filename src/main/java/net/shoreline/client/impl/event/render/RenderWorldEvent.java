package net.shoreline.client.impl.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.shoreline.eventbus.event.Event;

public class RenderWorldEvent extends Event {
   private final PoseStack matrices;
   private final float tickDelta;

   public RenderWorldEvent(PoseStack matrices, float tickDelta) {
      this.matrices = matrices;
      this.tickDelta = tickDelta;
   }

   public PoseStack getMatrices() {
      return this.matrices;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public static class Game extends RenderWorldEvent {
      public Game(PoseStack matrices, float tickDelta) {
         super(matrices, tickDelta);
      }
   }

   public static class Hand extends RenderWorldEvent {
      public Hand(PoseStack matrices, float tickDelta) {
         super(matrices, tickDelta);
      }
   }
}

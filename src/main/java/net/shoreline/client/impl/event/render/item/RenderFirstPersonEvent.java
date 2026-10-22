package net.shoreline.client.impl.event.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

public class RenderFirstPersonEvent extends Event {
   public final InteractionHand hand;
   public final ItemStack item;
   public final float equipProgress;
   public final PoseStack matrices;

   public RenderFirstPersonEvent(InteractionHand hand, ItemStack item, float equipProgress, PoseStack matrices) {
      this.hand = hand;
      this.item = item;
      this.equipProgress = equipProgress;
      this.matrices = matrices;
   }

   @Cancelable
   public static class Head extends RenderFirstPersonEvent {
      public Head(InteractionHand hand, ItemStack item, float equipProgress, PoseStack matrices) {
         super(hand, item, equipProgress, matrices);
      }
   }
}

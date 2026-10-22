package net.shoreline.client.impl.event.network;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.event.Event;

public class SetCurrentHandEvent extends Event implements Globals {
   private final InteractionHand hand;

   public SetCurrentHandEvent(InteractionHand hand) {
      this.hand = hand;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public ItemStack getStackInHand() {
      return mc.player.getItemInHand(this.hand);
   }
}

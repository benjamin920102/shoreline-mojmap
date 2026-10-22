package net.shoreline.client.impl.event.entity;

import net.minecraft.world.InteractionHand;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class SwingEvent extends Event {
   private final InteractionHand hand;

   public SwingEvent(InteractionHand hand) {
      this.hand = hand;
   }

   public InteractionHand getHand() {
      return this.hand;
   }
}
